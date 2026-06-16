package com.groq.controller;

import com.groq.dto.ChatRequest;
import com.groq.dto.ChatResponse;
import com.groq.dto.DocumentUploadResponse;
import com.groq.service.GroqChatService;
import com.groq.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "Astra Chatbot", description = "RAG-powered Groq AI Chatbot API")
public class ChatController {

    private final GroqChatService groqChatService;
    private final RagService ragService;

    /**
     * Serve the chatbot UI
     * GET http://localhost:8080/
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * Chat API — supports both normal and RAG mode
     * POST http://localhost:8080/api/chat
     */
    @PostMapping("/api/chat")
    @ResponseBody
    @Operation(
            summary = "Send a chat message",
            description = "Sends message to Groq AI. If PDFs are uploaded, automatically uses RAG to answer from document content."
    )
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request received");
        ChatResponse response = groqChatService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * PDF Upload API — ingest PDF into Pinecone
     * POST http://localhost:8080/api/upload
     */
    @PostMapping("/api/upload")
    @ResponseBody
    @Operation(
            summary = "Upload a PDF document",
            description = "Uploads and indexes a PDF into Pinecone. After upload, Astra can answer questions about this document."
    )
    public ResponseEntity<DocumentUploadResponse> uploadPdf(
            @RequestParam("file") MultipartFile file) {

        log.info("PDF upload request | file={} | size={}KB",
                file.getOriginalFilename(),
                file.getSize() / 1024);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    DocumentUploadResponse.builder()
                            .status("FAILED")
                            .message("Please select a PDF file to upload.")
                            .build()
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest().body(
                    DocumentUploadResponse.builder()
                            .status("FAILED")
                            .message("Only PDF files are supported.")
                            .build()
            );
        }

        DocumentUploadResponse response = ragService.ingestPdf(file);
        return ResponseEntity.ok(response);
    }
}
