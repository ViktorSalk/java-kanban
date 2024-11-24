package history;

import task.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class HistoryManagerImpl implements HistoryManager {
    private final List<Task> history = new ArrayList<>();

    @Override
    public void loadFromFile(String filename) throws IOException {
        if (!Files.exists(Paths.get(filename))) {
            throw new IOException("Файл не существует");
        }
        // остальной код метода
    }

    @Override
    public void add(Task task) {
        if (task != null && !history.stream().anyMatch(t -> t.getId() == task.getId())) {
            history.add(task);
        }
    }

    @Override
    public void remove(int id) {
        history.removeIf(task -> task.getId() == id);
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}