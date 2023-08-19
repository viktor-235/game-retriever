package com.github.viktor235.gameretriever.shell;

import com.github.viktor235.gameretriever.helper.BackgroundWorker;
import org.jline.terminal.Terminal;

public class Spinner implements AutoCloseable {

    private static final String CUU = "\u001B[A";
    private static final String CLEAR_LINE = "\u001B[2K";

    private final char[] spinner
            = "⣷⣯⣟⡿⢿⣻⣽⣾".toCharArray();
//            = "|/-\\".toCharArray();
//            = "▁▃▄▅▆▇█▇▆▅▄▃".toCharArray();

    private final BackgroundWorker bgWorker;
    private final ShellHelper shellHelper;

    private byte spinnerIndex = 0;
    private boolean inProgress;
    private String message;

    public Spinner(ShellHelper shellHelper) {
        this.shellHelper = shellHelper;

        bgWorker = new BackgroundWorker(
                () -> this.display(true),
                200
        );
    }

    public static Spinner start(ShellHelper shellHelper) {
        return start(shellHelper, null);
    }

    public static Spinner start(ShellHelper shellHelper, String message) {
        Spinner spinner = new Spinner(shellHelper);
        spinner.start(message);
        return spinner;
    }

    public void start() {
        start("");
    }

    public void start(String message) {
        setMessage(message);
        spinnerIndex = 0;
        shellHelper.println();
        inProgress = true;
        bgWorker.start();
    }

    public void info(String message) {
        stop(shellHelper.getInfo(message));
    }

    public void success(String message) {
        stop(shellHelper.getSuccess(message));
    }

    public void warning(String message) {
        stop(shellHelper.getWarning(message));
    }

    public void error(String message) {
        stop(shellHelper.getError(message));
    }

    public void stop(String message) {
        setMessage(message);
        bgWorker.interrupt();
        display(false);
        inProgress = false;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private synchronized void display(boolean spinning) {
        if (!inProgress)
            return;

        Terminal terminal = shellHelper.getTerminal();
        terminal.writer().print(CUU + CLEAR_LINE + "\r"); // Clear prev. msg

        if (spinning) {
            String spinner = shellHelper.getInfo(getSpinnerChar() + " ");
            if (message != null) {
                terminal.writer().printf("%s%s%n", spinner, shellHelper.getInfo(message));
            } else {
                terminal.writer().printf("%s%n", spinner);
            }
        } else {
            if (message != null) {
                terminal.writer().printf("%s%n", shellHelper.getInfo(message));
            }
        }

        terminal.flush();
    }

    private char getSpinnerChar() {
        if (spinnerIndex >= spinner.length) {
            spinnerIndex = 0;
        }
        return spinner[spinnerIndex++];
    }

    @Override
    public void close() {
        stop(null);
    }
}
