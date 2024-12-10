package task;

import com.sun.net.httpserver.HttpExchange;
import http.HttpStatusCode;
import manager.TaskManager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TaskHandler extends AbstractTaskHandler {


    public TaskHandler(TaskManager taskManager) {
        super(taskManager);
    }

    protected void handleGet(HttpExchange httpExchange, String path) throws IOException {
        if (path.equals("/tasks/task")) {
            List<Task> tasks = taskManager.getAllTasks();
            sendResponse(httpExchange, gson.toJson(tasks), HttpStatusCode.OK);
        } else {
            String[] pathParts = path.split("/");
            if (pathParts.length == 4) {
                try {
                    int taskId = Integer.parseInt(pathParts[3]);
                    Task task = taskManager.getTaskById(taskId);
                    if (task != null) {
                        sendResponse(httpExchange, gson.toJson(task), HttpStatusCode.OK);
                    } else {
                        sendResponse(httpExchange,
                                "{\"error\": \"Task not found\"}",
                                HttpStatusCode.NOT_FOUND);
                    }
                } catch (NumberFormatException formatException) {
                    sendResponse(httpExchange,
                            "{\"error\": \"Invalid task ID\"}",
                            HttpStatusCode.BAD_REQUEST);
                }
            } else {
                sendResponse(httpExchange,
                        "{\"error\": \"Invalid path\"}",
                        HttpStatusCode.BAD_REQUEST);
            }
        }
    }

    protected void handlePost(HttpExchange httpExchange) throws IOException {
        try {
            String body = new String(httpExchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Task task = gson.fromJson(body, Task.class);

            if (task.getId() != 0) {
                Task existingTask = taskManager.getTaskById(task.getId());
                if (existingTask == null) {
                    sendResponse(httpExchange,
                            "{\"error\": \"Task not found\"}",
                            HttpStatusCode.NOT_FOUND);
                    return;
                }
                try {
                    taskManager.updateTask(task);
                    sendResponse(httpExchange, gson.toJson(task), HttpStatusCode.CREATED);
                } catch (IllegalArgumentException exception) {
                    if (exception.getMessage().contains("overlaps")) {
                        sendResponse(httpExchange,
                                "{\"error\": \"Task time overlaps with existing task\"}",
                                HttpStatusCode.NOT_ACCEPTABLE);
                    } else {
                        throw exception;
                    }
                }
            } else {
                try {
                    Task newTask = taskManager.addTask(task);
                    sendResponse(httpExchange, gson.toJson(newTask), HttpStatusCode.CREATED);
                } catch (IllegalArgumentException exception) {
                    if (exception.getMessage().contains("overlaps")) {
                        sendResponse(httpExchange,
                                "{\"error\": \"Task time overlaps with existing task\"}",
                                HttpStatusCode.NOT_ACCEPTABLE);
                    } else {
                        throw exception;
                    }
                }
            }
        } catch (Exception exception) {
            if (exception.getMessage().contains("overlaps")) {
                sendResponse(httpExchange,
                        "{\"error\": \"Task time overlaps with existing task\"}",
                        HttpStatusCode.NOT_ACCEPTABLE);
            } else {
                System.out.println("Произошла ошибка: " + exception.getMessage());
                sendResponse(httpExchange,
                        "{\"error\": \"" + exception.getMessage() + "\"}",
                        HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
        }
    }

    protected void handleDelete(HttpExchange httpExchange, String path) throws IOException {
        if (path.equals("/tasks/task")) {
            taskManager.clearTasks();
            sendResponse(httpExchange, "{\"status\": \"success\"}", HttpStatusCode.OK);
        } else {
            String[] pathParts = path.split("/");
            if (pathParts.length == 4) {
                try {
                    int taskId = Integer.parseInt(pathParts[3]);
                    Task task = taskManager.getTaskById(taskId);
                    if (task == null) {
                        sendResponse(httpExchange,
                                "{\"error\": \"Task not found\"}",
                                HttpStatusCode.NOT_FOUND);
                        return;
                    }
                    taskManager.deleteTask(taskId);
                    sendResponse(httpExchange, "{\"status\": \"success\"}", HttpStatusCode.OK);
                } catch (NumberFormatException formatException) {
                    sendResponse(httpExchange,
                            "{\"error\": \"Invalid task ID\"}",
                            HttpStatusCode.BAD_REQUEST);
                }
            } else {
                sendResponse(httpExchange,
                        "{\"error\": \"Invalid path\"}",
                        HttpStatusCode.BAD_REQUEST);
            }
        }
    }

    private void sendResponse(HttpExchange httpExchange, String response, HttpStatusCode statusCode)
            throws IOException {
        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        httpExchange.sendResponseHeaders(statusCode.getCode(), responseBytes.length);

        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }
}