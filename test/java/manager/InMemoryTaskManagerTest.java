package manager;

import history.HistoryManager;
import history.InMemoryHistoryManager;
import org.junit.jupiter.api.Nested;
import task.Task;
import task.TaskStatus;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {

    @Override
    protected InMemoryTaskManager createTaskManager() {
        return Managers.getInMemoryTaskManager();
    }

    @Nested
    class TaskTests { // тесты для задач
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
            assertEquals(task, tasks.get(0), "Задачи не совпадают.");
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
    }

    @Nested
    class HistoryTests { // тесты для истории
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
    }

    @Nested
    class ManagerTests { // тесты для менеджеров
        @Test
        void testManagersGetDefault() {
            TaskManager taskManager = Managers.getDefault();
            assertNotNull(taskManager);
            assertTrue(taskManager instanceof InMemoryTaskManager);

            HistoryManager historyManager = Managers.getDefaultHistory();
            assertNotNull(historyManager);
            assertInstanceOf(InMemoryHistoryManager.class, historyManager);
        }
    }
}