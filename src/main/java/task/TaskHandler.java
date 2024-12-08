package task;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.HttpTaskServer;
import manager.TaskManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TaskHandler implements HttpHandler {
    private final TaskManager taskManager;
    private final Gson gson;

    public TaskHandler(TaskManager taskManager) {
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

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/tasks/task")) {
            List<Task> tasks = taskManager.getAllTasks();
            System.out.println("Получены задачи: " + tasks);
            sendResponse(exchange, gson.toJson(tasks), 200);
        } else {
            String[] pathParts = path.split("/");
            if (pathParts.length == 4) {
                try {
                    int id = Integer.parseInt(pathParts[3]);
                    Task task = taskManager.getTaskById(id);
                    if (task != null) {
                        sendResponse(exchange, gson.toJson(task), 200);
                    } else {
                        sendResponse(exchange, "{\"error\": \"Task not found\"}", 404);
                    }
                } catch (NumberFormatException e) {
                    sendResponse(exchange, "{\"error\": \"Invalid task ID\"}", 400);
                }
            } else {
                sendResponse(exchange, "{\"error\": \"Invalid path\"}", 400);
            }
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Task task = gson.fromJson(body, Task.class);

        // Проверяем, является ли это обновлением существующей задачи
        if (task.getId() != 0) {
            // Проверяем существование задачи
            Task existingTask = taskManager.getTaskById(task.getId());
            if (existingTask == null) {
                System.out.println("Task not found with id: " + task.getId());
                sendResponse(exchange, "{\"error\": \"Task not found\"}", 404);
                return;
            }

            try {
                taskManager.updateTask(task);
                sendResponse(exchange, gson.toJson(task), 201);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("overlaps")) {
                    sendResponse(exchange, "{\"error\": \"Task time overlaps with existing task\"}", 406);
                } else {
                    sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 400);
                }
            }
        } else {
            // Создание новой задачи
            try {
                Task newTask = taskManager.addTask(task);
                sendResponse(exchange, gson.toJson(newTask), 201);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("overlaps")) {
                    sendResponse(exchange, "{\"error\": \"Task time overlaps with existing task\"}", 406);
                } else {
                    sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 400);
                }
            }
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/tasks/task")) {
            taskManager.clearTasks();
            sendResponse(exchange, "{\"status\": \"success\"}", 200);
        } else {
            String[] pathParts = path.split("/");
            if (pathParts.length == 4) {
                try {
                    int id = Integer.parseInt(pathParts[3]);
                    Task task = taskManager.getTaskById(id);
                    if (task == null) {
                        sendResponse(exchange, "{\"error\": \"Task not found\"}", 404);
                        return;
                    }
                    taskManager.deleteTask(id);
                    sendResponse(exchange, "{\"status\": \"success\"}", 200);
                } catch (NumberFormatException e) {
                    sendResponse(exchange, "{\"error\": \"Invalid task ID\"}", 400);
                }
            } else {
                sendResponse(exchange, "{\"error\": \"Invalid path\"}", 400);
            }
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