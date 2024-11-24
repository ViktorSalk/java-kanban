package history;

import task.Task;

import java.io.IOException;
import java.util.List;

public interface HistoryManager {
    void add(Task task);

    void remove(int id);

    List<Task> getHistory();

    void loadFromFile(String s) throws IOException;
}