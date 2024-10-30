package manager.history;

import manager.history.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {
    private final Map<Integer, Node> taskNodes = new HashMap<>();
    private Node head;
    private Node tail;

    public InMemoryHistoryManager() {
        head = null;
        tail = null;
    }

    @Override
    public void add(Task task) {
        if (task != null) {
            remove(task.getId());
            Node newNode = new Node(task);
            if (head == null) {
                head = newNode;
                tail = newNode;
            } else {
                tail.next = newNode;
                newNode.prev = tail;
                tail = newNode;
            }
            taskNodes.put(task.getId(), newNode);
        }
    }

    @Override
    public void remove(int id) {
        Node nodeToRemove = taskNodes.get(id);
        if (nodeToRemove != null) {
            if (nodeToRemove == head && nodeToRemove == tail) {
                head = null;
                tail = null;
            } else if (nodeToRemove == head) {
                head = head.next;
                head.prev = null;
            } else if (nodeToRemove == tail) {
                tail = tail.prev;
                tail.next = null;
            } else {
                nodeToRemove.prev.next = nodeToRemove.next;
                nodeToRemove.next.prev = nodeToRemove.prev;
            }
            taskNodes.remove(id);
        }
    }

    @Override
    public List<Task> getHistory() {
        List<Task> history = new ArrayList<>();
        Node current = head;
        while (current != null) {
            history.add(current.task);
            current = current.next;
        }
        return history;
    }

    private static class Node {
        Task task;
        Node next;
        Node prev;

        public Node(Task task) {
            this.task = task;
            this.next = null;
            this.prev = null;
        }
    }
}