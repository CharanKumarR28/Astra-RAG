package com.groq.service;

import com.groq.dto.DocumentUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final PdfChunkingService pdfChunkingService;
    private final EmbeddingService embeddingService;
    private final PineconeService pineconeService;

    /**
     * Full PDF ingestion pipeline:
     * PDF → extract text → chunk → embed → store in Pinecone
     */
    public DocumentUploadResponse ingestPdf(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("Starting PDF ingestion | file={}", fileName);

        try {
            // Step 1 — Extract text from PDF using PDFBox
            log.info("[Step 1] Extracting text from PDF");
            String fullText = pdfChunkingService.extractText(file);

            if (fullText.isEmpty()) {
                return DocumentUploadResponse.builder()
                        .fileName(fileName)
                        .totalChunks(0)
                        .status("FAILED")
                        .message("No text could be extracted from the PDF.")
                        .build();
            }

            // Step 2 — Split into chunks
            log.info("[Step 2] Splitting text into chunks");
            List<String> chunks = pdfChunkingService.chunkText(fullText);

            // Step 3 — Delete existing vectors for this file (re-upload case)
            log.info("[Step 3] Cleaning existing vectors for file: {}", fileName);
            pineconeService.deleteVectorsByFile(fileName);

            // Step 4 — Embed each chunk and store in Pinecone
            log.info("[Step 4] Embedding {} chunks and storing in Pinecone", chunks.size());
            int stored = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                String vectorId = fileName + "_chunk_" + i + "_" + UUID.randomUUID().toString().substring(0, 8);

                // Get embedding from Groq
                List<Double> embedding = embeddingService.getEmbedding(chunk);
                float[] floatVector = embeddingService.toFloatArray(embedding);

                // Store in Pinecone
                pineconeService.upsertVector(vectorId, floatVector, chunk, fileName);
                stored++;

                log.info("Chunk {}/{} stored | id={}", i + 1, chunks.size(), vectorId);
            }

            log.info("PDF ingestion complete | file={} | chunks={}", fileName, stored);

            return DocumentUploadResponse.builder()
                    .fileName(fileName)
                    .totalChunks(stored)
                    .status("SUCCESS")
                    .message("PDF processed successfully. " + stored + " chunks stored. You can now ask questions about this document!")
                    .build();

        } catch (Exception e) {
            log.error("PDF ingestion failed | file={} | error={}", fileName, e.getMessage(), e);
            return DocumentUploadResponse.builder()
                    .fileName(fileName)
                    .totalChunks(0)
                    .status("FAILED")
                    .message("Failed to process PDF: " + e.getMessage())
                    .build();
        }
    }

    /**
     * RAG Query pipeline:
     * User question → embed → search Pinecone → build context → return chunks
     */
    public String buildRagContext(String userQuestion) {
        try {
            log.info("Building RAG context for question: {}", userQuestion);

            // Step 1 — Embed user question
            List<Double> questionEmbedding = embeddingService.getEmbedding(userQuestion);
            float[] questionVector = embeddingService.toFloatArray(questionEmbedding);

            // Step 2 — Search Pinecone for similar chunks
            List<String> relevantChunks = pineconeService.querySimilarChunks(questionVector);

            if (relevantChunks.isEmpty()) {
                log.info("No relevant chunks found — will use general AI knowledge");
                return null;
            }

            // Step 3 — Build context string from chunks
            StringBuilder context = new StringBuilder();
            context.append("Based on the following document content:\n\n");
            for (int i = 0; i < relevantChunks.size(); i++) {
                context.append("--- Section ").append(i + 1).append(" ---\n");
                context.append(relevantChunks.get(i)).append("\n\n");
            }
            context.append("Please answer the following question using the above content:\n");

            log.info("RAG context built with {} chunks", relevantChunks.size());
            return context.toString();

        } catch (Exception e) {
            log.error("RAG context building failed: {}", e.getMessage());
            return null; // fallback to normal chat
        }
    }
}
