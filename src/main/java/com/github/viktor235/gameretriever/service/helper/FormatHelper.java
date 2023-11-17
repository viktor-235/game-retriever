package com.github.viktor235.gameretriever.service.helper;

import com.github.viktor235.gameretriever.model.PlatformStats;
import com.github.viktor235.gameretriever.model.entity.Platform;
import com.github.viktor235.gameretriever.shell.ShellHelper;
import lombok.RequiredArgsConstructor;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static liquibase.repackaged.org.apache.commons.lang3.StringUtils.firstNonBlank;
import static liquibase.repackaged.org.apache.commons.lang3.StringUtils.isNotEmpty;

@Controller
@RequiredArgsConstructor
public class FormatHelper {

    private final ShellHelper shellHelper;

    public String getPlatformList(List<Platform> platforms, boolean activeOnly) {
        String header = (activeOnly ? "Active platforms (%d):%n" : "Platforms (%d):%n").formatted(platforms.size());
        if (platforms.isEmpty()) {
            return header + "Nothing to show\n";
        }
        Function<Platform, String> platformAsString = p -> "%s%s %s%s".formatted(
                activeOnly ? "" : shellHelper.getCheckbox(p.getActive()) + " ",
                shellHelper.getColored(p.getId() + ".", AttributedStyle.BRIGHT),
                p.getName(),
                isNotEmpty(p.getShortName()) ?
                        shellHelper.getColored(" (%s)".formatted(p.getShortName()), AttributedStyle.BRIGHT) : "");

        return platforms.stream()
                .map(platformAsString)
                .collect(Collectors.joining("\n", header, "\n"));
    }

    public String getPlatformShortNames(List<Platform> platforms) {
        return platforms.stream()
                .map(p -> firstNonBlank(p.getShortName(), p.getName()))
                .collect(Collectors.joining(", "));
    }

    public String getPlatformStats(PlatformStats stats) {
        String header = "Changelog contains: %d platforms, %d games, %d game-platform relations%n".formatted(
                stats.activePlatformCount(), stats.gameCount(), stats.gamePlatformCount());
        if (stats.gameCount() == 0)
            return header;
        String platformStats = stats.platformStats().stream()
                .map(p -> "- %s: %d games".formatted(p.name(), p.gameCount()))
                .collect(Collectors.joining("\n"));
        return "%sDetails:%n%s%n".formatted(header, platformStats);
    }
}
