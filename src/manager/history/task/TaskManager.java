package manager.history.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class TaskManager {
    private int nextTaskId = 1;
    private HashMap<Integer, Task> tasks = new HashMap<>();
    private HashMap<Integer, Epic> epics = new HashMap<>();
    private HashMap<Integer, Subtask> subtasks = new HashMap<>();

    public Task addTask(Task task) {
        task.setId(nextTaskId++);
        tasks.put(task.getId(), task);
        return task;
    }

    public Task getTaskById(int id) {
        return tasks.get(id);
    }

    public void updateTask(Task task) {
        tasks.put(task.getId(), task);
    }

    public void deleteTask(int id) {
        tasks.remove(id);
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
        return epics.get(id);
    }

    public void updateEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    public void deleteEpic(int id) {
        // Удаление подзадач, связанных с эпиком
        for (Subtask subtask : epics.get(id).getSubtasks()) {
            deleteSubtask(subtask.getId());
        }
        epics.remove(id);
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
            throw new IllegalArgumentException("Эпик с ID " + subtask.getEpicId() + " отсутствует.");
        }

        epics.get(subtask.getEpicId()).addSubtask(subtask);
        updateEpicStatus(subtask.getEpicId());
        return subtask;
    }

    public Subtask getSubtaskById(int id) {
        return subtasks.get(id);
    }

    public void updateSubtask(Subtask subtask) {
        subtasks.put(subtask.getId(), subtask);
        updateEpicStatus(subtask.getEpicId());
    }

    public void deleteSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        epics.get(subtask.getEpicId()).removeSubtask(id);
        subtasks.remove(id);
        updateEpicStatus(subtask.getEpicId());
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
        tasks.clear();
    }

    public void clearEpics() {
        for (Epic epic : epics.values()) {
            for (Subtask subtask : epic.getSubtasks()) {
                deleteSubtask(subtask.getId());
            }
        }
        epics.clear();
    }

    public void clearSubtasks() {
        for (Subtask subtask : subtasks.values()) {
            epics.get(subtask.getEpicId()).removeSubtask(subtask.getId());
        }
        subtasks.clear();
    }
}