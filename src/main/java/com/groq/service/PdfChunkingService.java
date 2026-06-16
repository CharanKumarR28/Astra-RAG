package com.groq.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PdfChunkingService {

    @Value("${rag.chunk.size}")
    private int chunkSize;

    @Value("${rag.chunk.overlap}")
    private int chunkOverlap;

    /**
     * Extract full text from PDF using PDFBox
     */
    public String extractText(MultipartFile file) throws IOException {
        log.info("Extracting text from PDF: {}", file.getOriginalFilename());

        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();

        // Clean up text
        text = text.replaceAll("\\s+", " ").trim();
        log.info("Extracted {} characters from PDF", text.length());
        return text;
    }

    /**
     * Split text into overlapping chunks for better RAG retrieval
     *
     * Example with chunkSize=500, overlap=50:
     * Chunk 1: chars 0-500
     * Chunk 2: chars 450-950  (50 char overlap with chunk 1)
     * Chunk 3: chars 900-1400 (50 char overlap with chunk 2)
     */
    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // Split by sentences first for cleaner chunks
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            // If adding this sentence exceeds chunk size — save current chunk
            if (currentChunk.length() + sentence.length() > chunkSize
                    && currentChunk.length() > 0) {

                chunks.add(currentChunk.toString().trim());

                // Keep last part for overlap
                String overlap = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
            }
            currentChunk.append(sentence).append(" ");
        }

        // Add last remaining chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        log.info("Text split into {} chunks | chunkSize={} | overlap={}",
                chunks.size(), chunkSize, chunkOverlap);
        return chunks;
    }

    /**
     * Get overlap text from end of current chunk
     */
    private String getOverlapText(String text) {
        if (text.length() <= chunkOverlap) return text;
        return text.substring(text.length() - chunkOverlap);
    }
}
