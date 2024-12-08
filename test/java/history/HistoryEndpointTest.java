package history;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import manager.HttpTaskServer;
import manager.Managers;
import manager.TaskManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.*;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryEndpointTest {
    private static final String BASE_URL = "http://localhost:8080";
    private HttpTaskServer server;
    private TaskManager manager;
    private HttpClient client;

    // Адаптер для Duration
    private static class DurationAdapter implements JsonSerializer<Duration>, JsonDeserializer<Duration> {
        @Override
        public JsonElement serialize(Duration duration, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(duration.toMinutes());
        }

        @Override
        public Duration deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
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
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            return LocalDateTime.parse(json.getAsString());
        }
    }

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Duration.class, new DurationAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @BeforeEach
    void setUp() throws IOException {
        manager = Managers.getDefault();
        server = new HttpTaskServer(manager);
        client = HttpClient.newHttpClient();
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void testEmptyHistory() throws IOException, InterruptedException {
        // Отправляем GET запрос к истории
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Проверяем, что история пуста
        List<Task> history = gson.fromJson(response.body(), new TypeToken<List<Task>>() {
        }.getType());
        assertTrue(history.isEmpty());
    }

    @Test
    void testHistoryWithTasks() throws IOException, InterruptedException {
        // Создаем и добавляем задачи
        Task task = new Task("Задача", "Описание задачи");
        task.setStatus(TaskStatus.NEW);
        Task addedTask = manager.addTask(task);

        Epic epic = new Epic("Эпик", "Описание эпика");
        Epic addedEpic = manager.addEpic(epic);

        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", addedEpic.getId());
        subtask.setStatus(TaskStatus.NEW);
        Subtask addedSubtask = manager.addSubtask(subtask);

        // Получаем задачи, чтобы они попали в историю
        manager.getTaskById(addedTask.getId());
        manager.getEpicById(addedEpic.getId());
        manager.getSubtaskById(addedSubtask.getId());

        // Отправляем GET запрос к истории
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Проверяем содержимое истории
        List<Task> history = gson.fromJson(response.body(), new TypeToken<List<Task>>() {
        }.getType());
        assertEquals(3, history.size());

        // Проверяем порядок задач в истории (последняя просмотренная должна быть последней)
        assertEquals(addedSubtask.getId(), history.get(2).getId());
        assertEquals(addedEpic.getId(), history.get(1).getId());
        assertEquals(addedTask.getId(), history.get(0).getId());
    }

    @Test
    void testHistoryWithDuplicates() throws IOException, InterruptedException {
        // Создаем и добавляем задачу
        Task task = new Task("Задача", "Описание задачи");
        task.setStatus(TaskStatus.NEW);
        Task addedTask = manager.addTask(task);

        // Получаем задачу несколько раз
        manager.getTaskById(addedTask.getId());
        manager.getTaskById(addedTask.getId());
        manager.getTaskById(addedTask.getId());

        // Отправляем GET запрос к истории
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Проверяем, что в истории задача присутствует только один раз
        List<Task> history = gson.fromJson(response.body(), new TypeToken<List<Task>>() {
        }.getType());
        assertEquals(1, history.size());
        assertEquals(addedTask.getId(), history.get(0).getId());
    }

    @Test
    void testInvalidHistoryEndpoint() throws IOException, InterruptedException {
        // Отправляем GET запрос к неверному эндпоинту
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/history")) // Неправильный путь
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем, что получаем код 404
        assertEquals(404, response.statusCode());
    }
}