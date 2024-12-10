package manager;

import history.HistoryManager;
import task.Epic;
import task.Subtask;
import task.Task;
import task.TaskStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTaskManager implements TaskManager {
    protected int nextTaskId = 1;
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager = Managers.getDefaultHistory();
    private final Set<Task> prioritizedTasks = new HashSet<>();

    // Проверьте, не перекрываются ли два временных интервала
    private boolean isOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    // Проверьте, не пересекается ли новая задача с существующими задачами или подзадачами
    private boolean isTaskOverlapping(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getDuration() == null) {
            return false;
        }

        LocalDateTime newStart = newTask.getStartTime();
        LocalDateTime newEnd = newTask.getEndTime();

        return prioritizedTasks.stream()
                .filter(task -> task.getId() != newTask.getId()) // Исключаем саму задачу при обновлении
                .filter(task -> task.getStartTime() != null && task.getDuration() != null)
                .anyMatch(existingTask -> {
                    LocalDateTime existingStart = existingTask.getStartTime();
                    LocalDateTime existingEnd = existingTask.getEndTime();

                    // Проверяем пересечение интервалов
                    return !(newEnd.isEqual(existingStart) || newEnd.isBefore(existingStart) ||
                            newStart.isEqual(existingEnd) || newStart.isAfter(existingEnd));
                });
    }

    @Override
    public Task addTask(Task task) {
        if (task == null) return null;

        // Проверяем пересечение только если у задачи есть время
        if (task.getStartTime() != null && isTaskOverlapping(task)) {
            throw new IllegalArgumentException("Task overlaps with existing task.");
        }

        task.setId(nextTaskId++);
        tasks.put(task.getId(), task);
        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
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
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        // Проверяем существование задачи
        Task existingTask = tasks.get(task.getId());
        if (existingTask == null) {
            throw new IllegalArgumentException("Task with id " + task.getId() + " not found");
        }

        // Проверяем пересечение по времени
        if (task.getStartTime() != null && isTaskOverlapping(task)) {
            throw new IllegalArgumentException("Task time overlaps with existing task");
        }

        tasks.put(task.getId(), task);
        prioritizedTasks.remove(existingTask);
        prioritizedTasks.add(task);
    }

    @Override
    public void deleteTask(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            prioritizedTasks.remove(task);
            tasks.remove(id);
            historyManager.remove(id);
        }
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    // Методы для эпиков

    @Override
    public Epic addEpic(Epic epic) {
        if (epic == null) {
            return null;
        }

        // Проверяем пересечения для всех подзадач эпика
        if (epic.getSubtasks() != null) {
            for (Subtask subtask : epic.getSubtasks()) {
                if (subtask.getStartTime() != null && isTaskOverlapping(subtask)) {
                    throw new IllegalArgumentException("Task time overlaps with existing task");
                }
            }
        }

        epic.setId(nextTaskId++);
        epics.put(epic.getId(), epic);

        // Добавляем подзадачи, если они есть
        if (epic.getSubtasks() != null) {
            for (Subtask subtask : epic.getSubtasks()) {
                addSubtask(subtask);
            }
        }

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
        if (subtask == null) {
            return null;
        }

        // Проверка на существование эпика
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) {
            throw new IllegalArgumentException("Epic with ID " + subtask.getEpicId() + " does not exist");
        }

        // Проверка на пересечение по времени
        if (subtask.getStartTime() != null && isTaskOverlapping(subtask)) {
            throw new IllegalArgumentException("Task time overlaps with existing task");
        }

        subtask.setId(nextTaskId++);
        subtasks.put(subtask.getId(), subtask);
        epic.addSubtask(subtask);
        prioritizedTasks.add(subtask);
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
        tasks.values().forEach(prioritizedTasks::remove);
        tasks.clear();
    }

    @Override
    public void clearEpics() {
        for (Epic epic : epics.values()) {
            epic.getSubtasks().forEach(prioritizedTasks::remove);
        }
        epics.clear();
        subtasks.clear();
    }

    @Override
    public void clearSubtasks() {
        subtasks.values().forEach(prioritizedTasks::remove);
        subtasks.clear();
        for (Epic epic : epics.values()) {
            epic.getSubtasks().clear();
            updateEpicStatus(epic.getId());
        }
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        List<Task> result = new ArrayList<>();
        result.addAll(tasks.values());
        result.addAll(subtasks.values());

        return result.stream()
                .filter(task -> task.getStartTime() != null)
                .sorted((t1, t2) -> {
                    if (t1.getStartTime() == null && t2.getStartTime() == null) {
                        return Integer.compare(t1.getId(), t2.getId());
                    }
                    if (t1.getStartTime() == null) return 1;
                    if (t2.getStartTime() == null) return -1;
                    int timeCompare = t1.getStartTime().compareTo(t2.getStartTime());
                    return timeCompare != 0 ? timeCompare : Integer.compare(t1.getId(), t2.getId());
                })
                .collect(Collectors.toList());
    }


    @Override
    public void validateTaskTime(Task task) throws IllegalArgumentException {
        if (isTaskOverlapping(task)) {
            throw new IllegalArgumentException("Task time overlaps with existing task");
        }
    }
}