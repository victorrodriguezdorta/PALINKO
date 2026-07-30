package com.kawser.cleanspringbootproject.game.infrastructure.adapter.out.ai;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Loads a single multilingual sentence-embedding model (paraphrase
 * -multilingual-MiniLM-L12-v2, a small/fast distilled sentence-transformer)
 * once per JVM and exposes mean-pooled, L2-normalized embeddings plus cosine
 * similarity over them - shared by every adapter that needs semantic
 * word/text comparison (WordRelationChecker, ChainWordBank), so the ~120 MB
 * ONNX session and tokenizer are only ever loaded a single time regardless
 * of how many consumers exist.
 *
 * <p>The model weights are not committed to the repo: they are downloaded on
 * first use straight from the Hugging Face Hub into {@code
 * ~/.guesstheai/models}, a per-machine cache shared by every checkout/branch
 * so the download only ever happens once per machine. ONNX Runtime and the
 * Hugging Face tokenizer library both ship their native binaries inside
 * their jars for Windows/macOS/Linux, so no separate install step is needed
 * beyond `mvn` resolving those two dependencies.
 */
@Component
public class SentenceEmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(SentenceEmbeddingModel.class);

    private static final String MODEL_REPO = "Xenova/paraphrase-multilingual-MiniLM-L12-v2";
    private static final String HUB_BASE_URL = "https://huggingface.co/" + MODEL_REPO + "/resolve/main/";
    private static final String MODEL_FILE_NAME = "model_quantized.onnx";
    private static final String TOKENIZER_FILE_NAME = "tokenizer.json";

    private static final Path CACHE_DIR = Paths.get(
            System.getProperty("user.home"), ".guesstheai", "models", "paraphrase-multilingual-minilm-l12-v2");

    private final Object loadLock = new Object();
    private volatile boolean loaded = false;

    private OrtEnvironment environment;
    private OrtSession session;
    private HuggingFaceTokenizer tokenizer;

    @PostConstruct
    void warmUpInBackground() {
        CompletableFuture.runAsync(() -> {
            try {
                ensureLoaded();
            } catch (RuntimeException e) {
                log.warn("Could not pre-load the semantic embedding model; "
                        + "it will be retried on the first request.", e);
            }
        });
    }

    /**
     * A mean-pooled, L2-normalized embedding of the given text, so that
     * {@link #cosineSimilarity(float[], float[])} on two such vectors
     * reduces to a plain dot product.
     */
    public float[] embed(String text) {
        ensureLoaded();
        Encoding encoding = tokenizer.encode(text);
        long[] ids = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();
        long[] typeIds = encoding.getTypeIds();
        long[] shape = {1, ids.length};

        try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(ids), shape);
             OnnxTensor attentionMaskTensor =
                     OnnxTensor.createTensor(environment, LongBuffer.wrap(attentionMask), shape);
             OnnxTensor typeIdsTensor = OnnxTensor.createTensor(environment, LongBuffer.wrap(typeIds), shape)) {

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);
            if (session.getInputNames().contains("token_type_ids")) {
                inputs.put("token_type_ids", typeIdsTensor);
            }

            try (OrtSession.Result result = session.run(inputs)) {
                OnnxTensor tokenEmbeddings = (OnnxTensor) result.get(0);
                long[] outputShape = ((TensorInfo) tokenEmbeddings.getInfo()).getShape();
                int seqLen = (int) outputShape[1];
                int hiddenDim = (int) outputShape[2];

                FloatBuffer buffer = tokenEmbeddings.getFloatBuffer();
                float[] flat = new float[buffer.remaining()];
                buffer.get(flat);

                return meanPoolAndNormalize(flat, seqLen, hiddenDim, attentionMask);
            }
        } catch (OrtException e) {
            throw new IllegalStateException("Semantic embedding inference failed for \"" + text + "\"", e);
        }
    }

    /**
     * Cosine similarity between two embeddings produced by {@link
     * #embed(String)} - a plain dot product suffices since both inputs are
     * already unit-normalized.
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        return dotProduct(a, b);
    }

    private static float[] meanPoolAndNormalize(float[] flat, int seqLen, int hiddenDim, long[] attentionMask) {
        float[] pooled = new float[hiddenDim];
        int validTokens = 0;
        for (int t = 0; t < seqLen; t++) {
            if (attentionMask[t] == 1L) {
                int base = t * hiddenDim;
                for (int d = 0; d < hiddenDim; d++) {
                    pooled[d] += flat[base + d];
                }
                validTokens++;
            }
        }
        if (validTokens == 0) {
            validTokens = 1;
        }

        double normSquared = 0.0;
        for (int d = 0; d < hiddenDim; d++) {
            pooled[d] /= validTokens;
            normSquared += (double) pooled[d] * pooled[d];
        }
        double norm = normSquared == 0.0 ? 1.0 : Math.sqrt(normSquared);
        for (int d = 0; d < hiddenDim; d++) {
            pooled[d] = (float) (pooled[d] / norm);
        }
        return pooled;
    }

    private static double dotProduct(float[] a, float[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += (double) a[i] * b[i];
        }
        return sum;
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (loadLock) {
            if (loaded) {
                return;
            }
            try {
                Path modelFile = CACHE_DIR.resolve(MODEL_FILE_NAME);
                Path tokenizerFile = CACHE_DIR.resolve(TOKENIZER_FILE_NAME);

                if (!Files.exists(modelFile)) {
                    log.info("Downloading semantic embedding model to {} (one-time, ~120 MB)...", CACHE_DIR);
                    downloadIfMissing(HUB_BASE_URL + "onnx/" + MODEL_FILE_NAME, modelFile);
                }
                downloadIfMissing(HUB_BASE_URL + TOKENIZER_FILE_NAME, tokenizerFile);

                this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile);
                this.environment = OrtEnvironment.getEnvironment();
                this.session = environment.createSession(modelFile.toString(), new OrtSession.SessionOptions());
                loaded = true;
                log.info("Semantic embedding model ready.");
            } catch (IOException | OrtException | InterruptedException e) {
                throw new IllegalStateException(
                        "Could not load the semantic embedding model from " + CACHE_DIR, e);
            }
        }
    }

    private static void downloadIfMissing(String url, Path target) throws IOException, InterruptedException {
        if (Files.exists(target) && Files.size(target) > 0) {
            return;
        }
        Files.createDirectories(target.getParent());
        Path partial = target.resolveSibling(target.getFileName() + ".part");

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(partial));

        if (response.statusCode() != 200) {
            Files.deleteIfExists(partial);
            throw new IOException("Failed to download " + url + " (HTTP " + response.statusCode() + ")");
        }
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
