import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    static int threadCount = 20;
    static double step = 0.5;
    static final Object startLock = new Object();
    static volatile boolean started = false;

    public static void main(String[] args) {
        List<WorkerThread> workers = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < threadCount; i++) {
            int delay = rand.nextInt(7001) + 3000;
            delays.add(delay);
            workers.add(new WorkerThread(i + 1, step));
        }

        // Запуск усіх обчислювальних потоків
        for (WorkerThread worker : workers) {
            new Thread(worker).start();
        }

        // Синхронний запуск усіх потоків
        synchronized (startLock) {
            started = true;
            startLock.notifyAll();
        }

        // Старт контролера
        new ControllerThread(workers, delays).start();
    }

    static class WorkerThread implements Runnable {
        private final int id;
        private final double step;
        private final AtomicBoolean running = new AtomicBoolean(true);

        public WorkerThread(int id, double step) {
            this.id = id;
            this.step = step;
        }

        public void stop() {
            running.set(false);
        }

        @Override
        public void run() {
            synchronized (startLock) {
                while (!started) {
                    try {
                        startLock.wait();
                    } catch (InterruptedException ignored) {}
                }
            }

            double sum = 0;
            int count = 0;
            double current = 0;

            while (running.get()) {
                sum += current;
                current += step;
                count++;
            }

            System.out.printf("[Потік %d] Завершився. Сума: %.2f, Елементів: %d%n", id, sum, count);
        }
    }

    static class ControllerThread {
        private final List<WorkerThread> threads;
        private final List<Integer> stopTimes;

        public ControllerThread(List<WorkerThread> threads, List<Integer> stopTimes) {
            this.threads = threads;
            this.stopTimes = stopTimes;
        }

        public void start() {
            new Thread(() -> {
                List<ThreadInfo> infos = new ArrayList<>();
                for (int i = 0; i < threads.size(); i++) {
                    infos.add(new ThreadInfo(i, stopTimes.get(i)));
                }

                infos.sort(Comparator.comparingInt(t -> t.stopTime));

                int previousTime = 0;

                for (ThreadInfo info : infos) {
                    int delay = info.stopTime - previousTime;
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {}
                    threads.get(info.index).stop();
                    previousTime = info.stopTime;
                }
            }).start();
        }

        static class ThreadInfo {
            int index;
            int stopTime;

            ThreadInfo(int index, int stopTime) {
                this.index = index;
                this.stopTime = stopTime;
            }
        }
    }
}