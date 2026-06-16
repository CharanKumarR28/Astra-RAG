package com.groq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PineconeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${pinecone.api.key}")
    private String apiKey;

    @Value("${pinecone.index.url}")
    private String indexUrl;

    @Value("${rag.top.k}")
    private int topK;

    /**
     * Upsert a vector with metadata into Pinecone
     *
     * @param id        unique ID for this chunk
     * @param vector    embedding float array
     * @param chunkText original text chunk
     * @param fileName  source PDF filename
     */
    public void upsertVector(String id, float[] vector,
                              String chunkText, String fileName) {
        try {
            HttpHeaders headers = buildHeaders();

            // Convert float[] to List<Float> for JSON
            List<Float> vectorList = new ArrayList<>();
            for (float f : vector) vectorList.add(f);

            // Build metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("text", chunkText);
            metadata.put("fileName", fileName);

            // Build vector object
            Map<String, Object> vectorObj = new HashMap<>();
            vectorObj.put("id", id);
            vectorObj.put("values", vectorList);
            vectorObj.put("metadata", metadata);

            // Build upsert request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vectors", Collections.singletonList(vectorObj));

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            restTemplate.exchange(
                    indexUrl + "/vectors/upsert",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("Vector upserted | id={} | file={}", id, fileName);

        } catch (Exception e) {
            log.error("Pinecone upsert failed | id={} | error={}", id, e.getMessage());
            throw new RuntimeException("Pinecone upsert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Query Pinecone for top-K most similar vectors
     * Returns list of matching text chunks
     *
     * @param queryVector embedding of the user's question
     * @return list of relevant text chunks
     */
    public List<String> querySimilarChunks(float[] queryVector) {
        try {
            HttpHeaders headers = buildHeaders();

            // Convert float[] to List<Float>
            List<Float> vectorList = new ArrayList<>();
            for (float f : queryVector) vectorList.add(f);

            // Build query request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vector", vectorList);
            requestBody.put("topK", topK);
            requestBody.put("includeMetadata", true);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            log.info("Querying Pinecone for top {} similar chunks", topK);

            ResponseEntity<Map> response = restTemplate.exchange(
                    indexUrl + "/query",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Parse matches from response
            List<String> chunks = new ArrayList<>();
            if (response.getBody() != null) {
                List<Map> matches = (List<Map>) response.getBody().get("matches");
                if (matches != null) {
                    for (Map match : matches) {
                        Map metadata = (Map) match.get("metadata");
                        if (metadata != null && metadata.get("text") != null) {
                            chunks.add((String) metadata.get("text"));
                        }
                    }
                }
            }

            log.info("Found {} relevant chunks from Pinecone", chunks.size());
            return chunks;

        } catch (Exception e) {
            log.error("Pinecone query failed: {}", e.getMessage());
            return new ArrayList<>(); // return empty — fallback to normal chat
        }
    }

    /**
     * Delete all vectors for a specific file (useful for re-uploading)
     */
    public void deleteVectorsByFile(String fileName) {
        try {
            HttpHeaders headers = buildHeaders();

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> filter = new HashMap<>();
            filter.put("fileName", Collections.singletonMap("$eq", fileName));
            requestBody.put("filter", filter);
            requestBody.put("deleteAll", false);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            restTemplate.exchange(
                    indexUrl + "/vectors/delete",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("Deleted vectors for file: {}", fileName);

        } catch (Exception e) {
            log.warn("Could not delete vectors for file {}: {}", fileName, e.getMessage());
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        return headers;
    }
}
