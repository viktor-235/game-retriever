package com.github.viktor235.gameretriever.command;

import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.exception.AuthException;
import com.github.viktor235.gameretriever.model.converter.Converter;
import com.github.viktor235.gameretriever.model.entity.Platform;
import com.github.viktor235.gameretriever.service.AuthService;
import com.github.viktor235.gameretriever.service.ConverterService;
import com.github.viktor235.gameretriever.service.GameGrabberService;
import com.github.viktor235.gameretriever.service.LiquibaseService;
import com.github.viktor235.gameretriever.shell.ShellHelper;
import com.github.viktor235.gameretriever.shell.ShellSpinner;
import liquibase.repackaged.org.apache.commons.lang3.BooleanUtils;
import lombok.RequiredArgsConstructor;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static liquibase.repackaged.org.apache.commons.collections4.MapUtils.isEmpty;
import static liquibase.repackaged.org.apache.commons.lang3.StringUtils.isNotEmpty;

@ShellComponent
@RequiredArgsConstructor
public class GameRetrieverCommands {

    private final AuthService authService;
    private final GameGrabberService gameGrabberService;
    private final LiquibaseService liquibaseService;
    private final ConverterService converterService;
    private final ShellHelper shellHelper;
    private final ShellSpinner spinner;

    @ShellMethod(key = "auth", value = "Log into Twitch developers to access igdb.com API. Credentials saves into 'auth.json'. Details: https://api-docs.igdb.com/#account-creation")
    public void auth(
            @ShellOption(value = {"client-id", "-i"}, defaultValue = ShellOption.NULL)
            String clientId,
            @ShellOption(value = {"client-secret", "-s"}, defaultValue = ShellOption.NULL)
            String clientSecret,
            @ShellOption(value = {"logout", "-l"}, defaultValue = "false", help = "Forget auth data and delete credential file 'auth.json'")
            boolean logout
    ) throws AppException {
        if (logout) {
            authService.logout();
            shellHelper.printSuccess("Credential file 'auth.json' deleted");
            return;
        }

        if (clientId == null) {
            clientId = shellHelper.prompt("Enter client-id:");
        }
        if (clientSecret == null) {
            clientSecret = shellHelper.promptPassword("Enter client-secret:");
        }

        if (clientId == null || clientSecret == null) {
            shellHelper.printWarning("Empty input. Authorizing canceled");
            return;
        }

        try {
            spinner.start("Logging into Twitch developers");
            authService.auth(clientId, clientSecret);
            spinner.stop(shellHelper.getSuccess("Successfully authorized"));
        } catch (AuthException e) {
            spinner.stop(shellHelper.getError(e.getMessage()));
            auth(null, null, false);
        } finally {
            spinner.stopIfSpinning();
        }
    }

    @ShellMethod(key = "platforms update", value = "Grab platforms data from IGDB into local")
    public void grabPlatforms() throws AppException {
        Boolean confirm = shellHelper.confirm("Update platforms from IGDB?");
        if (BooleanUtils.isNotTrue(confirm)) {
            shellHelper.printInfo("Platform updating skipped");
            shellHelper.println();
            return;
        }

        try {
            spinner.start("Updating the platforms");
            gameGrabberService.grabPlatforms();
            spinner.stop(shellHelper.getSuccess("Platforms updated")); //FIXME repeats because of recursion
        } catch (AuthException e) {
            spinner.stop(shellHelper.getWarning("Unauthorized. Logging in:"));
            auth(null, null, false);
            grabPlatforms();
        } finally {
            spinner.stopIfSpinning();
            shellHelper.println();
        }
    }

    @ShellMethod(key = "platforms ls", value = "Show platform list")
    public void showPlatforms(
            @ShellOption(value = {"active-only", "-a"}, help = "Show active platforms only")
            boolean activeOnly
    ) throws AppException {
        List<Platform> platforms = gameGrabberService.getPlatforms(activeOnly);

        StringBuilder sb = new StringBuilder();
        sb.append("""
                Format: [activation status] [platform id]. [platform name] ([short name])
                Platform count: %s%n
                """.formatted(platforms.size()));

        for (Platform p : platforms) {
            sb.append("%s %s %s".formatted(
                    shellHelper.getCheckbox(p.getActive()),
                    shellHelper.getColored(p.getId() + ".", AttributedStyle.BRIGHT),
                    p.getName()
            ));
            if (isNotEmpty(p.getShortName())) {
                sb.append(shellHelper.getColored(
                        " (%s)".formatted(p.getShortName()),
                        AttributedStyle.BRIGHT
                ));
            }

            sb.append(System.lineSeparator());
        }
        shellHelper.println(sb.toString());
    }

