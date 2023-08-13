package com.github.viktor235.gameretriever.helper;

/**
 * <code>
 * bw = new BackgroundWorker2();
 * <p>
 * bw.start();
 * <p>
 * bw.interrupt();
 * </code>
 */
public class BackgroundWorker extends Thread {

    private final Runnable action;
    private final int sleepInterval;

    public BackgroundWorker(Runnable action, int sleepInterval) {
        this.action = action;
        this.sleepInterval = sleepInterval;
    }

    @Override
    public void run() {
        do {
            if (interrupted()) {
                return; // Stop thread
            } else {
                action.run(); // Do action
            }

            try {
                Thread.sleep(sleepInterval);
            } catch (InterruptedException e) {
                return; // Stop thread while sleep
            }
        }
        while (true);
    }
}
