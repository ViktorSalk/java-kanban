package manager;

import task.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
        loadFromFile(); // Загружать существующие задачи из файла при создании
    }

    protected void loadFromFile() {
        try {
            if (file.exists() && Files.size(file.toPath()) > 0) { // Проверьте, есть ли файл и не является ли он пустым
                List<String> lines = Files.readAllLines(file.toPath());
                Map<Integer, Task> tempTaskMap = new HashMap<>();

                for (String line : lines.subList(1, lines.size())) { //Пропустить заголовок
                    if (!line.isEmpty()) {
                        Task task = fromString(line);
                        tempTaskMap.put(task.getId(), task); // Сохранить в мапу
                    }
                }
                // Добавляйте задачи только в том случае, если они еще не существуют
                for (Task task : tempTaskMap.values()) {
                    if (task.getType() == TaskType.EPIC) {
                        addEpic((Epic) task);
                    } else if (task.getType() == TaskType.SUBTASK) {
                        addSubtask((Subtask) task);
                    } else {
                        addTask(task);
                    }
                }

                updateNextTaskId(); // Проверяем на корректность следующий ID
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Error loading tasks", e);
        }
    }

    private void updateNextTaskId() {
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
        sb.append("id,type,name,status,description,duration,startTime,epic\n");

        for (Epic epic : getAllEpics()) {
            sb.append(toString(epic)).append("\n");
        }

        for (Subtask subtask : getAllSubtasks()) {
            sb.append(toString(subtask)).append("\n");
        }

        for (Task task : getAllTasks()) {
            sb.append(toString(task)).append("\n");
        }

        try {
            Files.writeString(file.toPath(), sb.toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении задач", e);
        }
    }

    // Преобразуйте задачу в строковое представление CSV
    private String toString(Task task) {
        if (task.getType() == TaskType.SUBTASK) {
            return String.format("%d,%s,%s,%s,%s,%d,%s,%d", task.getId(), TaskType.SUBTASK,
                    task.getName(), task.getStatus(), task.getDescription(),
                    task.getDuration().toMinutes(), task.getStartTime(),
                    ((Subtask) task).getEpicId());
        } else if (task.getType() == TaskType.EPIC) {
            return String.format("%d,%s,%s,%s,%s,%d,%s,", task.getId(), TaskType.EPIC,
                    task.getName(), task.getStatus(), task.getDescription(),
                    task.getDuration().toMinutes(), task.getStartTime());
        } else {
            return String.format("%d,%s,%s,%s,%s,%d,%s,", task.getId(), TaskType.TASK,
                    task.getName(), task.getStatus(), task.getDescription(),
                    task.getDuration().toMinutes(), task.getStartTime());
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
        Duration duration = Duration.ofMinutes(Long.parseLong(parts[5]));
        LocalDateTime startTime = null;
        if (parts[6] != null && !parts[6].equals("null")) {
            startTime = LocalDateTime.parse(parts[6]);
        }

        switch (type) {
            case TASK:
                Task task = new Task(name, description);
                task.setId(id);
                task.setStatus(status);
                task.setDuration(duration);
                task.setStartTime(startTime);
                return task;
            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                epic.setDuration(duration);
                epic.setStartTime(startTime);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(parts[7]);
                Subtask subtask = new Subtask(name, description, epicId);
                subtask.setId(id);
                subtask.setStatus(status);
                subtask.setDuration(duration);
                subtask.setStartTime(startTime);
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
        Task task2 = new Task("Задача 2", "Описание задачи 2");
        Epic epic1 = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Epic epic2 = taskManager.addEpic(new Epic("Эпик 2", "Описание эпика 2"));
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", epic1.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", epic1.getId());
        Subtask subtask3 = new Subtask("Подзадача 3", "Описание подзадачи 3", epic2.getId());
        Subtask subtask4 = new Subtask("Подзадача 4", "Описание подзадачи 4", epic2.getId());

        // Добавление задач и подзадач
        taskManager.addTask(task1);
        taskManager.addTask(task2);
        taskManager.addSubtask(subtask1);
        taskManager.addSubtask(subtask2);
        taskManager.addSubtask(subtask3);
        taskManager.addSubtask(subtask4);

        // Сохранение текущего состояния
        taskManager.save();

        // Выведите текущие задачи, чтобы проверить, правильно ли они сохранены
        System.out.println("Текущие задачи, сохраненные в файл:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println(task);
        }
        System.out.println("Текущие Эпики, сохраненные в файл:");
        for (Epic epic : taskManager.getAllEpics()) {
            System.out.println(epic);
        }
        System.out.println("Текущие Подзадачи, сохраненные в файл:");
        for (Subtask subtask : taskManager.getAllSubtasks()) {
            System.out.println(subtask);
        }

        // Создайте новый файловый менеджер задач с поддержкой файлов из того же файла
        FileBackedTaskManager newTaskManager = new FileBackedTaskManager(tempFile);

        // Убедитесь, что все задачи, эпики и подзадачи загружены правильно
        System.out.println("Задачи, загруженные из старого менеджера в новый:");
        for (Task task : newTaskManager.getAllTasks()) {
            System.out.println(task);
        }

        System.out.println("Эпики, загруженные из старого менеджера в новый:");
        for (Epic epic : newTaskManager.getAllEpics()) {
            System.out.println(epic);
        }

        System.out.println("Подзадачи, загруженные из старого менеджера в новый:");
        for (Subtask subtask : newTaskManager.getAllSubtasks()) {
            System.out.println(subtask);
        }
        // Убедитесь, что старые и новые задачи, эпики и подзадачи совпадают
        System.out.println("\nПроверка:");
        System.out.println("Задачи совпадают: " + (taskManager.getAllTasks().equals(newTaskManager.getAllTasks())));
        System.out.println("Эпики совпадают: " + (taskManager.getAllEpics().equals(newTaskManager.getAllEpics())));
        System.out.println("Подзадачи совпадают: " + (taskManager.getAllSubtasks().equals(newTaskManager.getAllSubtasks())));
    }
}