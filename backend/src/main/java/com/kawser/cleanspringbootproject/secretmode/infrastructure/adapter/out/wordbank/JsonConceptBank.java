package com.kawser.cleanspringbootproject.secretmode.infrastructure.adapter.out.wordbank;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import com.kawser.cleanspringbootproject.secretmode.application.port.out.ConceptBank;
import com.kawser.cleanspringbootproject.secretmode.domain.exception.ConceptNotAvailableException;
import com.kawser.cleanspringbootproject.secretmode.domain.model.Concept;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads every concept from resources/secretmode/concepts/*.json (indexed by
 * id) plus the day-to-conceptId calendar from
 * resources/secretmode/daily-challenges.json, picking today's entry by the
 * same day-of-year modulo trick as
 * game.infrastructure.adapter.out.wordbank.CuratedDailyChainWordBank - so
 * adding future daily challenges is purely a matter of dropping in new JSON
 * files and calendar entries, no code changes.
 *
 * <p>Unlike CuratedDailyChainWordBank, which reads one fixed-name resource
 * per language via ClassPathResource, this needs to list an unknown number
 * of concept files in a directory - ClassPathResource can't do that, hence
 * PathMatchingResourcePatternResolver.
 */
public class JsonConceptBank implements ConceptBank {

    private static final String CONCEPTS_LOCATION_PATTERN = "classpath:secretmode/concepts/*.json";
    private static final String CALENDAR_RESOURCE_PATH = "secretmode/daily-challenges.json";

    private final Map<String, Concept> conceptsById;
    private final List<DailyEntry> calendar;

    public JsonConceptBank(ObjectMapper objectMapper) {
        this.conceptsById = loadConcepts(objectMapper);
        this.calendar = loadCalendar(objectMapper);
        if (calendar.isEmpty()) {
            throw new IllegalStateException("secretmode daily challenge calendar must not be empty");
        }
    }

    private static Map<String, Concept> loadConcepts(ObjectMapper objectMapper) {
        try {
            Resource[] resources =
                    new PathMatchingResourcePatternResolver().getResources(CONCEPTS_LOCATION_PATTERN);
            Map<String, Concept> concepts = new HashMap<>();
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    ConceptJson json = objectMapper.readValue(inputStream, ConceptJson.class);
                    Concept concept = new Concept(
                            json.id(), json.language(), json.hiddenQuality(),
                            lowerCased(json.acceptedWords()), lowerCased(json.rejectedWords()));
                    concepts.put(concept.id(), concept);
                }
            }
            return concepts;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load secretmode concepts from " + CONCEPTS_LOCATION_PATTERN, e);
        }
    }

    private static Set<String> lowerCased(List<String> words) {
        Set<String> lowered = new HashSet<>();
        if (words != null) {
            words.forEach(word -> lowered.add(word.trim().toLowerCase()));
        }
        return lowered;
    }

    private static List<DailyEntry> loadCalendar(ObjectMapper objectMapper) {
        try (InputStream inputStream = new PathMatchingResourcePatternResolver()
                .getResource("classpath:" + CALENDAR_RESOURCE_PATH).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<DailyEntry>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load secretmode daily calendar from " + CALENDAR_RESOURCE_PATH, e);
        }
    }

    @Override
    public Concept dailyConceptFor(LocalDate utcDate) {
        int dayOfYear = utcDate.getDayOfYear();
        DailyEntry entry = calendar.get((dayOfYear - 1) % calendar.size());
        Concept concept = conceptsById.get(entry.conceptId());
        if (concept == null) {
            throw new ConceptNotAvailableException(entry.conceptId());
        }
        return concept;
    }

    @Override
    public Optional<Concept> findById(String id) {
        return Optional.ofNullable(conceptsById.get(id));
    }

    private record ConceptJson(
            String id, GameLanguage language, String hiddenQuality, List<String> acceptedWords, List<String> rejectedWords) {
    }

    private record DailyEntry(int day, String conceptId) {
    }
}
