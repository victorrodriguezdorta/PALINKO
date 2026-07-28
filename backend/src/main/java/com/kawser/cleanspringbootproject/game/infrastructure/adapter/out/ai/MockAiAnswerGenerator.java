package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.kawser.cleanspringbootproject.game.application.port.out.AiAnswerGenerator;
import com.kawser.cleanspringbootproject.game.domain.model.Question;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Placeholder AI response, picked from a small pool of generic-sounding
 * answers. Replace with a real provider-backed implementation later without
 * touching the domain/application layers — they only depend on
 * AiAnswerGenerator.
 */
@Component
public class MockAiAnswerGenerator implements AiAnswerGenerator {

    private static final List<String> GENERIC_ANSWERS = List.of(
            "Creo que depende mucho del contexto de cada persona.",
            "Es una pregunta interesante, lo pensaría con calma antes de responder.",
            "Probablemente optaría por la opción más equilibrada.",
            "Diría que la experiencia personal influye bastante en la respuesta.",
            "Me parece algo subjetivo, pero intentaría ser honesto al responder.");

    private final Random random = new Random();

    @Override
    public String generateAnswer(Question question) {
        return GENERIC_ANSWERS.get(random.nextInt(GENERIC_ANSWERS.size()));
    }
}
