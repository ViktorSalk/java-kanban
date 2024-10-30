package manager.history.task.manager;

import manager.history.HistoryManager;
import manager.history.InMemoryHistoryManager;
import manager.history.task.Epic;
import manager.history.task.Subtask;
import manager.history.task.Task;
import manager.history.task.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {

    private final TaskManager taskManager = Managers.getDefault();
    private final HistoryManager historyManager = Managers.getDefaultHistory();

    @Test
    void addNewTask() {
        Task task = new Task("Test addNewTask", "Test addNewTask description");
        final int taskId = taskManager.addTask(task).getId();

        final Task savedTask = taskManager.getTaskById(taskId);

        assertNotNull(savedTask, "Задача не найдена.");
        assertEquals(task, savedTask, "Задачи не совпадают.");

        final List<Task> tasks = taskManager.getAllTasks();

        assertNotNull(tasks, "Задачи не возвращаются.");
        assertEquals(1, tasks.size(), "Неверное количество задач.");
        assertEquals(task, tasks.getFirst(), "Задачи не совпадают.");
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

    @Test
    void testTaskEquality() {
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        Task task2 = new Task("Задача 2", "Описание задачи 2");
        Task task3 = new Task("Задача 1", "Описание задачи 1");

        TaskManager taskManager = Managers.getDefault(); // Фабричный метод вместо создания экземпляров через nev
        taskManager.addTask(task1);
        taskManager.addTask(task2);

        assertEquals(taskManager.getTaskById(task1.getId()), task1);
        assertNotEquals(taskManager.getTaskById(task1.getId()), task2);
        assertNotEquals(taskManager.getTaskById(task1.getId()), task3);
    }

    @Test
    void testSubtaskEquality() {
        Epic epic = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", epic.getId());
        Subtask subtask3 = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());

        TaskManager taskManager = Managers.getDefault(); // Managers для создания нового менеджера задач еще в 8 тестах
        taskManager.addEpic(epic);
        taskManager.addSubtask(subtask1);
        taskManager.addSubtask(subtask2);

        assertEquals(taskManager.getSubtaskById(subtask1.getId()), subtask1);
        assertNotEquals(taskManager.getSubtaskById(subtask1.getId()), subtask2);
        assertNotEquals(taskManager.getSubtaskById(subtask1.getId()), subtask3);
    }

    @Test
    void testEpicEquality() {
        Epic epic1 = new Epic("Эпик 1", "Описание эпика 1");
        Epic epic2 = new Epic("Эпик 2", "Описание эпика 2");
        Epic epic3 = new Epic("Эпик 1", "Описание эпика 1");

        TaskManager taskManager = Managers.getDefault();
        taskManager.addEpic(epic1);
        taskManager.addEpic(epic2);

        assertEquals(taskManager.getEpicById(epic1.getId()), epic1);
        assertNotEquals(taskManager.getEpicById(epic1.getId()), epic2);
        assertNotEquals(taskManager.getEpicById(epic1.getId()), epic3);
    }

    @Test
    void testSubtaskWithItselfAsEpic() {
        Subtask subtask = new Subtask("Подзадача 1", "Описание подзадачи 1", 1);

        TaskManager taskManager = Managers.getDefault();

        assertThrows(IllegalArgumentException.class, () -> taskManager.addSubtask(subtask));
    }

    @Test
    void testManagersGetDefault() {
        TaskManager taskManager = Managers.getDefault();
        assertNotNull(taskManager);
        assertTrue(taskManager instanceof InMemoryTaskManager);

        HistoryManager historyManager = Managers.getDefaultHistory();
        assertNotNull(historyManager);
        assertInstanceOf(InMemoryHistoryManager.class, historyManager);
    }

    @Test
    void testAddingTasks() {
        TaskManager taskManager = Managers.getDefault();

        Task task1 = new Task("Задача 1", "Описание задачи 1");
        Epic epic1 = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", epic1.getId());

        taskManager.addTask(task1);
        taskManager.addSubtask(subtask1);

        assertEquals(taskManager.getTaskById(task1.getId()), task1);
        assertEquals(taskManager.getEpicById(epic1.getId()), epic1);
        assertEquals(taskManager.getSubtaskById(subtask1.getId()), subtask1);
    }

    @Test
    void testTaskWithGeneratedId() {
        TaskManager taskManager = Managers.getDefault();

        Task task1 = new Task("Задача 1", "Описание задачи 1");
        taskManager.addTask(task1);

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        taskManager.addTask(task2);

        assertEquals(taskManager.getTaskById(task1.getId()), task1);
        assertEquals(taskManager.getTaskById(task2.getId()), task2);
    }

    @Test
    void testTaskImmutability() {
        TaskManager taskManager = Managers.getDefault();

        Task task1 = new Task("Задача 1", "Описание задачи 1");
        task1.setStatus(TaskStatus.DONE);
        taskManager.addTask(task1);

        Task taskFromManager = taskManager.getTaskById(task1.getId());

        assertEquals(task1.getName(), taskFromManager.getName());
        assertEquals(task1.getDescription(), taskFromManager.getDescription());
        assertEquals(task1.getStatus(), taskFromManager.getStatus());
    }

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