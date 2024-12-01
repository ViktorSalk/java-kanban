package manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {

    private File tempFile;
    private FileBackedTaskManager taskManager;

    @Override
    protected FileBackedTaskManager createTaskManager() throws IOException {
        tempFile = File.createTempFile("Временные задачи", ".csv");
        tempFile.deleteOnExit(); // Убеждаемся, что он будет удален после завершения теста
        return new FileBackedTaskManager(tempFile);
    }

    @BeforeEach
    public void setUp() throws IOException {
        super.setUp();
        tempFile = File.createTempFile("Временные задачи", ".csv");
        tempFile.deleteOnExit(); // Убеждаемся, что он будет удален после завершения теста
        taskManager = new FileBackedTaskManager(tempFile);
    }

    @Test
    void testSaveAndLoadEmptyFile() throws IOException {
        // Создаем временный файл для тестирования через File.createTempFile
        tempFile = File.createTempFile("Временные задачи", ".csv");
        tempFile.deleteOnExit(); // Убедимся, что он будет удален после завершения теста
        taskManager = new FileBackedTaskManager(tempFile);

        // Проверяем, что в новом менеджере задач нет задач
        assertTrue(taskManager.getAllTasks().isEmpty(), "Менеджер задач не должен содержать задач.");

        // Загружаем задачи из пустого файла
        taskManager.loadFromFile();

        // Проверяем, что задачи по-прежнему отсутствуют
        assertTrue(taskManager.getAllTasks().isEmpty(), "Менеджер задач не должен содержать задач после загрузки из пустого файла.");
    }

    @Test
    void testSaveMultipleTasks() {
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        Task task2 = new Task("Задача 2", "Описание задачи 2");
        taskManager.addTask(task1);
        taskManager.addTask(task2);

        // Сохраняем задачи в файл
        taskManager.save();

        // Проверяем, что файл не пустой
        assertTrue(tempFile.length() > 0, "Файл должен содержать данные после сохранения.");
    }

    @Test
    void testLoadMultipleTasks() throws IOException {
        // Сначала добавим задачи
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        Task task2 = new Task("Задача 2", "Описание задачи 2");
        taskManager.addTask(task1);
        taskManager.addTask(task2);
        taskManager.save();

        // Создаем новый экземпляр FileBackedTaskManager для загрузки задач
        FileBackedTaskManager newTaskManager = new FileBackedTaskManager(tempFile);

        // Проверяем, что загруженные задачи соответствуют добавленным
        assertEquals(2, newTaskManager.getAllTasks().size(), "Должно быть 2 задачи.");
        assertEquals(task1.getName(), newTaskManager.getTaskById(task1.getId()).getName(), "Задача 1 должна совпадать.");
        assertEquals(task2.getName(), newTaskManager.getTaskById(task2.getId()).getName(), "Задача 2 должна совпадать.");
    }
}