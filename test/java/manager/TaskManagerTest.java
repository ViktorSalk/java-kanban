package manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import task.Epic;
import task.Subtask;
import task.Task;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class TaskManagerTest<T extends TaskManager> {
    protected T taskManager;

    protected abstract T createTaskManager() throws IOException;

    @BeforeEach
    public void setUp() throws IOException {
        taskManager = createTaskManager();
        taskManager = createTaskManager();
        taskManager.clearTasks();
        taskManager.clearEpics();
        taskManager.clearSubtasks();
        assertNotNull(taskManager, "Task manager cannot be null");
    }

    @Nested
    class TaskTests { // тесты для задач

        @Test
        public void testAddTask() {
            Task task = new Task("Задача 1", "Описание задачи 1");
            taskManager.addTask(task);
            assertEquals(1, taskManager.getAllTasks().size());
        }

        @Test
        public void testGetTaskById() {
            Task task = new Task("Задача 1", "Описание задачи 1");
            taskManager.addTask(task);
            assertEquals(task, taskManager.getTaskById(task.getId()));
        }

        @Test
        public void testUpdateTask() {
            Task task = new Task("Задача 1", "Описание задачи 1");
            taskManager.addTask(task);
            task.setName("Задача 2");
            taskManager.updateTask(task);
            assertEquals("Задача 2", taskManager.getTaskById(task.getId()).getName());
        }

        @Test
        public void testDeleteTask() {
            Task task = new Task("Задача 1", "Описание подзадачи 1");
            taskManager.addTask(task);
            taskManager.deleteTask(task.getId());
            assertEquals(0, taskManager.getAllTasks().size());
        }
    }

    @Nested
    class EpicTests { // тесты для эпик

        @Test
        public void testAddEpic() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            assertEquals(1, taskManager.getAllEpics().size());
        }

        @Test
        public void testGetEpicById() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            assertEquals(epic, taskManager.getEpicById(epic.getId()));
        }

        @Test
        public void testUpdateEpic() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            epic.setName("Эпик 2");
            taskManager.updateEpic(epic);
            assertEquals("Эпик 2", taskManager.getEpicById(epic.getId()).getName());
        }

        @Test
        public void testDeleteEpic() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            taskManager.deleteEpic(epic.getId());
            assertEquals(0, taskManager.getAllEpics().size());
        }
    }

    @Nested
    class SubtaskTests { // тесты для подзадач

        @Test
        public void testAddSubtask() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            Subtask subtask = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());
            taskManager.addSubtask(subtask);
            assertEquals(1, taskManager.getAllSubtasks().size());
        }

        @Test
        public void testGetSubtaskById() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            Subtask subtask = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());
            taskManager.addSubtask(subtask);
            assertEquals(subtask, taskManager.getSubtaskById(subtask.getId()));
        }

        @Test
        public void testUpdateSubtask() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            Subtask subtask = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());
            taskManager.addSubtask(subtask);
            subtask.setName("Подзадача 2");
            taskManager.updateSubtask(subtask);
            assertEquals("Подзадача 2", taskManager.getSubtaskById(subtask.getId()).getName());
        }

        @Test
        public void testDeleteSubtask() {
            Epic epic = new Epic("Эпик 1", "Описание эпика 1");
            taskManager.addEpic(epic);
            Subtask subtask = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());
            taskManager.addSubtask(subtask);
            taskManager.deleteSubtask(subtask.getId());
            assertEquals(0, taskManager.getAllSubtasks().size());
        }
    }
}