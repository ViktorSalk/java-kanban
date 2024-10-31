package manager.history;

import manager.history.task.Epic;
import manager.history.task.Subtask;
import manager.history.task.Task;
import manager.history.task.manager.Managers;
import manager.history.task.manager.TaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryTest {

    private TaskManager taskManager;
    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {
        taskManager = Managers.getDefault();
        historyManager = Managers.getDefaultHistory();
    }

    @Test
    void add() {
        Task task = new Task("Test add", "Test add description");
        historyManager.add(task);
        final List<Task> history = historyManager.getHistory();
        assertNotNull(history, "История не пустая.");
        assertEquals(1, history.size(), "История не пустая.");
    }

    @Test
    void getHistory() {
        Task task1 = new Task("Test getHistory 1", "Test getHistory 1 description");
        Task task2 = new Task("Test getHistory 2", "Test getHistory 2 description");
        Task task3 = new Task("Test getHistory 3", "Test getHistory 3 description");

        taskManager.addTask(task1);
        taskManager.addTask(task2);
        taskManager.addTask(task3);

        taskManager.getTaskById(task1.getId());
        taskManager.getTaskById(task2.getId());
        taskManager.getTaskById(task3.getId());

        List<Task> history = taskManager.getHistory();

        assertEquals(3, history.size(), "Неверное количество элементов в истории");
        assertEquals(task3, history.get(2), "Последний элемент в истории неверный");
    }

    @Test
    void getHistoryWithOverflow() {
        for (int i = 0; i < 12; i++) {
            Task task = new Task("Test getHistory " + i, "Test getHistory " + i + " description");
            taskManager.addTask(task);
            taskManager.getTaskById(task.getId());
        }

        List<Task> history = taskManager.getHistory();

        assertEquals(12, history.size(), "Неверное количество элементов в истории после переполнения");
    }

    // Другие тесты
    @Test
    void testHistoryManager() {
        TaskManager taskManager = Managers.getDefault();

        Task task1 = new Task("Задача 1", "Описание задачи 1");
        taskManager.addTask(task1);
        taskManager.getTaskById(task1.getId());

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        taskManager.addTask(task2);
        taskManager.getTaskById(task2.getId());

        List<Task> history = taskManager.getHistory();

        assertEquals(2, history.size());
        assertEquals(task2, history.get(1));
        assertEquals(task1, history.get(0));
    }

    @Test
    void testHistoryManagerWithDuplicates() {
        TaskManager taskManager = Managers.getDefault();

        Task task1 = new Task("Задача 1", "Описание задачи 1");
        taskManager.addTask(task1);
        taskManager.getTaskById(task1.getId());
        taskManager.getTaskById(task1.getId());

        List<Task> history = taskManager.getHistory();

        assertEquals(1, history.size());
        assertEquals(task1, history.get(0));
    }

    @Test
    void testHistoryManagerWithRemove() {
        TaskManager taskManager = Managers.getDefault();

        Task task1 = new Task("Задача 1", "Описание задачи 1");
        taskManager.addTask(task1);
        taskManager.getTaskById(task1.getId());

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        taskManager.addTask(task2);
        taskManager.getTaskById(task2.getId());

        taskManager.deleteTask(task1.getId());

        List<Task> history = taskManager.getHistory();

        assertEquals(1, history.size());
        assertEquals(task2, history.get(0));
    }

    @Test
    void testHistoryManagerWithRemoveEpic() {
        TaskManager taskManager = Managers.getDefault();

        Epic epic1 = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = taskManager.addSubtask(new Subtask("Подзадача 1", "Описание подзадачи 1", epic1.getId()));
        taskManager.getEpicById(epic1.getId());
        taskManager.getSubtaskById(subtask1.getId());

        taskManager.deleteEpic(epic1.getId());

        List<Task> history = taskManager.getHistory();

        assertEquals(0, history.size());
    }

    @Test
    void testHistoryManagerWithRemoveSubtask() {
        TaskManager taskManager = Managers.getDefault();

        Epic epic1 = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = taskManager.addSubtask(new Subtask("Подзадача 1", "Описание подзадачи 1", epic1.getId()));
        taskManager.getEpicById(epic1.getId());
        taskManager.getSubtaskById(subtask1.getId());

        taskManager.deleteSubtask(subtask1.getId());

        List<Task> history = taskManager.getHistory();

        assertEquals(1, history.size());
        assertEquals(epic1, history.get(0));
    }
}