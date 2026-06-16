package com.groq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groq.dto.ChatRequest;
import com.groq.dto.ChatResponse;
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
public class GroqChatService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RagService ragService;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String defaultModel;

    @Value("${groq.api.max-tokens}")
    private int defaultMaxTokens;

    /**
     * Send chat message to Groq AI with optional RAG context
     * If relevant PDF chunks found → RAG mode
     * If no chunks found → normal chat mode
     */
    public ChatResponse chat(ChatRequest request) {
        try {
            // Extract user message from request
            String userMessage = extractUserMessage(request.getMessages());
            log.info("Chat request | message={}", userMessage);

            // Try to get RAG context from Pinecone
            String ragContext = ragService.buildRagContext(userMessage);

            // Build final messages list
            List<Map<String, String>> messages = new ArrayList<>();

            if (ragContext != null) {
                // RAG mode — inject document context as system message
                log.info("RAG mode — injecting document context");
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content",
                        "You are Astra, a helpful AI assistant. " +
                        "Answer questions based on the provided document context. " +
                        "If the answer is not in the context, say so politely and answer from general knowledge.\n\n"
                        + ragContext);
                messages.add(systemMessage);
            } else {
                // Normal mode — general Astra assistant
                log.info("Normal chat mode — no document context");
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content",
                        "You are Astra, a helpful and friendly AI assistant. " +
                        "Answer questions clearly and concisely. " +
                        "If a user uploads a PDF, you can answer questions about it.");
                messages.add(systemMessage);
            }

            // Add original user messages
            messages.addAll(request.getMessages());

            // Build request body
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", request.getModel() != null
                    ? request.getModel() : defaultModel);
            requestBody.put("messages", messages);
            requestBody.put("temperature", request.getTemperature() > 0
                    ? request.getTemperature() : 0.7);
            requestBody.put("max_tokens", request.getMax_tokens() > 0
                    ? request.getMax_tokens() : defaultMaxTokens);

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            log.info("Calling Groq API | model={} | ragMode={}",
                    defaultModel, ragContext != null);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                List<Map> choices = (List<Map>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map message = (Map) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    String modelUsed = (String) response.getBody().get("model");

                    return ChatResponse.builder()
                            .response(content)
                            .model(modelUsed)
                            .status(ragContext != null ? "RAG_SUCCESS" : "SUCCESS")
                            .build();
                }
            }

            throw new RuntimeException("Empty response from Groq API");

        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage());
            return ChatResponse.builder()
                    .response("Sorry, I encountered an error: " + e.getMessage())
                    .model(defaultModel)
                    .status("ERROR")
                    .build();
        }
    }

    private String extractUserMessage(List<Map<String, String>> messages) {
        if (messages != null && !messages.isEmpty()) {
            for (int i = messages.size() - 1; i >= 0; i--) {
                if ("user".equals(messages.get(i).get("role"))) {
                    return messages.get(i).get("content");
                }
            }
        }
        return "";
    }
}
