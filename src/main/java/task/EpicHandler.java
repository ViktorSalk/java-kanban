package task;

import com.sun.net.httpserver.HttpExchange;
import manager.TaskManager;
import http.HttpStatusCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

public class EpicHandler extends AbstractTaskHandler {
    public EpicHandler(TaskManager taskManager) {
        super(taskManager);
    }

    @Override
    protected void handleGet(HttpExchange exchange, String path) throws IOException {
        try {
            if (path.equals("/tasks/epic")) {
                List<Epic> epics = taskManager.getAllEpics();
                sendResponse(exchange, gson.toJson(epics), HttpStatusCode.OK.getCode());
            } else if (path.contains("/subtasks")) {
                String[] parts = path.split("/");
                int epicId = Integer.parseInt(parts[parts.length - 2]);
                Epic epic = taskManager.getEpicById(epicId);

                if (epic == null) {
                    sendResponse(exchange, "{\"error\": \"Epic not found\"}", HttpStatusCode.NOT_FOUND.getCode());
                    return;
                }

                List<Subtask> subtasks = taskManager.getSubtasksByEpicId(epicId);
                sendResponse(exchange, gson.toJson(subtasks), HttpStatusCode.OK.getCode());
            } else {
                String[] parts = path.split("/");
                int id = Integer.parseInt(parts[parts.length - 1]);
                Epic epic = taskManager.getEpicById(id);
                if (epic != null) {
                    sendResponse(exchange, gson.toJson(epic), HttpStatusCode.OK.getCode());
                } else {
                    sendResponse(exchange, "{\"error\": \"Epic not found\"}", HttpStatusCode.NOT_FOUND.getCode());
                }
            }
        } catch (NumberFormatException exception) {
            System.out.println("Некорректный формат ID: " + exception.getMessage());
            sendResponse(exchange, "{\"error\": \"Invalid epic ID format\"}", HttpStatusCode.BAD_REQUEST.getCode());
        } catch (Exception exception) {
            System.out.println("Ошибка при обработке GET-запроса: " + exception.getMessage());
            sendResponse(exchange, "{\"error\": \"" + exception.getMessage() + "\"}", HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
        }
    }

    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        try {
            try (InputStream inputStream = exchange.getRequestBody()) {
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
            }
        } catch (Exception e) {
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", 400);
        }
    }

    @Override
    protected void handleDelete(HttpExchange exchange, String path) throws IOException {
        try {
            if (path.equals("/tasks/epic")) {
                taskManager.clearEpics();
                sendResponse(exchange, "{\"status\": \"success\"}", 200);
            } else {
                String[] parts = path.split("/");
                int id = Integer.parseInt(parts[parts.length - 1]);

                Epic epic = taskManager.getEpicById(id);
                if (epic == null) {
                    sendResponse(exchange, "{\"error\": \"Epic not found\"}", 404);
                    return;
                }

                taskManager.deleteEpic(id);
                sendResponse(exchange, "{\"status\": \"success\"}", 200);
            }
        } catch (NumberFormatException exception) {
            System.out.println("Некорректный формат ID: " + exception.getMessage());
            sendResponse(exchange, "{\"error\": \"Invalid epic ID format\"}", 400);
        } catch (Exception exception) {
            System.out.println("Ошибка при обработке DELETE-запроса: " + exception.getMessage());
            sendResponse(exchange, "{\"error\": \"" + exception.getMessage() + "\"}", 500);
        }
    }

    private boolean isTasksOverlap(Task task1, Task task2) {
        LocalDateTime start1 = task1.getStartTime();
        LocalDateTime end1 = task1.getEndTime();
        LocalDateTime start2 = task2.getStartTime();
        LocalDateTime end2 = task2.getEndTime();

        return !(end1.isBefore(start2) || start1.isAfter(end2));
    }
}