package net.arcana.friendsforlife;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaskScheduler {

    private static final List<Task> TASKS = new ArrayList<>();

    public static void register() {

    }

    public static void schedule(Runnable action, int ticks) {
        TASKS.add(new Task(ticks, action));
    }

    public static void tick() {
        Iterator<Task> it = TASKS.iterator();

        while (it.hasNext()) {
            Task t = it.next();
            t.ticks--;

            if (t.ticks <= 0) {
                t.action.run();
                it.remove();
            }
        }
    }

    private static class Task {
        int ticks;
        Runnable action;

        Task(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }
}
