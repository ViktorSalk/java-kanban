package manager;

import task.Epic;
import task.Subtask;
import task.Task;

import java.util.*;

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

    public default List<Subtask> getSubtasksByEpicId(int epicId) {
        Epic epic = getEpicById(epicId);
        if (epic == null) {
            return Collections.emptyList(); // Возвращаем пустой список, если эпик не найден
        }
        return new ArrayList<>(epic.getSubtasks());
    }

    List<Task> getHistory();

    void clearTasks();

    void clearEpics();

    void clearSubtasks();


    List<Task> getPrioritizedTasks();

    Optional<Task> getTaskByIdOptional(int id);

    Optional<Epic> getEpicByIdOptional(int id);

    Optional<Subtask> getSubtaskByIdOptional(int id);

    void validateTaskTime(Task task) throws IllegalArgumentException;
}