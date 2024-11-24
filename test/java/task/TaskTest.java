package task;

import task.Task;
import task.TaskStatus;
import manager.Managers;
import manager.TaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = Managers.getDefault();
    }

    @Test
    void testTaskWithDurationAndStartTime() {
        Task task = new Task("Тестовое задание", "Описание теста");
        task.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 0));
        task.setDuration(Duration.ofHours(1));
        taskManager.addTask(task);

        assertEquals(Duration.ofHours(1), task.getDuration());
        assertEquals(LocalDateTime.of(2023, 10, 10, 10, 0), task.getStartTime());
        assertEquals(LocalDateTime.of(2023, 10, 10, 11, 0), task.getEndTime());
    }

    @Test
    void testEpicDurationAndStartTime() {
        Epic epic = taskManager.addEpic(new Epic("Тестовый Эпик", "Описание Эпика"));
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());
        subtask1.setDuration(Duration.ofMinutes(30));
        subtask1.setStartTime(LocalDateTime.of(2023, 10, 10, 10, 0));

        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", epic.getId());
        subtask2.setDuration(Duration.ofMinutes(45));
        subtask2.setStartTime(LocalDateTime.of(2023, 10, 10, 11, 0));

        taskManager.addSubtask(subtask1);
        taskManager.addSubtask(subtask2);

        assertEquals(Duration.ofMinutes(75), epic.getDuration());
        assertEquals(LocalDateTime.of(2023, 10, 10, 10, 0), epic.getStartTime());
    }
}