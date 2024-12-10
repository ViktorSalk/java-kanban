package task;

import com.sun.net.httpserver.HttpExchange;
import manager.TaskManager;
import http.HttpStatusCode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubtaskHandler extends AbstractTaskHandler {
    public SubtaskHandler(TaskManager taskManager) {
        super(taskManager);
    }

    @Override
    protected void handleGet(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/tasks/subtask")) {
            List<Subtask> subtasks = taskManager.getAllSubtasks();
            sendResponse(exchange, gson.toJson(subtasks), HttpStatusCode.OK.getCode());
        } else if (path.contains("/epic/")) {
            String[] parts = path.split("/");
            int epicId = Integer.parseInt(parts[parts.length - 1]);
            List<Subtask> subtasks = taskManager.getSubtasksByEpicId(epicId);
            sendResponse(exchange, gson.toJson(subtasks), HttpStatusCode.OK.getCode());
        } else {
            String[] parts = path.split("/");
            int id = Integer.parseInt(parts[parts.length - 1]);
            Subtask subtask = taskManager.getSubtaskById(id);
            if (subtask != null) {
                sendResponse(exchange, gson.toJson(subtask), HttpStatusCode.OK.getCode());
            } else {
                sendResponse(exchange, "{\"error\": \"Subtask not found\"}", HttpStatusCode.NOT_FOUND.getCode());
            }
        }
    }

    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        try {
            InputStream inputStream = exchange.getRequestBody();
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Subtask subtask = gson.fromJson(body, Subtask.class);

            try {
                if (subtask.getId() != 0) {
                    // Обновление существующей подзадачи
                    if (taskManager.getSubtaskById(subtask.getId()) == null) {
                        sendResponse(exchange, "{\"error\": \"Subtask not found\"}", HttpStatusCode.NOT_FOUND.getCode());
                        return;
                    }
                    taskManager.updateSubtask(subtask);
                } else {
                    // Создание новой подзадачи
                    subtask = taskManager.addSubtask(subtask);
                }
                sendResponse(exchange, gson.toJson(subtask), HttpStatusCode.CREATED.getCode());
            } catch (IllegalArgumentException e) {
                if (e.getMessage().contains("overlaps")) {
                    sendResponse(exchange, "{\"error\": \"Task time overlaps with existing task\"}", HttpStatusCode.NOT_ACCEPTABLE.getCode());
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            sendResponse(exchange, "{\"error\": \"" + e.getMessage() + "\"}", HttpStatusCode.BAD_REQUEST.getCode());
        }
    }

    @Override
    protected void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/tasks/subtask")) {
            taskManager.clearSubtasks();
            sendResponse(exchange, "{\"status\": \"success\"}", HttpStatusCode.OK.getCode());
        } else {
            String[] parts = path.split("/");
            int id = Integer.parseInt(parts[parts.length - 1]);
            taskManager.deleteSubtask(id);
            sendResponse(exchange, "{\"status\": \"success\"}", HttpStatusCode.OK.getCode());
        }
    }
}