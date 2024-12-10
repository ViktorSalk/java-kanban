package task;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.HttpTaskServer;
import manager.TaskManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class AbstractTaskHandler implements HttpHandler {
    protected final TaskManager taskManager;
    protected final Gson gson;

    public AbstractTaskHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            System.out.println("Received " + method + " request for path: " + path);

            switch (method) {
                case "GET":
                    handleGet(exchange, path);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange, path);
                    break;
                default:
                    sendResponse(exchange, "{\"error\": \"Method not allowed\"}", 405);
            }
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.getMessage());
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 500);
        }
    }

    protected abstract void handleGet(HttpExchange exchange, String path) throws IOException;

    protected abstract void handlePost(HttpExchange exchange) throws IOException;

    protected abstract void handleDelete(HttpExchange exchange, String path) throws IOException;

    protected void sendResponse(HttpExchange exchange, String response, int code) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}