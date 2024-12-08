package task;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.HttpTaskServer;
import manager.TaskManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubtaskHandler implements HttpHandler {
    private final TaskManager taskManager;
    private final Gson gson;

    public SubtaskHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            System.out.println("Получен " + method + " запрос по пути: " + path);

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
            System.out.println("Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 500);
        }
    }


    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Subtask subtask = gson.fromJson(body, Subtask.class);

            try {
                if (subtask.getId() != 0) {
                    // Обновление существующей подзадачи
                    if (taskManager.getSubtaskById(subtask.getId()) == null) {
                        sendResponse(exchange, "{\"error\": \"Subtask not found\"}", 404);
                        return;
                    }
                    taskManager.updateSubtask(subtask);
                } else {
                    // Создание новой подзадачи
                    subtask = taskManager.addSubtask(subtask);
                }
                sendResponse(exchange, gson.toJson(subtask), 201);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("overlaps")) {
                    sendResponse(exchange, "{\"error\": \"Task time overlaps with existing task\"}", 406);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 400);
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/tasks/subtask")) {
            List<Subtask> subtasks = taskManager.getAllSubtasks();
            sendResponse(exchange, gson.toJson(subtasks), 200);
        } else if (path.contains("/epic/")) {
            String[] parts = path.split("/");
            int epicId = Integer.parseInt(parts[parts.length - 1]);
            List<Subtask> subtasks = taskManager.getSubtasksByEpicId(epicId);
            sendResponse(exchange, gson.toJson(subtasks), 200);
        } else {
            String[] parts = path.split("/");
            int id = Integer.parseInt(parts[parts.length - 1]);
            Subtask subtask = taskManager.getSubtaskById(id);
            if (subtask != null) {
                sendResponse(exchange, gson.toJson(subtask), 200);
            } else {
                sendResponse(exchange, "{\"error\": \"Subtask not found\"}", 404);
            }
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/tasks/subtask")) {
            taskManager.clearSubtasks();
            sendResponse(exchange, "{\"status\": \"success\"}", 200);
        } else {
            String[] parts = path.split("/");
            int id = Integer.parseInt(parts[parts.length - 1]);
            taskManager.deleteSubtask(id);
            sendResponse(exchange, "{\"status\": \"success\"}", 200);
        }
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