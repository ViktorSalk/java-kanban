package task;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import manager.HttpTaskServer;
import manager.Managers;
import manager.TaskManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class PrioritizedEndpointTest {
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
    void testEmptyPrioritizedTasks() throws IOException, InterruptedException {
        // Отправляем GET запрос к списку приоритетных задач
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Проверяем, что список пуст
        List<Task> tasks = gson.fromJson(response.body(), new TypeToken<List<Task>>() {
        }.getType());
        assertTrue(tasks.isEmpty());
    }

    @Test
    void testPrioritizedTasksOrder() throws IOException, InterruptedException {
        // Создаем задачи с разным временем начала
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        task1.setStartTime(LocalDateTime.now().plusHours(2));
        task1.setDuration(Duration.ofMinutes(30));
        manager.addTask(task1);

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        task2.setStartTime(LocalDateTime.now().plusHours(1));
        task2.setDuration(Duration.ofMinutes(45));
        manager.addTask(task2);

        Task task3 = new Task("Задача 3", "Описание задачи 3");
        task3.setStartTime(LocalDateTime.now().plusHours(3));
        task3.setDuration(Duration.ofMinutes(60));
        manager.addTask(task3);

        // Проверяем, что задачи добавлены
        assertEquals(3, manager.getAllTasks().size(), "Задачи не были добавлены");

        // Отправляем GET запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Проверяем порядок задач
        List<Task> prioritizedTasks = gson.fromJson(response.body(), new TypeToken<List<Task>>() {
        }.getType());
        assertEquals(3, prioritizedTasks.size(), "Неверное количество задач в ответе");

        // Проверяем, что задачи отсортированы по времени начала
        for (int i = 0; i < prioritizedTasks.size() - 1; i++) {
            LocalDateTime current = prioritizedTasks.get(i).getStartTime();
            LocalDateTime next = prioritizedTasks.get(i + 1).getStartTime();
            assertTrue(current.isBefore(next),
                    String.format("Неверный порядок задач: %s должно быть раньше %s", current, next));
        }
    }

    @Test
    void testPrioritizedTasksWithDifferentTypes() throws IOException, InterruptedException {
        // Создаем задачи разных типов
        Task task = new Task("Обычная задача", "Описание задачи");
        task.setStartTime(LocalDateTime.now().plusHours(1));
        task.setDuration(Duration.ofMinutes(30));
        Task addedTask = manager.addTask(task);
        assertNotNull(addedTask, "Задача не была добавлена");

        Epic epic = new Epic("Эпик", "Описание эпика");
        Epic addedEpic = manager.addEpic(epic);
        assertNotNull(addedEpic, "Эпик не был добавлен");

        Subtask subtask = new Subtask("Подзадача", "Описание подзадачи", addedEpic.getId());
        subtask.setStartTime(LocalDateTime.now().plusHours(2));
        subtask.setDuration(Duration.ofMinutes(45));
        Subtask addedSubtask = manager.addSubtask(subtask);
        assertNotNull(addedSubtask, "Подзадача не была добавлена");

        // Проверяем, что задачи добавлены
        assertEquals(1, manager.getAllTasks().size(), "Обычная задача не добавлена");
        assertEquals(1, manager.getAllEpics().size(), "Эпик не добавлен");
        assertEquals(1, manager.getAllSubtasks().size(), "Подзадача не добавлена");

        // Отправляем GET запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/prioritized"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Проверяем содержимое ответа
        List<Task> prioritizedTasks = gson.fromJson(response.body(), new TypeToken<List<Task>>() {
        }.getType());

        // Должны быть только задачи с установленным временем (обычная задача и подзадача)
        assertEquals(2, prioritizedTasks.size(), "Неверное количество задач в списке");

        // Проверяем порядок (по времени начала)
        LocalDateTime firstTime = prioritizedTasks.get(0).getStartTime();
        LocalDateTime secondTime = prioritizedTasks.get(1).getStartTime();
        assertTrue(firstTime.isBefore(secondTime),
                String.format("Неверный порядок задач: %s должно быть раньше %s", firstTime, secondTime));
    }
}