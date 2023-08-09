package com.github.viktor235.gameretriever.shell;

import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.utils.BackgroundWorker;
import lombok.RequiredArgsConstructor;
import org.jline.terminal.Terminal;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static liquibase.repackaged.org.apache.commons.lang3.StringUtils.repeat;

@Component
@RequiredArgsConstructor
public class ShellSpinner {

    private final ShellHelper shellHelper;

    private static final String CUU = "\u001B[A";

    private final char[] spinner
            = "⣷⣯⣟⡿⢿⣻⣽⣾".toCharArray();
//            = "|/-\\".toCharArray();
//            = "▁▃▄▅▆▇█▇▆▅▄▃".toCharArray();

    private byte spinnerIndex = 0;
    private String message;
    private int prevMsgLength = 0;

    private BackgroundWorker backgroundWorker;

    @PostConstruct
    private void init() throws AppException {
        backgroundWorker = new BackgroundWorker(() -> this.display(false), 200);
        backgroundWorker.start();
    }

    public void start() {
        start("");
    }

    public void start(String message) {
        setMessage(message);
        shellHelper.println();
        spinnerIndex = 0;
        backgroundWorker.resumeWorker();
    }

    public void setMessage(String message) {
        prevMsgLength = this.message == null ? 0 : this.message.length();
        this.message = message;
    }

    private void display(boolean stopped) {
        //FIXME Issue when setMessage calls very often. When converter works, for example
        Terminal terminal = shellHelper.getTerminal();
        terminal.writer().print(CUU + "\r" + repeat(' ', 2 + prevMsgLength) + "\n"); // Erase prev msg

        if (stopped) {
            if (message != null) {
                terminal.writer().printf(CUU + "%s%n", shellHelper.getInfo(message));
            } else {
                message = "";
                terminal.writer().printf(CUU + "\r");
            }
        } else {
            String spinner = shellHelper.getInfo(getSpinnerChar() + " ");
            if (message != null) {
                terminal.writer().printf(CUU + "%s%s%n", spinner, shellHelper.getInfo(message));
            } else {
                terminal.writer().printf(CUU + "%s%n", spinner);
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

    public void stop() {
        stop(null);
    }

    public void stopIfSpinning() {
        stop(this.message);
    }

    public void stop(String message) {
        setMessage(message);
        backgroundWorker.pauseWorker();
        display(true);
    }
}
