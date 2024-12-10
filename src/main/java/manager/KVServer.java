package manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class KVServer {
    private static final int PORT = 8078;
    private final String apiToken;
    private final HttpServer server;
    private final Map<String, String> data;

    public KVServer() throws IOException {
        apiToken = generateApiToken();
        server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
        data = new HashMap<>();

        server.createContext("/register", this::handleRegister);
        server.createContext("/save", this::handleSave);
        server.createContext("/load", this::handleLoad);
    }

    private void handleRegister(HttpExchange h) throws IOException {
        try {
            System.out.println("\n/register");
            if (!hasAuth(h)) {
                System.out.println("Запрос неавторизован, нужен параметр в query API_TOKEN со значением апи-ключа");
                sendResponse(h, "Unauthorized", 403);
                return;
            }
            if ("GET".equals(h.getRequestMethod())) {
                sendResponse(h, apiToken, 200);
            } else {
                System.out.println("/register ждёт GET-запрос, а получил " + h.getRequestMethod());
                sendResponse(h, "Method not allowed", 405);
            }
        } finally {
            h.close();
        }
    }

    private void handleSave(HttpExchange h) throws IOException {
        try {
            System.out.println("\n/save");
            if (!hasAuth(h)) {
                System.out.println("Запрос неавторизован, нужен параметр в query API_TOKEN со значением апи-ключа");
                sendResponse(h, "Unauthorized", 403);
                return;
            }
            if ("POST".equals(h.getRequestMethod())) {
                String key = h.getRequestURI().getPath().substring("/save/".length());
                if (key.isEmpty()) {
                    sendResponse(h, "Key is empty", 400);
                    return;
                }
                String value = new String(h.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                if (value.isEmpty()) {
                    sendResponse(h, "Value is empty", 400);
                    return;
                }
                data.put(key, value);
                sendResponse(h, "Value saved", 201);
            } else {
                System.out.println("/save ждёт POST-запрос, а получил " + h.getRequestMethod());
                sendResponse(h, "Method not allowed", 405);
            }
        } finally {
            h.close();
        }
    }

    private void handleLoad(HttpExchange h) throws IOException {
        try {
            System.out.println("\n/load");
            if (!hasAuth(h)) {
                System.out.println("Запрос неавторизован, нужен параметр в query API_TOKEN со значением апи-ключа");
                sendResponse(h, "Unauthorized", 403);
                return;
            }
            if ("GET".equals(h.getRequestMethod())) {
                String key = h.getRequestURI().getPath().substring("/load/".length());
                if (key.isEmpty()) {
                    sendResponse(h, "Key is empty", 400);
                    return;
                }
                String value = data.get(key);
                if (value == null) {
                    sendResponse(h, "Value not found", 404);
                    return;
                }
                sendResponse(h, value, 200);
            } else {
                System.out.println("/load ждёт GET-запрос, а получил " + h.getRequestMethod());
                sendResponse(h, "Method not allowed", 405);
            }
        } finally {
            h.close();
        }
    }

    private void sendResponse(HttpExchange h, String response, int statusCode) throws IOException {
        h.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        h.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = h.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String generateApiToken() {
        return "" + System.currentTimeMillis();
    }

    private boolean hasAuth(HttpExchange h) {
        String rawQuery = h.getRequestURI().getRawQuery();
        return rawQuery != null && (rawQuery.contains("API_TOKEN=" + apiToken) || rawQuery.contains("API_TOKEN=DEBUG"));
    }

    public void start() {
        System.out.println("Запускаем сервер на порту " + PORT);
        System.out.println("API_TOKEN: " + apiToken);
        server.start();
    }

    public void stop() {
        server.stop(0);
    }
}