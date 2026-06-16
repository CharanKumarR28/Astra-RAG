package com.groq.service;

import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    private ZooModel<String, float[]> model;
    private Predictor<String, float[]> predictor;

    @PostConstruct
    public void init() {
        try {
            log.info("Loading embedding model locally via DJL...");
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                    .optProgress(new ProgressBar())
                    .build();

            model     = criteria.loadModel();
            predictor = model.newPredictor();
            log.info("Embedding model loaded successfully!");

        } catch (Exception e) {
            log.error("Failed to load embedding model: {}", e.getMessage());
            throw new RuntimeException("Embedding model init failed", e);
        }
    }

    /**
     * Convert text to embedding vector locally — no external API needed
     * Model: all-MiniLM-L6-v2 — outputs 384 dimensions
     */
    public List<Double> getEmbedding(String text) {
        try {
            float[] floats = predictor.predict(text);
            List<Double> result = new ArrayList<>();
            for (float f : floats) {
                result.add((double) f);
            }
            log.info("Embedding generated locally | dimensions={}", result.size());
            return result;

        } catch (TranslateException e) {
            log.error("Embedding failed: {}", e.getMessage());
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Convert List<Double> to float[] for Pinecone
     */
    public float[] toFloatArray(List<Double> embedding) {
        float[] floats = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            floats[i] = embedding.get(i).floatValue();
        }
        return floats;
    }
}