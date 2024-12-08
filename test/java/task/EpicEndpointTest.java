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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EpicEndpointTest {
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
    void testGetAllEpics() throws IOException, InterruptedException {
        // Создаем и добавляем тестовые эпики
        Epic epic1 = new Epic("Эпик 1", "Описание эпика 1");
        Epic epic2 = new Epic("Эпик 2", "Описание эпика 2");

        manager.addEpic(epic1);
        manager.addEpic(epic2);

        // Отправляем GET запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic"))
                .header("Accept", "application/json")
                .GET()
                .build();

        System.out.println("Отправка запроса...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Получен ответ с кодом: " + response.statusCode());
        System.out.println("Тело ответа: " + response.body());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Преобразуем ответ в список эпиков
        List<Epic> epics = gson.fromJson(response.body(), new TypeToken<List<Epic>>() {
        }.getType());

        // Проверяем результат
        assertEquals(2, epics.size());
    }

    @Test
    void testDeleteEpic() throws IOException, InterruptedException {
        // Создаем и добавляем тестовый эпик
        Epic epic = new Epic("Тестовый эпик", "Описание тестового эпика");
        Epic addedEpic = manager.addEpic(epic);
        int epicId = addedEpic.getId();

        // Добавляем подзадачи к эпику
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", epicId);
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", epicId);
        manager.addSubtask(subtask1);
        manager.addSubtask(subtask2);

        // Проверяем, что эпик добавлен
        assertNotNull(manager.getEpicById(epicId), "Эпик не был добавлен");
        assertEquals(2, manager.getSubtasksByEpicId(epicId).size(), "Подзадачи не были добавлены");

        // Проверяем получение подзадач эпика до удаления
        HttpRequest subtasksRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic/" + epicId + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> subtasksResponse = client.send(subtasksRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, subtasksResponse.statusCode(), "Неверный код ответа при запросе подзадач");
        List<Subtask> subtasks = gson.fromJson(subtasksResponse.body(), new TypeToken<List<Subtask>>() {
        }.getType());
        assertEquals(2, subtasks.size(), "Неверное количество подзадач");

        // Отправляем DELETE запрос
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic/" + epicId))
                .DELETE()
                .build();

        System.out.println("Отправка DELETE запроса...");
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Получен ответ с кодом: " + response.statusCode());
        System.out.println("Тело ответа: " + response.body());

        // Проверяем ответ
        assertEquals(200, response.statusCode(), "Неверный код ответа при удалении");

        // Проверяем, что эпик удален
        assertNull(manager.getEpicById(epicId), "Эпик не был удален");

        // Проверяем, что подзадачи удалены
        List<Subtask> remainingSubtasks = manager.getAllSubtasks();
        assertTrue(remainingSubtasks.isEmpty(), "Подзадачи эпика не были удалены");

        // Проверяем, что попытка получить подзадачи удаленного эпика возвращает 404
        HttpRequest deletedEpicSubtasksRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic/" + epicId + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> deletedEpicSubtasksResponse = client.send(deletedEpicSubtasksRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, deletedEpicSubtasksResponse.statusCode(), "Неверный код ответа при запросе подзадач удаленного эпика");
    }

    @Test
    void testUpdateEpic() throws IOException, InterruptedException {
        // Создаем и добавляем тестовый эпик
        Epic epic = new Epic("Тестовый эпик", "Описание тестового эпика");
        Epic addedEpic = manager.addEpic(epic);

        // Изменяем данные эпика
        addedEpic.setName("Обновленный эпик");
        addedEpic.setDescription("Обновленное описание эпика");

        // Отправляем POST запрос для обновления
        String jsonEpic = gson.toJson(addedEpic);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonEpic))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(201, response.statusCode());

        // Получаем обновленный эпик из менеджера
        Epic updatedEpic = manager.getEpicById(addedEpic.getId());
        assertNotNull(updatedEpic);
        assertEquals("Обновленный эпик", updatedEpic.getName());
        assertEquals("Обновленное описание эпика", updatedEpic.getDescription());
    }

    @Test
    void testGetEpicSubtasks() throws IOException, InterruptedException {
        // Создаем эпик и его подзадачи
        Epic epic = new Epic("Тестовый эпик", "Описание тестового эпика");
        Epic addedEpic = manager.addEpic(epic);

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", addedEpic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", addedEpic.getId());
        manager.addSubtask(subtask1);
        manager.addSubtask(subtask2);

        // Отправляем GET запрос для получения подзадач эпика
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic/" + addedEpic.getId() + "/subtasks"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(200, response.statusCode());

        // Преобразуем ответ в список подзадач
        List<Subtask> subtasks = gson.fromJson(response.body(), new TypeToken<List<Subtask>>() {
        }.getType());

        // Проверяем результат
        assertEquals(2, subtasks.size());
        assertTrue(subtasks.stream().allMatch(s -> s.getEpicId() == addedEpic.getId()));
    }

    @Test
    void testEpicSubtasksOverlap() throws IOException, InterruptedException {
        // Создаем эпик с двумя пересекающимися подзадачами
        Epic epic = new Epic("Test Epic", "Epic Description");

        // Создаем первую подзадачу
        LocalDateTime baseTime = LocalDateTime.now().plusHours(1);
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", 0); // ID эпика будет установлен позже
        subtask1.setStartTime(baseTime);
        subtask1.setDuration(Duration.ofMinutes(30));

        // Создаем вторую подзадачу с пересекающимся временем
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", 0);
        subtask2.setStartTime(baseTime.plusMinutes(15)); // Пересекается с первой подзадачей
        subtask2.setDuration(Duration.ofMinutes(30));

        // Добавляем подзадачи в эпик
        List<Subtask> subtasks = new ArrayList<>();
        subtasks.add(subtask1);
        subtasks.add(subtask2);
        epic.setSubtasks(subtasks);

        // Подготавливаем POST запрос
        String jsonEpic = gson.toJson(epic);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonEpic))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(406, response.statusCode(),
                "Пересечение подзадач в эпике должно возвращать код 406");
        assertTrue(response.body().contains("overlaps"),
                "Ответ должен содержать информацию о пересечении подзадач");
    }

    @Test
    void testEpicSubtaskOverlapWithExistingTask() throws IOException, InterruptedException {
        // Создаем обычную задачу
        Task task = new Task("Task", "Description");
        LocalDateTime baseTime = LocalDateTime.now().plusHours(1);
        task.setStartTime(baseTime);
        task.setDuration(Duration.ofMinutes(30));
        manager.addTask(task);

        // Создаем эпик с подзадачей, которая пересекается с существующей задачей
        Epic epic = new Epic("Test Epic", "Epic Description");
        Subtask subtask = new Subtask("Subtask", "Description", 0);
        subtask.setStartTime(baseTime.plusMinutes(15)); // Пересекается с задачей
        subtask.setDuration(Duration.ofMinutes(30));
        epic.addSubtask(subtask);

        // Подготавливаем POST запрос
        String jsonEpic = gson.toJson(epic);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonEpic))
                .build();

        // Отправляем запрос
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Проверяем код ответа
        assertEquals(406, response.statusCode(),
                "Пересечение подзадачи с существующей задачей должно возвращать код 406");
        assertTrue(response.body().contains("overlaps"),
                "Ответ должен содержать информацию о пересечении задач");
    }

    @Test
    void testGetNonExistentEpic() throws IOException, InterruptedException {
        // Запрашиваем несуществующий эпик
        int nonExistentId = 999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic/" + nonExistentId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Запрос несуществующего эпика должен возвращать код 404");
        assertTrue(response.body().contains("not found"),
                "Ответ должен содержать сообщение о том, что эпик не найден");
    }

    @Test
    void testUpdateNonExistentEpic() throws IOException, InterruptedException {
        // Пытаемся обновить несуществующий эпик
        Epic epic = new Epic("Test Epic", "Description");
        epic.setId(999); // Несуществующий ID

        String jsonEpic = gson.toJson(epic);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/epic"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonEpic))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Обновление несуществующего эпика должно возвращать код 404");
    }
}