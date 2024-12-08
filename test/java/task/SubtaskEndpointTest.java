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

class SubtaskEndpointTest {
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
    void testGetAllSubtasks() throws IOException, InterruptedException {
        // Создаем эпик для подзадач
        Epic epic = new Epic("Тестовый эпик", "Описание эпика");
        Epic addedEpic = manager.addEpic(epic);

        // Создаем и добавляем тестовые подзадачи
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", addedEpic.getId());
        subtask1.setStatus(TaskStatus.NEW);
        subtask1.setDuration(Duration.ofMinutes(30));
        subtask1.setStartTime(LocalDateTime.now().plusHours(1));

        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", addedEpic.getId());
        subtask2.setStatus(TaskStatus.IN_PROGRESS);
        subtask2.setDuration(Duration.ofMinutes(45));
        subtask2.setStartTime(LocalDateTime.now().plusHours(2));

        manager.addSubtask(subtask1);
        manager.addSubtask(subtask2);

        // Отправляем GET запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/subtask"))
                .header("Accept", "application/json")
                .GET()
                .build();

        System.out.println("Отправка запроса...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Получен ответ с кодом: " + response.statusCode());
        System.out.println("Тело ответа: " + response.body());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Преобразуем ответ в список подзадач
        List<Subtask> subtasks = gson.fromJson(response.body(), new TypeToken<List<Subtask>>() {
        }.getType());

        // Проверяем результат
        assertEquals(2, subtasks.size());
    }

    @Test
    void testDeleteSubtask() throws IOException, InterruptedException {
        // Создаем эпик и подзадачу
        Epic epic = new Epic("Тестовый эпик", "Описание эпика");
        Epic addedEpic = manager.addEpic(epic);

        Subtask subtask = new Subtask("Тестовая подзадача", "Описание подзадачи", addedEpic.getId());
        subtask.setStatus(TaskStatus.NEW);
        subtask.setDuration(Duration.ofMinutes(30));
        subtask.setStartTime(LocalDateTime.now().plusHours(1));

        Subtask addedSubtask = manager.addSubtask(subtask);
        int subtaskId = addedSubtask.getId();

        // Проверяем, что подзадача добавлена
        assertNotNull(manager.getSubtaskById(subtaskId), "Подзадача не была добавлена");

        // Отправляем DELETE запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/subtask/" + subtaskId))
                .DELETE()
                .build();

        System.out.println("Отправка DELETE запроса...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Получен ответ с кодом: " + response.statusCode());
        System.out.println("Тело ответа: " + response.body());

        // Проверяем ответ
        assertEquals(200, response.statusCode(), "Неверный код ответа при удалении");

        // Проверяем, что подзадача удалена
        List<Subtask> remainingSubtasks = manager.getAllSubtasks();
        assertTrue(remainingSubtasks.isEmpty(), "Список подзадач должен быть пустым");

        // Проверяем, что попытка получить удаленную подзадачу возвращает null
        assertNull(manager.getSubtaskById(subtaskId), "Подзадача не была удалена");
    }


    @Test
    void testCreateSubtask() throws IOException, InterruptedException {
        // Сначала создаем эпик
        Epic epic = new Epic("Test Epic", "Epic Description");
        Epic savedEpic = manager.addEpic(epic);

        // Создаем подзадачу для POST запроса
        Subtask subtask = new Subtask("Test Subtask", "Subtask Description", savedEpic.getId());
        subtask.setStatus(TaskStatus.NEW);
        subtask.setStartTime(LocalDateTime.now().plusHours(1));
        subtask.setDuration(Duration.ofMinutes(30));

        // Подготавливаем POST запрос
        String jsonSubtask = gson.toJson(subtask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/subtask"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonSubtask))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(201, response.statusCode(), "Создание подзадачи должно возвращать код 201");

        // Проверяем, что подзадача действительно создана
        Subtask createdSubtask = gson.fromJson(response.body(), Subtask.class);
        assertNotNull(createdSubtask.getId(), "Созданная подзадача должна иметь ID");
    }

    @Test
    void testUpdateSubtask() throws IOException, InterruptedException {
        // Создаем эпик и подзадачу через менеджер
        Epic epic = new Epic("Test Epic", "Epic Description");
        Epic savedEpic = manager.addEpic(epic);

        Subtask subtask = new Subtask("Test Subtask", "Subtask Description", savedEpic.getId());
        subtask.setStatus(TaskStatus.NEW);
        subtask.setStartTime(LocalDateTime.now().plusHours(1));
        subtask.setDuration(Duration.ofMinutes(30));
        Subtask savedSubtask = manager.addSubtask(subtask);

        // Изменяем данные подзадачи
        savedSubtask.setName("Updated Subtask");
        savedSubtask.setStatus(TaskStatus.IN_PROGRESS);

        // Подготавливаем POST запрос для обновления
        String jsonSubtask = gson.toJson(savedSubtask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/subtask"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonSubtask))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(201, response.statusCode(), "Обновление подзадачи должно возвращать код 201");

        // Проверяем, что данные обновились
        Subtask updatedSubtask = gson.fromJson(response.body(), Subtask.class);
        assertEquals("Updated Subtask", updatedSubtask.getName(), "Название подзадачи должно обновиться");
        assertEquals(TaskStatus.IN_PROGRESS, updatedSubtask.getStatus(), "Статус подзадачи должен обновиться");
    }

    @Test
    void testSubtaskOverlap() throws IOException, InterruptedException {
        // Создаем эпик
        Epic epic = new Epic("Test Epic", "Epic Description");
        Epic savedEpic = manager.addEpic(epic);

        // Создаем первую подзадачу
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", savedEpic.getId());
        LocalDateTime baseTime = LocalDateTime.now().plusHours(1);
        subtask1.setStartTime(baseTime);
        subtask1.setDuration(Duration.ofMinutes(30));
        manager.addSubtask(subtask1);

        // Создаем вторую подзадачу с пересекающимся временем
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", savedEpic.getId());
        subtask2.setStartTime(baseTime.plusMinutes(15)); // Пересекается с первой подзадачей
        subtask2.setDuration(Duration.ofMinutes(30));

        // Подготавливаем POST запрос
        String jsonSubtask = gson.toJson(subtask2);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/subtask"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonSubtask))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(406, response.statusCode(), "Пересечение подзадач должно возвращать код 406");
        assertTrue(response.body().contains("overlaps"),
                "Ответ должен содержать информацию о пересечении подзадач");
    }

    @Test
    void testGetNonExistentSubtask() throws IOException, InterruptedException {
        // Запрашиваем несуществующую подзадачу
        int nonExistentId = 999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/subtask/" + nonExistentId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Запрос несуществующей подзадачи должен возвращать код 404");
        assertTrue(response.body().contains("not found"),
                "Ответ должен содержать сообщение о том, что подзадача не найдена");
    }

    @Test
    void testUpdateNonExistentSubtask() throws IOException, InterruptedException {
        // Создаем эпик для подзадачи
        Epic epic = new Epic("Test Epic", "Description");
        Epic savedEpic = manager.addEpic(epic);

        // Пытаемся обновить несуществующую подзадачу
        Subtask subtask = new Subtask("Test Subtask", "Description", savedEpic.getId());
        subtask.setId(999); // Несуществующий ID

        String jsonSubtask = gson.toJson(subtask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/subtask"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonSubtask))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Обновление несуществующей подзадачи должно возвращать код 404");
    }
}