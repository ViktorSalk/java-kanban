package manager;

import task.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
        loadFromFile(); // Загружать существующие задачи из файла при создании
    }

    // Способ загрузки задач из файла
    protected void loadFromFile() {
        try {
            if (file.exists() && file.length() > 0) { // Проверьте, есть ли файл и не является ли он пустым
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines.subList(1, lines.size())) { // Пропустить заголовок
                    if (!line.isEmpty()) {
                        Task task = fromString(line);
                        // Добавляйте задачи только в том случае, если они еще не существуют
                        if (!tasks.containsKey(task.getId())) {
                            if (task instanceof Epic) {
                                this.addEpic((Epic) task);
                            } else if (task instanceof Subtask) {
                                this.addSubtask((Subtask) task);
                            } else {
                                this.addTask(task);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Error loading tasks", e);
        }
    }

    // Другие методы остаются неизменными...

    @Override
    public Task addTask(Task task) {
        Task addedTask = super.addTask(task);
        save(); // Сохранить состояние после добавления
        return addedTask;
    }

    @Override
    public Epic addEpic(Epic epic) {
        Epic addedEpic = super.addEpic(epic);
        save(); // Сохранить состояние после добавления
        return addedEpic;
    }

    @Override
    public Subtask addSubtask(Subtask subtask) {
        Subtask addedSubtask = super.addSubtask(subtask);
        save(); // Сохранить состояние после добавления
        return addedSubtask;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save(); // Сохранить состояние после обновления
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save(); // Сохранить состояние после обновления
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save(); // Сохранить состояние после обновления
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
        save(); // Сохранить состояние после удаления
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save(); // Сохранить состояние после удаления
    }

    @Override
    public void deleteSubtask(int id) {
        super.deleteSubtask(id);
        save(); // Сохранить состояние после удаления
    }

    // Метод сохранения всех задач, эпопей и подзадач в файл
    protected void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("id,type,name,status,description,epic\n");

        for (Task task : getAllTasks()) {
            sb.append(toString(task)).append("\n");
        }

        for (Epic epic : getAllEpics()) {
            sb.append(toString(epic)).append("\n");
        }

        for (Subtask subtask : getAllSubtasks()) {
            sb.append(toString(subtask)).append("\n");
        }

        try {
            Files.writeString(file.toPath(), sb.toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении задач", e);
        }
    }

    // Преобразуйте задачу в строковое представление CSV
    private String toString(Task task) {
        if (task instanceof Subtask) {
            return String.format("%d,%s,%s,%s,%s,%d", task.getId(), TaskType.SUBTASK,
                    task.getName(), task.getStatus(), task.getDescription(),
                    ((Subtask) task).getEpicId());
        } else if (task instanceof Epic) {
            return String.format("%d,%s,%s,%s,%s,", task.getId(), TaskType.EPIC,
                    task.getName(), task.getStatus(), task.getDescription());
        } else {
            return String.format("%d,%s,%s,%s,%s,", task.getId(), TaskType.TASK,
                    task.getName(), task.getStatus(), task.getDescription());
        }
    }

    // Создайте экземпляр задачи из строки CSV
    private Task fromString(String value) {
        String[] parts = value.split(",");
        int id = Integer.parseInt(parts[0]);
        TaskType type = TaskType.valueOf(parts[1]);
        String name = parts[2];
        TaskStatus status = TaskStatus.valueOf(parts[3]);
        String description = parts[4];

        switch (type) {
            case TASK:
                Task task = new Task(name, description);
                task.setId(id);
                task.setStatus(status);
                return task;
            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(parts[5]);
                Subtask subtask = new Subtask(name, description, epicId);
                subtask.setId(id);
                subtask.setStatus(status);
                return subtask;
            default:
                throw new IllegalArgumentException("Unknown task type");
        }
    }

    public static void main(String[] args) throws IOException {
        File tempFile = File.createTempFile("задачи", ".csv");
        tempFile.deleteOnExit(); // Убедитесь, что он будет удален после завершения работы программы

        FileBackedTaskManager taskManager = new FileBackedTaskManager(tempFile);

        // Добавление задач, эпиков и подзадач
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        Epic epic1 = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", epic1.getId());

        // Добавление задач и подзадач
        taskManager.addTask(task1);
        taskManager.addSubtask(subtask1);

        // Выведите текущие задачи, чтобы проверить, правильно ли они сохранены
        System.out.println("Текущие задачи, сохраненные в файл:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println(task);
        }

        // Задачи уже загружены в конструктор, вы можете подтвердить:
        System.out.println("Задачи, загруженные из файла:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println(task);
        }
    }
}