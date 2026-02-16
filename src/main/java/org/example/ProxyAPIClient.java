package org.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Класс-обёртка для работы с ProxyAPI
 * Документация: https://proxyapi.ru/docs/overview
 */
public class ProxyAPIClient {
    private static final String BASE_URL = "https://api.proxyapi.ru";
    private static final String API_KEY = "sk-aGpjyQKhiyXMnm4q61MZ8SVD0bPa5OUj";
    
    private final HttpClient httpClient;
    private final String apiKey;
    
    /**
     * Конструктор с использованием ключа по умолчанию
     */
    public ProxyAPIClient() {
        this(API_KEY);
    }
    
    /**
     * Конструктор с указанием API ключа
     * @param apiKey ключ API для авторизации
     */
    public ProxyAPIClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Получает баланс аккаунта
     * @return баланс в виде строки JSON
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    public String getBalance() throws Exception {
        String url = BASE_URL + "/proxyapi/balance";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка при получении баланса. Код ответа: " + response.statusCode() + 
                    ", Тело ответа: " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Получает баланс аккаунта и возвращает как объект BalanceResponse
     * @return объект BalanceResponse с информацией о балансе
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    public BalanceResponse getBalanceResponse() throws Exception {
        String jsonResponse = getBalance();
        // Простой парсинг JSON (в реальном проекте лучше использовать Jackson или Gson)
        return BalanceResponse.fromJson(jsonResponse);
    }

    private static final String CHAT_MODEL = "gpt-5.1-codex-mini";
    private static final String RESPONSES_URL = BASE_URL + "/openai/v1/responses";

    /**
     * Отправляет сообщение модели и возвращает ответ.
     * Использует Responses API (v1/responses), т.к. gpt-5.1-codex-mini не поддерживает chat/completions.
     * Авторизация по Bearer ключу.
     *
     * @param userMessage текст сообщения пользователя
     * @return ответ модели
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    public String chat(String userMessage) throws Exception {
        String escapedContent = escapeJson(userMessage);
        String requestBody = "{\"model\":\"" + CHAT_MODEL + "\",\"input\":[{\"role\":\"user\",\"content\":[{\"type\":\"input_text\",\"text\":\"" + escapedContent + "\"}]}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESPONSES_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка при обращении к модели. Код ответа: " + response.statusCode() +
                    ", Тело ответа: " + response.body());
        }

        return extractContentFromResponsesApi(response.body());
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Парсит ответ Responses API (output[].content[].text для output_text)
     */
    private static String extractContentFromResponsesApi(String json) {
        // Ищем "type":"output_text" и затем "text":"..."
        int outputTextPos = json.indexOf("\"output_text\"");
        if (outputTextPos == -1) return "";
        int textKeyPos = json.indexOf("\"text\":", outputTextPos);
        if (textKeyPos == -1) return "";
        int contentStart = json.indexOf("\"", textKeyPos + 7) + 1;
        int contentEnd = contentStart;
        boolean escaped = false;
        while (contentEnd < json.length()) {
            char c = json.charAt(contentEnd);
            if (escaped) {
                escaped = false;
                contentEnd++;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                contentEnd++;
                continue;
            }
            if (c == '"') break;
            contentEnd++;
        }
        return unescapeJson(json.substring(contentStart, contentEnd));
    }

    private static String unescapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'b' -> { sb.append('\b'); i++; }
                    case 'f' -> { sb.append('\f'); i++; }
                    case 'u' -> {
                        if (i + 5 < s.length()) {
                            try {
                                int codePoint = Integer.parseInt(s.substring(i + 2, i + 6), 16);
                                sb.append((char) codePoint);
                                i += 5;
                            } catch (NumberFormatException e) {
                                sb.append(s.charAt(i));
                            }
                        } else {
                            sb.append(s.charAt(i));
                        }
                    }
                    default -> sb.append(s.charAt(i));
                }
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }
}
