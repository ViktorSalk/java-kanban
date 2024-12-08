package manager;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import history.HistoryHandler;
import task.EpicHandler;
import task.PrioritizedHandler;
import task.SubtaskHandler;
import task.TaskHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDateTime;

public class HttpTaskServer {
    private final TaskManager taskManager;
    private final HttpServer httpServer;
    private static final int PORT = 8080;

    // Адаптер для Duration
    private static class DurationAdapter implements JsonSerializer<Duration>, JsonDeserializer<Duration> {
        @Override
        public JsonElement serialize(Duration duration, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(duration.toMinutes());
        }

        @Override
        public Duration deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return Duration.ofMinutes(json.getAsLong());
        }
    }

    // Адаптер для LocalDateTime
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime dateTime, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(dateTime.toString());
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString());
        }
    }

    // Создаем Gson с адаптерами
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public HttpTaskServer(TaskManager taskManager) throws IOException {
        this.taskManager = taskManager;
        this.httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        configureHandlers();
    }

    private void configureHandlers() {
        httpServer.createContext("/tasks/task", new TaskHandler(taskManager));
        httpServer.createContext("/tasks/subtask", new SubtaskHandler(taskManager));
        httpServer.createContext("/tasks/epic", new EpicHandler(taskManager));
        httpServer.createContext("/tasks/history", new HistoryHandler(taskManager));
        httpServer.createContext("/tasks/prioritized", new PrioritizedHandler(taskManager));
    }

    public void start() {
        httpServer.start();
        System.out.println("HTTP-сервер запущен по порту " + PORT);
    }

    public void stop() {
        httpServer.stop(0);
        System.out.println("HTTP-сервер остановлен на порту " + PORT);
    }

    public static Gson getGson() {
        return gson;
    }

    public static void main(String[] args) {
        try {
            // Проверяем, не занят ли порт
            try {
                new Socket("localhost", PORT).close();
                System.out.println("Port " + PORT + " уже используется. Пожалуйста, освободите порт и повторите попытку.");
                return;
            } catch (IOException ignored) {
                // Порт свободен, можно продолжать
            }

            // Создаем и запускаем HttpTaskServer
            TaskManager taskManager = Managers.getDefault();
            HttpTaskServer server = new HttpTaskServer(taskManager);

            // Добавляем shutdown hook для корректного завершения сервера
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Выключение сервера...");
                server.stop();
            }));

            server.start();

        } catch (IOException e) {
            System.out.println("Не удалось запустить сервер: " + e.getMessage());
        }
    }
}