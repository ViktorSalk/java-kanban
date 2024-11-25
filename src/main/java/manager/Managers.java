package manager;

import history.HistoryManager;
import history.InMemoryHistoryManager;

public class Managers {
    private static InMemoryTaskManager taskManager;

    public static InMemoryTaskManager getInMemoryTaskManager() {
        if (taskManager == null) {
            taskManager = new InMemoryTaskManager();
        }
        return taskManager;
    }

    private Managers() {
    }

    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}