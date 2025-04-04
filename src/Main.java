import java.util.Random;

public class Main {
    static int threadCount = 20;
    static double step = 0.5;
    static Object startLock = new Object();
    static volatile boolean started = false;

    public static void main(String[] args) {
        Thread[] threads = new Thread[threadCount];
        Random rand = new Random();

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            int delay = rand.nextInt(7001) + 3000;

            threads[i] = new Thread(() -> calculateSequence(index, step, delay));
            threads[i].start();
        }

        synchronized (startLock) {
            started = true;
            startLock.notifyAll();
        }
    }

    static void calculateSequence(int id, double step, int workTime) {
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
        int elapsed = 0;
        int interval = 10;

        while (elapsed < workTime) {
            sum += current;
            current += step;
            count++;
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ignored) {}
            elapsed += interval;
        }

        System.out.printf("[Потік %d] Завершився після %d мс. Сума: %.2f, Елементів: %d%n", id + 1, workTime, sum, count);
    }
}