    @ShellMethod(key = "platforms manage", value = "Manage active platforms")
    public void managePlatforms() throws AppException {
        List<Platform> platforms;
        try {
            spinner.start("Preparing platform list");
            platforms = gameGrabberService.getPlatforms(false);
            spinner.stop();
        } finally {
            spinner.stopIfSpinning();
        }

        List<Platform> chosenPlatforms = shellHelper.chooseMany("Select platforms to use", platforms, Platform::getName, Platform::getActive);

        try {
            spinner.start("Saving the selection");
            gameGrabberService.setActivePlatforms(chosenPlatforms.stream()
                    .map(Platform::getId)
                    .collect(Collectors.toSet())
            );
            spinner.stop();
        } finally {
            spinner.stopIfSpinning();
        }

        String activePlatforms = gameGrabberService.getPlatforms(true).stream()
                .map(Platform::getShortName)
                .collect(Collectors.joining(", "));
        shellHelper.printSuccess("Selected platforms saved: " + activePlatforms);
        shellHelper.println();
    }

    @ShellMethod(key = "games update", value = "Grab games from activated platforms info into local DB. See 'platform manage'")
    public void grabGames() throws AppException {
        Boolean confirm = shellHelper.confirm("Update games from IGDB?");
        if (BooleanUtils.isNotTrue(confirm)) {
            shellHelper.printInfo("Game updating skipped");
            shellHelper.println();
            return;
        }

        try {
            spinner.start("Updating the games");
            gameGrabberService.grabGames(spinner::setMessage);
            String activePlatforms = gameGrabberService.getPlatforms(true).stream()
                    .map(Platform::getShortName)
                    .collect(Collectors.joining(", "));
            spinner.stop(shellHelper.getSuccess("Games updated for platforms: " + activePlatforms)); //FIXME repeats because of recursion
        } catch (AuthException e) {
            spinner.stop(shellHelper.getWarning("Unauthorized. Logging in:"));
            auth(null, null, false);
            grabGames();
        } finally {
            spinner.stopIfSpinning();
            shellHelper.println();
        }
    }

    @ShellMethod(key = "output changelog", value = "Store all platforms and games as SQL insert file")
    public void generateChangelog() throws AppException {
        try {
            String changelogFile = liquibaseService.getChangelogFile();
            spinner.start("Generating changelog file: " + changelogFile);
            liquibaseService.generateDataChangelog();
            spinner.stop(shellHelper.getSuccess("DB changelog generated: " + changelogFile));
        } finally {
            spinner.stopIfSpinning();
            shellHelper.println();
        }
    }

    @ShellMethod(key = "output convert", value = "Convert changelog SQL file into result file. Converters located in 'converters/'")
    public void convertSql() throws AppException {
        Map<String, Converter> converters = converterService.getConverters();
        if (isEmpty(converters)) {
            shellHelper.printWarning("No converters found in the folder 'converters/'. Converting skipped");
            return;
        }

        String converterName = shellHelper.chooseOne("Select SQL converter (located in 'converters/')", converters.keySet());
        Converter converterCfg = converters.get(converterName);

        try {
            spinner.start("Converting changelog file '%s' to the result file '%s'"
                    .formatted(converterCfg.getInputFile(), converterCfg.getOutputFile()));
            converterService.convert(converterName, spinner::setMessage);
            spinner.stop(shellHelper.getSuccess("Changelog converted. Result file: " + converterCfg.getOutputFile()));
        } finally {
            spinner.stopIfSpinning();
            shellHelper.println();
        }
    }

    @ShellMethod(key = "wizard", value = "Start interactive wizard. This is the easiest way to interact with the application")
    public void wizard() throws AppException {
        grabPlatforms();
        managePlatforms();
        grabGames();
        generateChangelog();
        convertSql();
    }
}
