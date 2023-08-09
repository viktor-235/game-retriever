package com.github.viktor235.gameretriever.shell;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.SelectItem;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ShellHelper {

    @Lazy
    @Getter
    private final Terminal terminal;
    private final ComponentFlow.Builder componentFlowBuilder;

    public static final int INFO_COLOR = AttributedStyle.BLUE;
    public static final int SUCCESS_COLOR = AttributedStyle.GREEN;
    public static final int WARNING_COLOR = AttributedStyle.YELLOW;
    public static final int ERROR_COLOR = AttributedStyle.RED;

    public String getColored(String message, int color) {
        if (message == null)
            return null;
        return new AttributedString(message, AttributedStyle.DEFAULT.foreground(color)).toAnsi();
    }

    public String getInfo(String message) {
        return getColored(message, INFO_COLOR);
    }

    public String getSuccess(String message) {
        return getColored(message, SUCCESS_COLOR);
    }

    public String getWarning(String message) {
        return getColored(message, WARNING_COLOR);
    }

    public String getError(String message) {
        return getColored(message, ERROR_COLOR);
    }

    public void println(String message) {
        this.println(message, -1);
    }

    public void printInfo(String message) {
        println(message, INFO_COLOR);
    }

    public void printSuccess(String message) {
        println(message, SUCCESS_COLOR);
    }

    public void printWarning(String message) {
        println(message, WARNING_COLOR);
    }

    public void printError(String message) {
        println(message, ERROR_COLOR);
    }

    public void println(String message, int color) {
        String toPrint = getToPrint(message, color);
        terminal.writer().println(toPrint);
        terminal.flush();
    }

    public void println() {
        terminal.writer().println();
        terminal.flush();
    }

    public void printf(String format, int color, Object... args) {
        String toPrint = getToPrint(format, color);
        terminal.writer().printf(toPrint, args);
        terminal.flush();
    }

    private String getToPrint(String message, int color) {
        if (color > 0) {
            return getColored(message, color);
        } else {
            return message;
        }
    }

    public String getCheckbox(Boolean checked) {
        return getCheckbox(checked != null && checked);
    }

    public String getCheckbox(boolean checked) {
        return getColored(
                "[%s]".formatted(checked ? "X" : " "),
                checked ? AttributedStyle.GREEN : AttributedStyle.BRIGHT);
    }

    /* Input */

    public Boolean confirm(String msg) {
        return confirm(msg, true);
    }

    public Boolean confirm(String msg, boolean defaultValue) {
        AtomicReference<Boolean> confirmed = new AtomicReference<>();

        componentFlowBuilder.clone().reset()
                .withConfirmationInput("withConfirmationInput")
                .name(msg)
                .defaultValue(defaultValue)
                .postHandler(t -> confirmed.set(t.getResultValue()))
                .and().build().run();

        return confirmed.get();
    }

    public String chooseOne(String msg, Collection<String> items) {
        return chooseOne(msg, items, String::toString);
    }

    public <T> T chooseOne(String msg, Collection<T> items, Function<T, String> toStringFunc) {
        Map<String, T> itemMap = items.stream()
                .collect(Collectors.toMap(
                        toStringFunc,
                        Function.identity())
                );

        AtomicReference<T> chosenItem = new AtomicReference<>();

        componentFlowBuilder.clone().reset()
                .withSingleItemSelector("withSingleItemSelector")
                .name(msg)
                .selectItems(items.stream()
                        .collect(Collectors.toMap(
                                toStringFunc,
                                toStringFunc)
                        )
                )
                .postHandler(ctx -> {
                    String itemName = ctx.getResultItem().get().getItem();
                    chosenItem.set(itemMap.get(itemName));
                })
                .and().build().run();

        return chosenItem.get();
    }

    public List<String> chooseMany(String msg, Collection<String> items) {
        return chooseMany(msg, items, String::toString, t -> false);
    }

    public <T> List<T> chooseMany(String msg, Collection<T> items, Function<T, String> toStringFunc, Function<T, Boolean> isCheckedFunc) {
        Map<String, T> itemMap = items.stream()
                .collect(Collectors.toMap(
                        toStringFunc,
                        Function.identity())
                );

        AtomicReference<List<T>> chosenItems = new AtomicReference<>();

        componentFlowBuilder.clone().reset()
                .withMultiItemSelector("withMultiItemSelector")
                .name(msg)
                .selectItems(items.stream()
                        .map(i -> SelectItem.of(toStringFunc.apply(i), toStringFunc.apply(i), true, isCheckedFunc.apply(i)))
                        .collect(Collectors.toList())
                )
                .max(15)
                .postHandler(ctx -> chosenItems.set(
                        ctx.getValues().stream()
                                .map(itemMap::get)
                                .toList())
                )
                .and().build().run();

        return chosenItems.get();
    }


    public String prompt(String msg) {
        return prompt(msg, null, true);
    }

    public String promptPassword(String msg) {
        return prompt(msg, null, false);
    }

    public String prompt(String msg, String defaultValue) {
        return prompt(msg, defaultValue, true);
    }

    public String prompt(String msg, String defaultValue, boolean echo) {
        AtomicReference<String> confirmed = new AtomicReference<>();

        componentFlowBuilder.clone().reset()
                .withStringInput("withStringInput")
                .name(msg)
                .defaultValue(defaultValue)
                .maskCharacter(echo ? null : '*')
                .postHandler(t -> confirmed.set(t.getResultValue()))
                .and().build().run();

        return confirmed.get();
    }
}
