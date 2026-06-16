# Java Chatbot — Groq AI Powered Spring Boot Chatbot

A full-stack chatbot application built with Spring Boot backend
and a clean HTML/CSS/JS frontend, powered by Groq AI (Llama model).

---

## Tech Stack

| Layer     | Technology                    |
|-----------|-------------------------------|
| Backend   | Spring Boot 2.7.18 + Java 8   |
| Build     | Maven                         |
| AI        | Groq API (llama-3.1-8b-instant)|
| Frontend  | HTML + CSS + JavaScript       |
| Template  | Thymeleaf                     |
| HTTP      | Spring RestTemplate           |
| Docs      | Swagger / OpenAPI             |

---

## Project Structure

```
java-chatbot/
├── pom.xml
├── src/main/
│   ├── java/com/groq/
│   │   ├── JavaChatbotApplication.java
│   │   ├── config/
│   │   │   ├── RestTemplateConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── controller/
│   │   │   ├── ChatController.java       ← serves UI + API
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── service/
│   │   │   └── GroqChatService.java      ← Groq API integration
│   │   └── dto/
│   │       ├── ChatRequest.java
│   │       ├── ChatResponse.java
│   └── resources/
│       ├── application.properties
│       ├── templates/
│       │   └── index.html                ← Chatbot UI
│       └── static/
│           ├── css/style.css
│           └── js/script.js
```

---

## Setup & Run

### 1. Get Groq API Key
- Go to https://console.groq.com
- Sign up → API Keys → Create Key (free)

### 2. Update application.properties
```properties
groq.api.key=YOUR_GROQ_API_KEY
```

### 3. Run
```bash
mvn spring-boot:run
```

### 4. Open Browser
```
http://localhost:8080
```

---

## API

### Chat Endpoint
```
POST http://localhost:8080/api/chat
Content-Type: application/json

{
  "model": "llama-3.1-8b-instant",
  "messages": [{ "role": "user", "content": "Hello!" }],
  "temperature": 0.7,
  "max_tokens": 1024
}
```

**Response:**
```json
{
  "response": "Hello! How can I help you today?",
  "model": "llama-3.1-8b-instant",
  "status": "SUCCESS"
}
```

### Swagger UI
```
http://localhost:8080/swagger-ui/index.html
```

---

## What was fixed in JavaScript
- Added **typing indicator** (animated dots while waiting for AI)
- Added **disabled state** on input + button while waiting
- Added `.finally()` block to always re-enable input
- Used `llama-3.1-8b-instant` (active model — old llama3 models decommissioned)
