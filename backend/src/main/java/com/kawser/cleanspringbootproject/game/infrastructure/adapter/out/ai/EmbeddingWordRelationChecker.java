package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import com.kawser.cleanspringbootproject.game.application.port.out.WordRelation;
import com.kawser.cleanspringbootproject.game.application.port.out.WordRelationChecker;
import com.kawser.cleanspringbootproject.game.domain.model.GameLanguage;
import org.springframework.stereotype.Component;

/**
 * Judges word relatedness with a local sentence-embedding model: both words
 * are embedded (via the shared {@link SentenceEmbeddingModel}) and scored by
 * cosine similarity of their vectors. A single multilingual model covers
 * both {@link GameLanguage} values, so the language argument is accepted for
 * interface/API stability but not otherwise used. No justification text is
 * available from this adapter, since it never reasons in natural language.
 *
 * <p>Kept alongside {@link GroqWordRelationChecker} (the default adapter,
 * see {@code GameApplicationConfig}) as an offline fallback that needs no
 * external API key.
 */
@Component
public class EmbeddingWordRelationChecker implements WordRelationChecker {

    private final SentenceEmbeddingModel embeddingModel;

    public EmbeddingWordRelationChecker(SentenceEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public WordRelation relatedness(String wordA, String wordB, GameLanguage language) {
        String a = wordA.trim();
        String b = wordB.trim();
        if (a.equalsIgnoreCase(b)) {
            return new WordRelation(100, null);
        }

        float[] embeddingA = embeddingModel.embed(a);
        float[] embeddingB = embeddingModel.embed(b);
        double cosine = SentenceEmbeddingModel.cosineSimilarity(embeddingA, embeddingB);
        double clamped = Math.max(0.0, Math.min(1.0, cosine));
        return new WordRelation((int) Math.round(clamped * 100), null);
    }
}
