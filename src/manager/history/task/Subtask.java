package manager.history.task;

class Subtask extends Task {
    private int epicId;

    public Subtask(String name, String description, TaskStatus status, int epicId) {
        super(name, description, status);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    @Override
    public String toString() {
        return "manager.history.task.Subtask {" +
                "id= " + getId() +
                ", name= '" + getName() + '\'' +
                ", description= " + getDescription() +
                ", status= " + getStatus() +
                ", epicId= " + epicId +
                '}';
    }
}