package history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import task.Epic;
import task.Subtask;
import task.Task;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Nested
public class HistoryManagerTest {
    private HistoryManager historyManager;

    @BeforeEach
    public void setUp() {
        historyManager = new HistoryManagerImpl();
    }

    @Nested
    class AddTaskTests { // Тесты добавления задач в историю
        @Test
        public void testAddEmptyHistory() {
            Task task = TaskUtils.assignTask();
            historyManager.add(task);
            List<Task> history = historyManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task, history.get(0));
        }

        @Test
        public void testAddDuplicate() {
            Task task = TaskUtils.assignTask();
            historyManager.add(task);
            historyManager.add(task); // Повторное добавление той же задачи
            List<Task> history = historyManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task, history.get(0));
        }
    }

    @Nested
    class RemoveTaskTests { // Тесты удаления задач из истории
        @Test
        public void testRemoveBeginning() {
            Task task1 = TaskUtils.assignTask();
            task1.setId(1); // Обеспечьте уникальный идентификатор
            Task task2 = TaskUtils.assignTask();
            task2.setId(2);
            historyManager.add(task1);
            historyManager.add(task2);
            historyManager.remove(task1.getId());
            List<Task> history = historyManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task2, history.get(0));
        }

        @Test
        public void testRemoveMiddle() {
            Task task1 = TaskUtils.assignTask();
            task1.setId(1);
            Task task2 = TaskUtils.assignTask();
            task2.setId(2);
            Task task3 = TaskUtils.assignTask();
            task3.setId(3);
            historyManager.add(task1);
            historyManager.add(task2);
            historyManager.add(task3);
            historyManager.remove(task2.getId());
            List<Task> history = historyManager.getHistory();
            assertEquals(2, history.size());
            assertEquals(task1, history.get(0));
            assertEquals(task3, history.get(1));
        }

        @Test
        public void testRemoveEnd() {
            Task task1 = TaskUtils.assignTask();
            task1.setId(1);
            Task task2 = TaskUtils.assignTask();
            task2.setId(2);
            historyManager.add(task1);
            historyManager.add(task2);
            historyManager.remove(task2.getId());
            List<Task> history = historyManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task1, history.get(0));
        }
    }

    @Nested
    class ExceptionTests { // Тесты исключений
        @Test
        void testException() {
            assertThrows(IOException.class, () -> {
                historyManager.loadFromFile("history.txt");
            }, "Загрузка из несуществующего файла должна вызвать IOException");
        }
    }

    @Nested
    class NoExceptionTests { // Тесты без исключений
        @Test
        void testNoException() {
            Task task = TaskUtils.assignTask();
            historyManager.add(task);
            List<Task> history = historyManager.getHistory();
            assertEquals(1, history.size());
            assertEquals(task, history.get(0));
        }
    }

    class TaskUtils { // Вспомогательные методы
        public static Task assignTask() {
            return new Task("Задача 1", "Описание 1");
        }

        public static Epic assignEpic() {
            return new Epic("Эпик 1", "Описание эпика 1");
        }

        public static Subtask assignSubtask() {
            return new Subtask("Подзадача 1", "Описание подзадачи 1", 1);
        }
    }
}