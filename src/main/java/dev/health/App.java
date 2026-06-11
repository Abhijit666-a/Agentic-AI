package dev.health;

import io.github.cdimascio.dotenv.Dotenv;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class App {
    private static ChatLanguageModel model;

    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Blood Work Analyzer Web Application...");

        // 1. Load the environment variables from .env
        Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();

        String apiKey = dotenv.get("GEMINI_API_KEY");
        if (apiKey == null) {
            apiKey = dotenv.get("GEMINI_API_KEY ");
        }
        if (apiKey == null) {
            apiKey = dotenv.get("GOOGLE_API_KEY");
        }
        if (apiKey == null) {
            apiKey = dotenv.get("GOOGLE_API_KEY ");
        }
        
        if (apiKey != null) {
            apiKey = apiKey.trim();
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("CRITICAL: Neither GEMINI_API_KEY nor GOOGLE_API_KEY was found in your .env file!");
            System.exit(1);
        }

        String modelName = dotenv.get("MODEL_NAME");
        if (modelName == null || modelName.trim().isEmpty()) {
            modelName = "gemma-4-31b-it"; // Matches streamlit_app model
        }

        System.out.println("Configuring LLM adapter...");
        System.out.println(" - Model Name: " + modelName);

        // 2. Initialize the OpenAI-compatible LangChain4j adapter for Gemini
        model = OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/")
            .modelName(modelName)
            .logRequests(false)
            .logResponses(false)
            .build();

        // 3. Start the Embedded HttpServer on port 8080
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // Router for UI
        server.createContext("/", new IndexHandler());
        
        // Router for Analysis API endpoint
        server.createContext("/analyze", new AnalyzeHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("\n=============================================");
        System.out.println("  Web Server is running at: http://localhost:" + port);
        System.out.println("  Press Ctrl+C to stop the application.");
        System.out.println("=============================================\n");
    }

    // Handler to serve the HTML/CSS/JS frontend
    static class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
                return;
            }

            byte[] responseBytes = IndexHtml.CONTENT.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    // Handler to process the clinical report analysis
    static class AnalyzeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            // Read raw POST body parameters
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String bloodReport = getFormParam(body, "blood_report");
            if (bloodReport == null || bloodReport.trim().isEmpty()) {
                sendErrorResponse(exchange, "Blood report input is empty.");
                return;
            }

            System.out.println("\n[API] Received report analysis request. Length: " + bloodReport.length() + " chars.");

            try {
                // Stage 1: Value Extraction and Status Classification
                System.out.println("[API] Executing Stage 1: Extracting values...");
                String extractionPrompt = String.format(
                    "You are a medical data extraction assistant.\n\n" +
                    "From the blood report below, extract ALL test values and classify each one as HIGH, LOW, or NORMAL " +
                    "based on the reference ranges provided in the report.\n\n" +
                    "Format your response as:\n" +
                    "- Test Name: value | Status: HIGH/LOW/NORMAL | Reference: range\n\n" +
                    "Blood Report:\n%s",
                    bloodReport
                );
                String extractedValues = model.generate(extractionPrompt);
                System.out.println("[API] Stage 1 finished.");

                // Stage 2: Health Summary and Indian Diet Plan Generation
                System.out.println("[API] Executing Stage 2: Generating Health Summary and Diet Plan...");
                String dietPrompt = String.format(
                    "You are a clinical nutritionist specializing in Indian dietary habits.\n\n" +
                    "Based on the blood work analysis below, provide two clearly separated sections:\n\n" +
                    "SECTION 1 - HEALTH SUMMARY:\n" +
                    "Write 4-5 lines explaining the patient's condition in simple, non-technical language.\n\n" +
                    "SECTION 2 - INDIAN DIET PLAN:\n" +
                    "List foods to eat more of and foods to avoid, using commonly available Indian foods " +
                    "like dal, sabzi, roti, rice, etc. Keep it practical and concise.\n\n" +
                    "Blood Work Analysis:\n%s",
                    extractedValues
                );
                String fullResponse = model.generate(dietPrompt);
                System.out.println("[API] Stage 2 finished.");

                // Parse the response into structured segments
                String healthSummary;
                String dietPlan;

                if (fullResponse.contains("SECTION 2")) {
                    String[] parts = fullResponse.split("SECTION 2");
                    healthSummary = parts[0].replace("SECTION 1 - HEALTH SUMMARY:", "")
                                            .replace("SECTION 1", "")
                                            .trim();
                    dietPlan = ("SECTION 2" + parts[1]).replace("SECTION 2 - INDIAN DIET PLAN:", "")
                                                       .replace("SECTION 2", "")
                                                       .trim();
                } else {
                    healthSummary = fullResponse;
                    dietPlan = "";
                }

                // Construct escaped JSON manually to minimize dependencies
                String jsonResponse = String.format(
                    "{\"healthSummary\": %s, \"dietPlan\": %s}",
                    escapeJson(healthSummary),
                    escapeJson(dietPlan)
                );

                byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                System.out.println("[API] Response successfully sent.");

            } catch (Exception e) {
                System.err.println("[API] Error calling LLM service: " + e.getMessage());
                e.printStackTrace();
                sendErrorResponse(exchange, "Error generating LLM response: " + e.getMessage());
            }
        }

        private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
            String jsonError = String.format("{\"error\": %s}", escapeJson(errorMessage));
            byte[] errorBytes = jsonError.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(500, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        }

        // Decode URL Form Parameters
        private String getFormParam(String body, String paramName) {
            try {
                for (String pair : body.split("&")) {
                    String[] parts = pair.split("=", 2);
                    if (parts.length == 2 && URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(paramName)) {
                        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        // Helper to escape values safely for JSON format
        private String escapeJson(String val) {
            if (val == null) {
                return "null";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("\"");
            for (int i = 0; i < val.length(); i++) {
                char ch = val.charAt(i);
                switch (ch) {
                    case '\\': sb.append("\\\\"); break;
                    case '"': sb.append("\\\""); break;
                    case '\b': sb.append("\\b"); break;
                    case '\f': sb.append("\\f"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    default:
                        if (ch < ' ') {
                            String hex = Integer.toHexString(ch);
                            sb.append("\\u").append("0".repeat(4 - hex.length())).append(hex);
                        } else {
                            sb.append(ch);
                        }
                }
            }
            sb.append("\"");
            return sb.toString();
        }
    }
}
