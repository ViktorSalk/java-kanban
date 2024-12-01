package manager;

import history.HistoryManager;
import history.InMemoryHistoryManager;

public class Managers {
    private static InMemoryTaskManager taskManager;

    public static TaskManager getDefault() {
        return new InMemoryTaskManager(); // Создает и возвращает новый экземпляр InMemoryTaskManager
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager(); // Возвращает новый экземпляр InMemoryHistoryManager
    }

    private Managers() {
        // Предотвратить создание экземпляра
    }
}
