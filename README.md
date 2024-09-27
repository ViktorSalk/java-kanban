# java-kanban

### Распределение функциональности между классами:

***manager.history.task.Task:*** Базовый класс для всех типов задач, содержащий общие свойства и методы.
***manager.history.task.Subtask:*** Подкласс manager.history.task.Task, представляющий подзадачи, связанные с эпиками.
***manager.history.task.Epic:*** Подкласс manager.history.task.Task, представляющий эпики, которые могут содержать подзадачи.
***manager.history.task.TaskManager:*** Менеджер задач, отвечающий за управление всеми задачами, эпиками и подзадачами.


### Взаимодействие классов:

***manager.history.task.TaskManager*** создает и управляет экземплярами ***manager.history.task.Task***, ***manager.history.task.Epic*** и ***manager.history.task.Subtask***.
***manager.history.task.TaskManager*** предоставляет методы для добавления, обновления, удаления и получения задач, эпиков и подзадач.
***manager.history.task.Epic*** содержит список ***manager.history.task.Subtask*** и обновляет свой статус на основе статусов подзадач.
***manager.history.task.Subtask*** ссылается на ***manager.history.task.Epic***, к которому он принадлежит.


### Наследование:

***manager.history.task.Subtask*** наследует от ***manager.history.task.Task***, что позволяет ему использовать общие свойства и методы.
***manager.history.task.Epic*** наследует от ***manager.history.task.Task***, что позволяет ему использовать общие свойства и методы, а также добавлять функциональность для управления подзадачами.


### Хранение задач:

***manager.history.task.TaskManager*** использует ***HashMap*** для хранения задач, эпиков и подзадач, что обеспечивает быстрый доступ по идентификатору.


### Интерфейс:

Приложение не имеет явного интерфейса, но ***manager.history.task.TaskManager*** предоставляет методы для взаимодействия с задачами, эпиками и подзадачами.