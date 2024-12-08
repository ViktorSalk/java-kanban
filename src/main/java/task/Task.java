package task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public class Task {
    protected int id;
    protected String name;
    protected String description;
    protected TaskStatus status;
    protected Duration duration; // Продолжительность выполнения задачи
    protected LocalDateTime startTime; // Время начала выполнения задачи

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = TaskStatus.NEW;
        this.duration = Duration.ZERO; // Продолжительность по умолчанию
        this.startTime = null; // Время начала по умолчанию
    }

    public Task(String name, String description, TaskStatus status, Duration duration, LocalDateTime startTime) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.duration = duration;
        this.startTime = startTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        if (startTime != null && duration != null) {
            return startTime.plus(duration);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Task {" +
                "id= " + id +
                ", name= '" + name + '\'' +
                ", description= " + description +
                ", status= " + status +
                ", duration= " + duration +
                ", startTime= " + startTime +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return id == task.id &&
                name.equals(task.name) &&
                description.equals(task.description) &&
                status == task.status &&
                duration.equals(task.duration) &&
                Objects.equals(startTime, task.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, status, duration, startTime);
    }

    public TaskType getType() {
        return TaskType.TASK;
    }
}