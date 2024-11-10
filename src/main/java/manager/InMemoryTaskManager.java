package manager;

import history.HistoryManager;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryTaskManager implements TaskManager {
    private int nextTaskId = 1;
    protected final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();

    public Task addTask(Task task) {
        task.setId(nextTaskId++);
        tasks.put(task.getId(), task);
        return task;
    }

    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public void deleteTask(int id) {
        tasks.remove(id);
        historyManager.remove(id); // Теперь используется в deleteTask
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    // Методы для эпиков

    public Epic addEpic(Epic epic) {
        epic.setId(nextTaskId++);
        epics.put(epic.getId(), epic);
        return epic;
    }

    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    public void updateEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    public void deleteEpic(int id) {
        // Удаление подзадач, связанных с эпиком
        Epic removedEpic = epics.remove(id);
        if (removedEpic != null) {
            for (Subtask subtask : removedEpic.getSubtasks()) {
                deleteSubtask(subtask.getId());
            }
        }
        historyManager.remove(id); // Теперь используется в deleteEpic
    }

    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    // Методы для подзадач

    public Subtask addSubtask(Subtask subtask) {
        subtask.setId(nextTaskId++);
        subtasks.put(subtask.getId(), subtask);

        // Проверка на наличие эпика перед добавлением
        if (!epics.containsKey(subtask.getEpicId())) {
            throw new IllegalArgumentException("Эпик с ID " + subtask.getEpicId()
                    + " отсутствует.");
        }

        epics.get(subtask.getEpicId()).addSubtask(subtask);
        updateEpicStatus(subtask.getEpicId());
        return subtask;
    }

    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    public void updateSubtask(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
        updateEpicStatus(subtask.getEpicId());
    }

    public void deleteSubtask(int id) {
        subtasks.remove(id);
        historyManager.remove(id); // Теперь используется в deleteSubtask
    }

    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    // Метод для получения списка подзадач для заданного эпика
    public List<Subtask> getSubtasksByEpicId(int epicId) {
        return epics.get(epicId).getSubtasks();
    }

    // Методы для обновления статуса эпика
    private void updateEpicStatus(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic.getSubtasks().stream().allMatch(subtask -> subtask.getStatus() == TaskStatus.NEW)) {
            epic.setStatus(TaskStatus.NEW);
        } else if (epic.getSubtasks().stream().allMatch(subtask -> subtask.getStatus() == TaskStatus.DONE)) {
            epic.setStatus(TaskStatus.DONE);
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    // Методы для удаления всех задач по типам
    public void clearTasks() {
        tasks.clear(); // Теперь включает historyManager
        for (Integer id : new ArrayList<>(tasks.keySet())) {
            historyManager.remove(id); // Очищает историю
        }
    }

    public void clearEpics() {
        epics.clear(); // Теперь включает в себя historyManager
        for (Integer id : new ArrayList<>(epics.keySet())) {
            historyManager.remove(id); // Очищает историю
        }
        subtasks.clear();
    }

    public void clearSubtasks() {
        for (Subtask subtask : subtasks.values()) {
            epics.get(subtask.getEpicId()).removeSubtask(subtask.getId());
            historyManager.remove(subtask.getId()); // Теперь включает в себя historyManager
        }
        subtasks.clear();
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }
}