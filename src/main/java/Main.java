import manager.history.task.Epic;
import manager.history.task.Subtask;
import manager.history.task.Task;
import manager.history.task.TaskStatus;
import manager.history.task.manager.Managers;
import manager.history.task.manager.TaskManager;

public class Main {
    public static void main(String[] args) {
        TaskManager taskManager = Managers.getDefault();

        // Создание задач
        Task task1 = new Task("Задача 1", "Описание задачи 1");
        Task task2 = new Task("Задача 2", "Описание задачи 2");

        // Создание эпиков
        Epic epic1 = taskManager.addEpic(new Epic("Эпик 1", "Описание эпика 1"));
        Epic epic2 = taskManager.addEpic(new Epic("Эпик 2", "Описание эпика 2"));

        // Создание подзадач
        Subtask subtask1 = new Subtask("Подзадача 1", "Описание подзадачи 1", epic1.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание подзадачи 2", epic1.getId());
        Subtask subtask3 = new Subtask("Подзадача 3", "Описание подзадачи 3", epic2.getId());

        // Добавление задач в менеджер
        taskManager.addTask(task1);
        taskManager.addTask(task2);
        taskManager.addSubtask(subtask1);
        taskManager.addSubtask(subtask2);
        taskManager.addSubtask(subtask3);

        // Вывод списка задач
        System.out.println();
        System.out.println("Список задач:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println(task);
        }

        // Вывод списка эпиков
        System.out.println();
        System.out.println("Список эпиков:");
        for (Epic epic : taskManager.getAllEpics()) {
            System.out.println(epic);
        }

        // Вывод списка подзадач
        System.out.println();
        System.out.println("Список подзадач:");
        for (Subtask subtask : taskManager.getAllSubtasks()) {
            System.out.println(subtask);
        }

        // Изменение статусов задач
        task1.setStatus(TaskStatus.IN_PROGRESS);
        subtask1.setStatus(TaskStatus.DONE);
        taskManager.updateTask(task1);
        taskManager.updateSubtask(subtask1);

        // Вывод обновленных статусов
        System.out.println();
        System.out.println("Обновленный список задач:");
        for (Task task : taskManager.getAllTasks()) {
            System.out.println(task);
        }
        System.out.println();
        System.out.println("Обновленный список подзадач:");
        for (Subtask subtask : taskManager.getAllSubtasks()) {
            System.out.println(subtask);
        }

        // Проверка обновления статуса эпика
        System.out.println();
        System.out.println("Проверка обновления статуса эпика:");
        System.out.println(String.format("Статус эпика 2: %s", taskManager.getEpicById(epic2.getId()).getStatus()));
        //  Добавление новой подзадачи для обновления статуса эпика 2
        Subtask subtask4 = new Subtask("Подзадача 4", "Описание подзадачи 4", epic2.getId());
        taskManager.addSubtask(subtask4);
        System.out.println(String.format("Статус эпика 2 после обновления: %s",
                taskManager.getEpicById(epic2.getId()).getStatus()));

        // Просмотр задач
        System.out.println();
        System.out.println("Просмотр задач:");
        taskManager.getTaskById(task1.getId());
        taskManager.getSubtaskById(subtask1.getId());
        taskManager.getEpicById(epic1.getId());

        // Вывод истории просмотров
        System.out.println();
        System.out.println("История просмотров:");
        for (Task task : taskManager.getHistory()) {
            System.out.println(task);
        }
    }
}