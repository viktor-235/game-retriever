package com.github.viktor235.gameretriever.utils;

public class BackgroundWorker extends Thread {

    private volatile boolean running = true;
    private volatile boolean paused = true;
    private final Object pauseLock = new Object();

    public BackgroundWorker(Runnable action, int sleepInterval) {
        this.action = action;
        this.sleepInterval = sleepInterval;
    }

    private final Runnable action;
    private final int sleepInterval;

    @Override
    public void run() {
        while (running) {
            synchronized (pauseLock) {
                if (!running) { // May have changed while waiting to synchronize on pauseLock
                    break;
                }
                if (paused) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (!running) {
                        break;
                    }
                }
            }

            action.run();

            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

    public void stopWorker() {
        running = false;
        resumeWorker();
    }

    public void pauseWorker() {
        paused = true;
    }

    public void resumeWorker() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
    }
}
