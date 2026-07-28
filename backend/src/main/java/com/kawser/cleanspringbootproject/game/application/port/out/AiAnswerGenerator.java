package com.kawser.cleanspringbootproject.game.application.port.out;

import com.kawser.cleanspringbootproject.game.domain.model.Question;

/**
 * Produces the AI's answer for a round's question. Mocked in this phase;
 * swapping in a real LLM-backed implementation later requires no change to
 * the domain or application layer.
 */
public interface AiAnswerGenerator {

    String generateAnswer(Question question);
}
