package manager;

import history.HistoryManager;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    protected int nextTaskId = 1;
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();

    // Проверьте, не перекрываются ли два временных интервала
    private boolean isOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    // Проверьте, не пересекается ли новая задача с существующими задачами или подзадачами
    private boolean isTaskOverlapping(Task newTask) {
        LocalDateTime newStart = newTask.getStartTime();
        LocalDateTime newEnd = newTask.getEndTime();

        if (newStart == null || newEnd == null) {
            return false; // Если время начала или окончания не задано, мы не сможем пересекаться
        }

        // Проверьте соответствие всем существующим задачам
        for (Task existingTask : tasks.values()) {
            LocalDateTime existingStart = existingTask.getStartTime();
            LocalDateTime existingEnd = existingTask.getEndTime();

            if (existingStart != null && existingEnd != null) {
                if (isOverlapping(newStart, newEnd, existingStart, existingEnd)) {
                    return true; // Обнаружено перекрытие
                }
            }
        }

        // Проверьте соответствие всем существующим подзадачам
        for (Subtask existingSubtask : subtasks.values()) {
            LocalDateTime existingStart = existingSubtask.getStartTime();
            LocalDateTime existingEnd = existingSubtask.getEndTime();

            if (existingStart != null && existingEnd != null) {
                if (isOverlapping(newStart, newEnd, existingStart, existingEnd)) {
                    return true; // Обнаружено перекрытие
                }
            }
        }

        return false; // Никакого перекрытия
    }

    @Override
    public Task addTask(Task task) {
        if (isTaskOverlapping(task)) {
            throw new IllegalArgumentException("Task overlaps with existing task.");
        }
        task.setId(nextTaskId++);
        tasks.put(task.getId(), task);
        return task;
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public void updateTask(Task task) {
        if (isTaskOverlapping(task)) {
            throw new IllegalArgumentException("Задача перекрывается с существующей задачей.");
        }
        tasks.put(task.getId(), task);
    }

    @Override
    public void deleteTask(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            historyManager.remove(id); // Теперь используется в deleteTask
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    // Методы для эпиков

    @Override
    public Epic addEpic(Epic epic) {
        epic.setId(nextTaskId++);
        epics.put(epic.getId(), epic);
        return epic;
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public void updateEpic(Epic epic) {
        epics.put(epic.getId(), epic);
    }

    @Override
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

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    // Методы для подзадач

    @Override
    public Subtask addSubtask(Subtask subtask) {
        if (isTaskOverlapping(subtask)) {
            throw new IllegalArgumentException("Subtask overlaps with existing task.");
        }
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

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (isTaskOverlapping(subtask)) {
            throw new IllegalArgumentException("Подзадача перекрывается с существующей задачей.");
        }
        subtasks.put(subtask.getId(), subtask);
        updateEpicStatus(subtask.getEpicId());
    }

    @Override
    public void deleteSubtask(int id) {
        subtasks.remove(id);
        historyManager.remove(id); // Теперь используется в deleteSubtask
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    // Метод для получения списка подзадач для заданного эпика
    @Override
    public List<Subtask> getSubtasksByEpicId(int epicId) {
        return epics.get(epicId).getSubtasks();
    }

    // Методы для обновления статуса эпика
    private void updateEpicStatus(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic.getSubtasks().isEmpty()) {
            epic.setStatus(TaskStatus.NEW);
        } else if (epic.getSubtasks().stream().allMatch(subtask -> subtask.getStatus() == TaskStatus.DONE)) {
            epic.setStatus(TaskStatus.DONE);
        } else {
            epic.setStatus(TaskStatus.IN_PROGRESS);
        }
    }

    // Методы для удаления всех задач по типам
    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public void clearTasks() {
        tasks.clear();
    }

    @Override
    public void clearEpics() {
        epics.clear();
    }

    @Override
    public void clearSubtasks() {
        subtasks.clear();
    }

    @Override
    public Optional<Task> getTaskByIdOptional(int id) {
        return Optional.empty();
    }

    @Override
    public Optional<Epic> getEpicByIdOptional(int id) {
        return Optional.empty();
    }

    @Override
    public Optional<Subtask> getSubtaskByIdOptional(int id) {
        return Optional.empty();
    }
}