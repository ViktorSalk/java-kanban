package manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import task.Task;
import task.TaskStatus;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TimeSlotManagerTest {
    private InMemoryTaskManager taskManager;

    @BeforeEach
    public void setUp() {
        taskManager = new InMemoryTaskManager();
    }

    @Test
    public void testAddNonOverlappingTask() {
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        task1.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 0));
        task1.setDuration(Duration.ofMinutes(30));

        Task addedTask = taskManager.addTask(task1);

        assertEquals(task1.getId(), addedTask.getId(), "Задача должна быть успешно добавлена.");
    }

    @Test
    public void testAddOverlappingTask() {
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        task1.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 0));
        task1.setDuration(Duration.ofMinutes(30));
        taskManager.addTask(task1); // Add the first task

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        task2.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 15)); // Overlaps with task1
        task2.setDuration(Duration.ofMinutes(30));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            taskManager.addTask(task2); // Attempt to add overlapping task
        });

        assertEquals("Task overlaps with existing task.", exception.getMessage());
    }

    @Test
    public void testAddMultipleTasks() {
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        task1.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 0));
        task1.setDuration(Duration.ofMinutes(30));
        taskManager.addTask(task1);

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        task2.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 30)); // No overlap
        task2.setDuration(Duration.ofMinutes(30));
        taskManager.addTask(task2);

        Task task3 = new Task("Задача 3", "Описание задачи 3");
        task3.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 15)); // Overlaps with task1

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            taskManager.addTask(task3); // Попытка добавить перекрывающуюся задачу
        });

        assertEquals("Task overlaps with existing task.", exception.getMessage());
    }

    @Test
    public void testDeleteTaskFreesTimeSlots() {
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        task1.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 0));
        task1.setDuration(Duration.ofMinutes(30));
        taskManager.addTask(task1); // Добавьте первую задачу

        Task task2 = new Task("Задача 2", "Описание задачи 2");
        task2.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 15)); // Совпадает с задачей 1

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            taskManager.addTask(task2); // Попытка добавить перекрывающуюся задачу
        });

        assertEquals("Task overlaps with existing task.", exception.getMessage());

        // Теперь удалите задачу 1 и проверьте, можно ли добавить задачу 2
        taskManager.deleteTask(task1.getId());

        // Попробуйте добавить task2 еще раз
        Task addedTask2 = taskManager.addTask(task2);
        assertEquals(task2.getId(), addedTask2.getId(), "Задача должна быть успешно добавлена после удаления перекрывающейся задачи.");
    }
}