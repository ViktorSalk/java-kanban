package manager.history.task;

import manager.history.task.manager.InMemoryTaskManager;
import manager.history.task.manager.TaskManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EpicTest {
    private final TaskManager taskManager = new InMemoryTaskManager();

    @Test
    void addSubtask() {
        Epic epic = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask = new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId());
        taskManager.addSubtask(subtask);
        assertEquals(1, epic.getSubtasks().size());
        assertEquals(subtask, epic.getSubtasks().get(0));
    }

    @Test
    void removeSubtask() {
        Epic epic = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = taskManager.addSubtask(new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId()));
        Subtask subtask2 = taskManager.addSubtask(new Subtask("Подзадача 2", "Описание подзадачи 2", epic.getId()));
        epic.removeSubtask(subtask1.getId());
        assertEquals(1, epic.getSubtasks().size());
        assertEquals(subtask2, epic.getSubtasks().get(0));
    }

    @Test
    void getSubtasks() {
        Epic epic = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = taskManager.addSubtask(new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId()));
        Subtask subtask2 = taskManager.addSubtask(new Subtask("Подзадача 2", "Описание подзадачи 2", epic.getId()));
        List<Subtask> subtasks = epic.getSubtasks();
        assertEquals(2, subtasks.size());
        assertTrue(subtasks.contains(subtask1));
        assertTrue(subtasks.contains(subtask2));
    }

    @Test
    void updateSubtask() {
        Epic epic = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = taskManager.addSubtask(new Subtask("Подзадача 1", "Описание подзадачи 1", epic.getId()));
        Subtask subtask2 = taskManager.addSubtask(new Subtask("Подзадача 2", "Описание подзадачи 2", epic.getId()));
        subtask1.setStatus(TaskStatus.DONE);
        epic.updateSubtask(subtask1);
        assertEquals(TaskStatus.DONE, epic.getSubtasks().get(0).getStatus());
    }
}