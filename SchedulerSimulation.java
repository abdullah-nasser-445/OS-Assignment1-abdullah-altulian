
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

// Terminal formatting
class TerminalStyle {
    static final String RESET = "\u001B[0m";
    static final String BOLD = "\u001B[1m";
    static final String CYAN = "\u001B[36m";
    static final String GREEN = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String BLUE = "\u001B[34m";
    static final String PURPLE = "\u001B[35m";
    static final String RED = "\u001B[31m";
}

// Represents one CPU process
class Task implements Runnable {
    private final String taskName;
    private final int totalBurst;
    private final int quantum;
    private final int priority;

    private int remainingBurst;
    private long arrivalTime;
    private long readySince;
    private long waitingTime;
    private long completionTime;

    public Task(String name, int burst, int slice, int level) {
        taskName = name;
        totalBurst = burst;
        quantum = slice;
        priority = level;
        remainingBurst = burst;

        arrivalTime = System.currentTimeMillis();
        readySince = arrivalTime;
        waitingTime = 0;
        completionTime = 0;
    }

    @Override
    public void run() {
        int executionTime = Math.min(quantum, remainingBurst);

        System.out.println(
            TerminalStyle.GREEN + "\n>> " + taskName +
            TerminalStyle.RESET + " is running for " +
            executionTime + " ms"
        );

        int steps = 5;

        for (int i = 1; i <= steps; i++) {
            int elapsed = (executionTime * i) / steps;
            int previous = (executionTime * (i - 1)) / steps;
            int delay = elapsed - previous;

            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.out.println(
                    TerminalStyle.RED + "Execution interrupted: " +
                    taskName + TerminalStyle.RESET
                );
                return;
            }

            showProgress(i * 100 / steps);
        }

        remainingBurst -= executionTime;

        System.out.println(
            "\n" + TerminalStyle.YELLOW +
            taskName + " | Remaining: " + remainingBurst + " ms" +
            TerminalStyle.RESET
        );

        if (remainingBurst == 0) {
            completionTime = System.currentTimeMillis();

            System.out.println(
                TerminalStyle.GREEN + taskName +
                " completed successfully." + TerminalStyle.RESET
            );
        } else {
            System.out.println(
                TerminalStyle.BLUE + taskName +
                " returned to the ready queue." + TerminalStyle.RESET
            );
        }
    }

    private void showProgress(int percentage) {
        int width = 20;
        int filled = percentage * width / 100;
        StringBuilder bar = new StringBuilder("[");

        for (int i = 0; i < width; i++) {
            bar.append(i < filled ? "#" : "-");
        }

        bar.append("] ").append(percentage).append("%");

        System.out.print("\r" + bar);

        if (percentage == 100) {
            System.out.println();
        }
    }

    public void recordWaitingTime() {
        waitingTime += System.currentTimeMillis() - readySince;
    }

    public void enterReadyQueue() {
        readySince = System.currentTimeMillis();
    }

    public String getTaskName() {
        return taskName;
    }

    public int getTotalBurst() {
        return totalBurst;
    }

    public int getRemainingBurst() {
        return remainingBurst;
    }

    public int getPriority() {
        return priority;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public long getTurnaroundTime() {
        if (completionTime == 0) {
            return 0;
        }

        return completionTime - arrivalTime;
    }

    public boolean isComplete() {
        return remainingBurst <= 0;
    }
}

public class SchedulerSimulation {

    private static int dispatchCount = 0;

