package task;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import manager.HttpTaskServer;
import manager.TaskManager;
import http.HttpStatusCode;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PrioritizedHandler implements HttpHandler {
    private final TaskManager taskManager;
    private final Gson gson;

    public PrioritizedHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (!"GET".equals(method)) {
                sendResponse(exchange, "{\"error\": \"Method not allowed\"}", HttpStatusCode.METHOD_NOT_ALLOWED.getCode());
                return;
            }

            if (!"/tasks/prioritized".equals(path)) {
                sendResponse(exchange, "{\"error\": \"Invalid path\"}", HttpStatusCode.NOT_FOUND.getCode());
                return;
            }

            List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();
            sendResponse(exchange, gson.toJson(prioritizedTasks), HttpStatusCode.OK.getCode());
        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    private void handleGetPrioritized(HttpExchange exchange) throws IOException {
        List<Task> prioritizedTasks = taskManager.getPrioritizedTasks();
        sendResponse(exchange, gson.toJson(prioritizedTasks), HttpStatusCode.OK.getCode());
    }

    private void sendResponse(HttpExchange exchange, String response, int code) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}