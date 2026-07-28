package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.game.application.port.out.QuestionBank;
import com.kawser.cleanspringbootproject.game.domain.model.Question;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Fixed in-memory bank for the first game mode ("Pretend to be an AI").
 * Stateless: which questions a given room has already used is passed in by
 * the caller, so this bean can be shared safely across every concurrent
 * room without leaking per-game state.
 */
@Component
public class InMemoryQuestionBank implements QuestionBank {

    private static final List<String> QUESTIONS = List.of(
            "¿Cuál es el mejor consejo que le darías a alguien que empieza en tu trabajo?",
            "Describe tu comida favorita sin decir su nombre.",
            "¿Qué harías si te tocara la lotería mañana?",
            "¿Cuál es tu recuerdo de infancia más vívido?",
            "Si pudieras vivir en cualquier época de la historia, ¿cuál elegirías y por qué?",
            "¿Qué opinas de trabajar desde casa?",
            "Describe cómo sería tu día perfecto.",
            "¿Cuál es la mejor película que has visto este año?",
            "Si tuvieras que enseñar una habilidad a los demás, ¿cuál sería?",
            "¿Qué consejo le darías a tu yo de hace 10 años?");

    private final Random random = new Random();

    @Override
    public Question nextQuestion(Set<String> excludedIds) {
        List<String> available = QUESTIONS.stream()
                .filter(text -> !excludedIds.contains(idFor(text)))
                .toList();
        List<String> pool = available.isEmpty() ? QUESTIONS : available;
        String text = pool.get(random.nextInt(pool.size()));
        return Question.fromBank(idFor(text), text);
    }

    private String idFor(String text) {
        return "bank-" + Integer.toHexString(text.hashCode());
    }
}
