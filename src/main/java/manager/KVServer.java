package manager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class KVServer {
    private static final int PORT = 8080;
    private final HttpServer server;
    private final Map<String, String> data = new HashMap<>();

    public KVServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/register", this::register);
        server.createContext("/save", this::save);
        server.createContext("/load", this::load);
    }

    private void register(HttpExchange h) throws IOException {
        try {
            System.out.println("\n/register");
            h.sendResponseHeaders(200, 0);
            h.getResponseBody().close();
        } finally {
            h.close();
        }
    }

    private void save(HttpExchange h) throws IOException {
        try {
            System.out.println("\n/save");
            if (!hasAuth(h)) {
                System.out.println("Запрос неавторизован, нужен параметр в query API_TOKEN со значением апи-ключа");
                h.sendResponseHeaders(403, 0);
                return;
            }
            String key = h.getRequestURI().getPath().substring("/save/".length());
            String value = readText(h);
            if (value.isEmpty()) {
                System.out.println("Value для сохранения пустой. key: " + key);
                h.sendResponseHeaders(400, 0);
                return;
            }
            data.put(key, value);
            System.out.println("Значение для ключа " + key + " успешно обновлено!");
            h.sendResponseHeaders(200, 0);
        } finally {
            h.close();
        }
    }

    private void load(HttpExchange h) throws IOException {
        try {
            System.out.println("\n/load");
            if (!hasAuth(h)) {
                System.out.println("Запрос неавторизован, нужен параметр в query API_TOKEN со значением апи-ключа");
                h.sendResponseHeaders(403, 0);
                return;
            }
            String key = h.getRequestURI().getPath().substring("/load/".length());
            String value = data.get(key);
            if (value == null) {
                System.out.println("Значение для ключа " + key + " не найдено");
                h.sendResponseHeaders(404, 0);
                return;
            }
            sendText(h, value);
        } finally {
            h.close();
        }
    }

    private String readText(HttpExchange h) throws IOException {
        return new String(h.getRequestBody().readAllBytes(), "UTF-8");
    }

    private void sendText(HttpExchange h, String text) throws IOException {
        byte[] resp = text.getBytes("UTF-8");
        h.getResponseHeaders().add("Content-Type", "application/json");
        h.sendResponseHeaders(200, resp.length);
        h.getResponseBody().write(resp);
    }

    private boolean hasAuth(HttpExchange h) {
        String rawQuery = h.getRequestURI().getRawQuery();
        return rawQuery != null && (rawQuery.contains("API_TOKEN=DEBUG") || rawQuery.contains("API_TOKEN=SUPER_SECRET_KEY"));
    }

    public void start() {
        System.out.println("Запускаем сервер на порту " + PORT);
        server.start();
    }

    public void stop() {
        server.stop(0);
        System.out.println("Остановили сервер на порту " + PORT);
    }
}