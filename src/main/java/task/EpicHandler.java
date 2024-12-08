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
import java.time.LocalDateTime;
import java.util.List;

public class EpicHandler implements HttpHandler {
    private final TaskManager taskManager;
    private final Gson gson;

    public EpicHandler(TaskManager taskManager) {
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
        try {
            if (path.equals("/tasks/epic")) {
                List<Epic> epics = taskManager.getAllEpics();
                sendResponse(exchange, gson.toJson(epics), 200);
            } else if (path.contains("/subtasks")) {
                String[] parts = path.split("/");
                int epicId = Integer.parseInt(parts[parts.length - 2]);
                Epic epic = taskManager.getEpicById(epicId);

                if (epic == null) {
                    sendResponse(exchange, "{\"error\": \"Epic not found\"}", 404);
                    return;
                }

                List<Subtask> subtasks = taskManager.getSubtasksByEpicId(epicId);
                sendResponse(exchange, gson.toJson(subtasks), 200);
            } else {
                String[] parts = path.split("/");
                int id = Integer.parseInt(parts[parts.length - 1]);
                Epic epic = taskManager.getEpicById(id);
                if (epic != null) {
                    sendResponse(exchange, gson.toJson(epic), 200);
                } else {
                    sendResponse(exchange, "{\"error\": \"Epic not found\"}", 404);
                }
            }
        } catch (NumberFormatException e) {
            sendResponse(exchange, "{\"error\": \"Invalid epic ID format\"}", 400);
        } catch (Exception e) {
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 500);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Epic epic = gson.fromJson(body, Epic.class);

            // Проверяем пересечения подзадач перед сохранением
            if (epic.getSubtasks() != null && epic.getSubtasks().size() > 1) {
                List<Subtask> subtasks = epic.getSubtasks();
                for (int i = 0; i < subtasks.size(); i++) {
                    Subtask current = subtasks.get(i);
                    if (current.getStartTime() == null || current.getDuration() == null) {
                        continue;
                    }

                    // Проверяем пересечение с другими подзадачами
                    for (int j = i + 1; j < subtasks.size(); j++) {
                        Subtask other = subtasks.get(j);
                        if (other.getStartTime() == null || other.getDuration() == null) {
                            continue;
                        }

                        if (isTasksOverlap(current, other)) {
                            sendResponse(exchange,
                                    "{\"error\": \"Subtasks time overlaps within epic\"}",
                                    406);
                            return;
                        }
                    }
                }
            }

            try {
                if (epic.getId() != 0) {
                    if (taskManager.getEpicById(epic.getId()) == null) {
                        sendResponse(exchange, "{\"error\": \"Epic not found\"}", 404);
                        return;
                    }
                    taskManager.updateEpic(epic);
                } else {
                    epic = taskManager.addEpic(epic);
                }
                sendResponse(exchange, gson.toJson(epic), 201);
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("overlaps")) {
                    sendResponse(exchange,
                            "{\"error\": \"Epic subtasks time overlaps with existing tasks\"}",
                            406);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 400);
        }
    }

    private boolean isTasksOverlap(Task task1, Task task2) {
        LocalDateTime start1 = task1.getStartTime();
        LocalDateTime end1 = task1.getEndTime();
        LocalDateTime start2 = task2.getStartTime();
        LocalDateTime end2 = task2.getEndTime();

        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        try {
            if (path.equals("/tasks/epic")) {
                taskManager.clearEpics();
                sendResponse(exchange, "{\"status\": \"success\"}", 200);
            } else {
                String[] parts = path.split("/");
                int id = Integer.parseInt(parts[parts.length - 1]);

                // Проверяем существование эпика перед удалением
                Epic epic = taskManager.getEpicById(id);
                if (epic == null) {
                    sendResponse(exchange, "{\"error\": \"Epic not found\"}", 404);
                    return;
                }

                taskManager.deleteEpic(id);
                sendResponse(exchange, "{\"status\": \"success\"}", 200);
            }
        } catch (NumberFormatException e) {
            sendResponse(exchange, "{\"error\": \"Invalid epic ID format\"}", 400);
        } catch (Exception e) {
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 500);
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