    public static void main(String[] args) {
        // Replace this value with your assigned student ID.
        int studentId = 445050084;
        Random generator = new Random(studentId);

        int timeSlice = 2000 + generator.nextInt(4) * 1000;
        int taskCount = 10 + generator.nextInt(11);

        Queue<Task> readyQueue = new ArrayDeque<>();
        List<Task> allTasks = new ArrayList<>();

        printHeader(taskCount, timeSlice, studentId);

        // Generate processes
        for (int i = 1; i <= taskCount; i++) {
            int burst = timeSlice / 2
                    + generator.nextInt(2 * timeSlice + 1);

            int priority = 1 + generator.nextInt(10);

            Task task = new Task(
                "Task-" + i,
                burst,
                timeSlice,
                priority
            );

            readyQueue.offer(task);
            allTasks.add(task);

            System.out.println(
                TerminalStyle.CYAN + "Added " +
                task.getTaskName() +
                TerminalStyle.RESET +
                " | Burst: " + burst + " ms" +
                " | Priority: " + priority
            );
        }

        System.out.println(
            "\n" + TerminalStyle.BOLD +
            "========== ROUND ROBIN START ==========" +
            TerminalStyle.RESET
        );

        Task previousTask = null;

        while (!readyQueue.isEmpty()) {
            Task current = readyQueue.poll();

            if (previousTask != null && previousTask != current) {
                dispatchCount++;
            }

            current.recordWaitingTime();

            System.out.println(
                "\nReady queue: " + queueContents(readyQueue)
            );

            Thread worker = new Thread(current);
            worker.start();

            try {
                worker.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                System.out.println("Scheduler interrupted.");
                return;
            }

            previousTask = current;

            if (!current.isComplete()) {
                current.enterReadyQueue();
                readyQueue.offer(current);
            }
        }

        System.out.println(
            "\n" + TerminalStyle.GREEN +
            "========== ALL TASKS COMPLETED ==========" +
            TerminalStyle.RESET
        );

        System.out.println(
            "Context switches: " + dispatchCount
        );

        printStatistics(allTasks);
    }

    private static String queueContents(Queue<Task> queue) {
        if (queue.isEmpty()) {
            return "[empty]";
        }

        StringBuilder result = new StringBuilder("[");
        boolean first = true;

        for (Task task : queue) {
            if (!first) {
                result.append(" -> ");
            }

            result.append(task.getTaskName());
            first = false;
        }

        result.append("]");
        return result.toString();
    }

    private static void printHeader(
            int count, int quantum, int studentId) {

        System.out.println(
            "\n" + TerminalStyle.BOLD +
            "========================================\n" +
            "          CPU SCHEDULER SIMULATION      \n" +
            "========================================" +
            TerminalStyle.RESET
        );

        System.out.println("Number of processes: " + count);
        System.out.println("Time quantum: " + quantum + " ms");
        System.out.println("Student ID: " + studentId);
        System.out.println("Scheduling algorithm: Round Robin\n");
    }

    private static void printStatistics(List<Task> tasks) {
        long totalWaiting = 0;
        long totalTurnaround = 0;

        System.out.println(
            "\n" + TerminalStyle.BOLD +
            "=============== PROCESS STATISTICS ===============" +
            TerminalStyle.RESET
        );

        System.out.printf(
            "%-12s %-12s %-12s %-16s %-18s%n",
            "Process", "Burst(ms)", "Priority",
            "Waiting(ms)", "Turnaround(ms)"
        );

        System.out.println(
            "-----------------------------------------------------------------------"
        );

        for (Task task : tasks) {
            long waiting = task.getWaitingTime();
            long turnaround = task.getTurnaroundTime();

            totalWaiting += waiting;
            totalTurnaround += turnaround;

            System.out.printf(
                "%-12s %-12d %-12d %-16d %-18d%n",
                task.getTaskName(),
                task.getTotalBurst(),
                task.getPriority(),
                waiting,
                turnaround
            );
        }

        if (!tasks.isEmpty()) {
            double averageWaiting =
                (double) totalWaiting / tasks.size();

            double averageTurnaround =
                (double) totalTurnaround / tasks.size();

            System.out.println(
                "-----------------------------------------------------------------------"
            );

            System.out.printf(
                "Average waiting time: %.2f ms%n",
                averageWaiting
            );

            System.out.printf(
                "Average turnaround time: %.2f ms%n",
                averageTurnaround
            );
        }
    }
}