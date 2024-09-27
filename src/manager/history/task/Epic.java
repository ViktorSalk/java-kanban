package manager.history.task;

import java.util.ArrayList;
import java.util.List;

class Epic extends Task {
    private List<Subtask> subtasks = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public void addSubtask(Subtask subtask) {
        subtasks.add(subtask);
    }

    public void removeSubtask(int subtaskId) {
        subtasks.removeIf(subtask -> subtask.getId() == subtaskId);
    }

    public List<Subtask> getSubtasks() {
        return subtasks;
    }

    public void updateSubtask(Subtask subtask) {
        for (int i = 0; i < subtasks.size(); i++) {
            if (subtasks.get(i).getId() == subtask.getId()) {
                subtasks.set(i, subtask);
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "Epic {" +
                "id= " + getId() +
                ", name= '" + getName() + '\'' +
                ", description= " + getDescription() +
                ", status= " + getStatus() +
                ", subtasks= " + subtasks +
                '}';
    }
}