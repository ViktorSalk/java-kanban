package task;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import manager.Managers;
import manager.TaskManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import manager.HttpTaskServer;

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

class TaskEndpointTest {
    private static final String BASE_URL = "http://localhost:8080";
    private HttpTaskServer server;
    private TaskManager manager;
    private HttpClient client;

    // Адаптер для сериализации/десериализации Duration
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

    // Адаптер для сериализации/десериализации LocalDateTime
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
    void testGetAllTasks() throws IOException, InterruptedException {
        // Создаем и добавляем тестовые задачи через менеджер
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        task1.setStatus(TaskStatus.NEW);
        task1.setDuration(Duration.ofMinutes(30));
        task1.setStartTime(LocalDateTime.now().plusHours(1));

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        task2.setStatus(TaskStatus.IN_PROGRESS);
        task2.setDuration(Duration.ofMinutes(45));
        task2.setStartTime(LocalDateTime.now().plusHours(2));

        manager.addTask(task1);
        manager.addTask(task2);

        // Отправляем GET запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/task"))
                .header("Accept", "application/json")
                .GET()
                .build();

        System.out.println("Отправка запроса...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Получен ответ с кодом: " + response.statusCode()); // Логирование
        System.out.println("Тело ответа: " + response.body()); // Логирование

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Преобразуем ответ в список задач
        List<Task> tasks = gson.fromJson(response.body(), new TypeToken<List<Task>>() {
        }.getType());

        // Проверяем результат
        assertEquals(2, tasks.size());
    }

    @Test
    void testDeleteTask() throws IOException, InterruptedException {
        // Создаем и добавляем тестовую задачу
        Task task = new Task("Тестовая задача", "Описание тестовой задачи");
        task.setStatus(TaskStatus.NEW);
        task.setDuration(Duration.ofMinutes(30));
        task.setStartTime(LocalDateTime.now().plusHours(1));

        Task addedTask = manager.addTask(task);
        int taskId = addedTask.getId();

        // Проверяем, что задача добавлена
        assertNotNull(manager.getTaskById(taskId), "Задача не была добавлена");

        // Отправляем DELETE запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/task/" + taskId))
                .DELETE()
                .build();

        System.out.println("Отправка DELETE запроса...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Получен ответ с кодом: " + response.statusCode());
        System.out.println("Тело ответа: " + response.body());

        // Проверяем ответ
        assertEquals(200, response.statusCode(), "Неверный код ответа при удалении");

        // Проверяем, что задача удалена
        List<Task> remainingTasks = manager.getAllTasks();
        assertTrue(remainingTasks.isEmpty(), "Список задач должен быть пустым");

        // Проверяем, что попытка получить удаленную задачу возвращает null
        assertNull(manager.getTaskById(taskId), "Задача не была удалена");
    }

    @Test
    void testCreateTask() throws IOException, InterruptedException {
        // Создаем задачу для POST запроса
        Task task = new Task("Test Task", "Task Description");
        task.setStatus(TaskStatus.NEW);
        task.setStartTime(LocalDateTime.now().plusHours(1));
        task.setDuration(Duration.ofMinutes(30));

        // Подготавливаем POST запрос
        String jsonTask = gson.toJson(task);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/task"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(201, response.statusCode(), "Создание задачи должно возвращать код 201");

        // Проверяем, что задача действительно создана
        Task createdTask = gson.fromJson(response.body(), Task.class);
        assertNotNull(createdTask.getId(), "Созданная задача должна иметь ID");
    }

    @Test
    void testUpdateTask() throws IOException, InterruptedException {
        // Создаем задачу через менеджер
        Task task = new Task("Test Task", "Task Description");
        task.setStatus(TaskStatus.NEW);
        task.setStartTime(LocalDateTime.now().plusHours(1));
        task.setDuration(Duration.ofMinutes(30));
        Task savedTask = manager.addTask(task);

        // Изменяем данные задачи
        savedTask.setName("Updated Task");
        savedTask.setStatus(TaskStatus.IN_PROGRESS);

        // Подготавливаем POST запрос для обновления
        String jsonTask = gson.toJson(savedTask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/task"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(201, response.statusCode(), "Обновление задачи должно возвращать код 201");

        // Проверяем, что данные обновились
        Task updatedTask = gson.fromJson(response.body(), Task.class);
        assertEquals("Updated Task", updatedTask.getName(), "Название задачи должно обновиться");
        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus(), "Статус задачи должен обновиться");
    }

    @Test
    void testTaskOverlap() throws IOException, InterruptedException {
        // Создаем первую задачу
        Task task1 = new Task("Task 1", "Description 1");
        LocalDateTime baseTime = LocalDateTime.now().plusHours(1);
        task1.setStartTime(baseTime);
        task1.setDuration(Duration.ofMinutes(30));
        manager.addTask(task1);

        // Создаем вторую задачу с пересекающимся временем
        Task task2 = new Task("Task 2", "Description 2");
        task2.setStartTime(baseTime.plusMinutes(15)); // Пересекается с первой задачей
        task2.setDuration(Duration.ofMinutes(30));

        // Подготавливаем POST запрос
        String jsonTask = gson.toJson(task2);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/task"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(406, response.statusCode(), "Пересечение задач должно возвращать код 406");
        assertTrue(response.body().contains("overlaps"),
                "Ответ должен содержать информацию о пересечении задач");
    }

    @Test
    void testGetNonExistentTask() throws IOException, InterruptedException {
        // Запрашиваем несуществующую задачу
        int nonExistentId = 999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/task/" + nonExistentId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Запрос несуществующей задачи должен возвращать код 404");
        assertTrue(response.body().contains("not found"),
                "Ответ должен содержать сообщение о том, что задача не найдена");
    }

    @Test
    void testUpdateNonExistentTaskReturns404() throws IOException, InterruptedException {
        // Пытаемся обновить несуществующую задачу
        Task task = new Task("Test Task", "Description");
        task.setId(999); // Устанавливаем несуществующий ID
        task.setStatus(TaskStatus.NEW);
        task.setStartTime(LocalDateTime.now().plusHours(1));
        task.setDuration(Duration.ofMinutes(30));

        String jsonTask = gson.toJson(task);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/task"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonTask))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем ответ
        assertEquals(404, response.statusCode(), "Обновление несуществующей задачи должно возвращать код 404");
        assertTrue(response.body().contains("not found"),
                "Ответ должен содержать сообщение о том, что задача не найдена");
    }
}