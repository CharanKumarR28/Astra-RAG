package com.groq.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String model;

    @NotEmpty(message = "messages cannot be empty")
    private List<Map<String, String>> messages;

    private double temperature;

    private int max_tokens;
}
