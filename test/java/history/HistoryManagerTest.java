package history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryManagerTest {
    private HistoryManager historyManager;

    @BeforeEach
    public void setUp() {
        historyManager = new HistoryManagerImpl();
    }

    @Test
    public void testAddEmptyHistory() {
        Task task = new Task("Задача 1", "Описание 1");
        historyManager.add(task);
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task, history.get(0));
    }

    @Test
    public void testAddDuplicate() {
        Task task = new Task("Задача 1", "Описание 1");
        historyManager.add(task);
        historyManager.add(task); // Повторное добавление той же задачи
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task, history.get(0));
    }

    @Test
    public void testRemoveBeginning() {
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1); // Обеспечьте уникальный идентификатор
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(2); //Обеспечьте уникальный идентификатор
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.remove(task1.getId());
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task2, history.get(0));
    }

    @Test
    public void testRemoveMiddle() {
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1); // Обеспечьте уникальный идентификатор
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(2); // Обеспечьте уникальный идентификатор
        Task task3 = new Task("Задача 3", "Описание 3");
        task3.setId(3); // Обеспечьте уникальный идентификатор
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
        Task task1 = new Task("Задача 1", "Описание 1");
        task1.setId(1); // Обеспечьте уникальный идентификатор
        Task task2 = new Task("Задача 2", "Описание 2");
        task2.setId(2); // Обеспечьте уникальный идентификатор
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.remove(task2.getId());
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task1, history.get(0));
    }

    @Test
    public void testException() {
        assertThrows(IOException.class, () -> {
            historyManager.loadFromFile("non-existent-file.txt");
        }, "Загрузка из несуществующего файла должна вызвать IOException");
    }

    @Test
    public void testNoException() {
        assertThrows(IOException.class, () -> {
            historyManager.loadFromFile("history.txt");
        }, "Загрузка из несуществующего файла должна вызвать IOException");
    }
}