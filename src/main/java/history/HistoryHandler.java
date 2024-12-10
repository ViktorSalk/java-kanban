package history;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import http.HttpStatusCode;
import manager.TaskManager;
import manager.HttpTaskServer;
import task.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HistoryHandler implements HttpHandler {
    private final TaskManager taskManager;
    private final Gson gson;

    public HistoryHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.gson = HttpTaskServer.getGson();
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try (httpExchange) {
            String path = httpExchange.getRequestURI().getPath();
            String method = httpExchange.getRequestMethod();

            if (!"GET".equals(method)) {
                sendResponse(httpExchange,
                        "{\"error\": \"Method not allowed\"}",
                        HttpStatusCode.METHOD_NOT_ALLOWED);
                return;
            }

            if (!"/tasks/history".equals(path)) {
                sendResponse(httpExchange,
                        "{\"error\": \"Invalid path\"}",
                        HttpStatusCode.NOT_FOUND);
                return;
            }

            List<Task> history = taskManager.getHistory();
            sendResponse(httpExchange, gson.toJson(history), HttpStatusCode.OK);

        } catch (Exception exception) {
            System.out.println("Произошла ошибка при получении истории: " + exception.getMessage());
            sendResponse(httpExchange,
                    "{\"error\": \"" + exception.getMessage() + "\"}",
                    HttpStatusCode.INTERNAL_SERVER_ERROR);
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