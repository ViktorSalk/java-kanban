package manager.history.task.manager;

import manager.history.task.Epic;
import manager.history.task.Subtask;
import manager.history.task.Task;

import java.util.List;

public interface TaskManager {
    Task addTask(Task task);

    Task getTaskById(int id);

    void updateTask(Task task);

    void deleteTask(int id);

    List<Task> getAllTasks();

    Epic addEpic(Epic epic);

    Epic getEpicById(int id);

    void updateEpic(Epic epic);

    void deleteEpic(int id);

    List<Epic> getAllEpics();

    Subtask addSubtask(Subtask subtask);

    Subtask getSubtaskById(int id);

    void updateSubtask(Subtask subtask);

    void deleteSubtask(int id);

    List<Subtask> getAllSubtasks();

    List<Subtask> getSubtasksByEpicId(int epicId);

    List<Task> getHistory();

    void clearTasks();

    void clearEpics();

    void clearSubtasks();
}