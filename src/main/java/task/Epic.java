package task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Subtask> subtasks = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public void addSubtask(Subtask subtask) {
        subtasks.add(subtask);
        updateEpicDetails(); // Продолжительность обновления и время начала при добавлении подзадачи
    }

    public void removeSubtask(int subtaskId) {
        subtasks.removeIf(subtask -> subtask.getId() == subtaskId);
        updateEpicDetails(); // Продолжительность обновления и время начала при удалении подзадачи
    }

    public List<Subtask> getSubtasks() {
        return new ArrayList<>(subtasks);
    }

    private void updateEpicDetails() {
        Duration totalDuration = Duration.ZERO;
        LocalDateTime earliestStart = null;

        for (Subtask subtask : subtasks) {
            totalDuration = totalDuration.plus(subtask.getDuration());
            if (subtask.getStartTime() != null) {
                if (earliestStart == null || subtask.getStartTime().isBefore(earliestStart)) {
                    earliestStart = subtask.getStartTime();
                }
            }
        }

        setDuration(totalDuration);
        setStartTime(earliestStart);
    }

    @Override
    public String toString() {
        return null;
    }

    public void updateSubtask(Subtask subtask1) {
    }

    public TaskType getType() {
        return TaskType.EPIC;
    }
}