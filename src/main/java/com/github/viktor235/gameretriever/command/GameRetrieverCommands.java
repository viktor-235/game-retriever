package com.github.viktor235.gameretriever.command;

import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.exception.AuthException;
import com.github.viktor235.gameretriever.helper.Spinner;
import com.github.viktor235.gameretriever.model.converter.Converter;
import com.github.viktor235.gameretriever.model.entity.Platform;
import com.github.viktor235.gameretriever.service.AuthService;
import com.github.viktor235.gameretriever.service.ConverterService;
import com.github.viktor235.gameretriever.service.GameGrabberService;
import com.github.viktor235.gameretriever.service.LiquibaseService;
import com.github.viktor235.gameretriever.shell.ShellHelper;
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

        try (Spinner spinner = shellHelper.spinner("Logging into Twitch developers")) {
            authService.auth(clientId, clientSecret);
            spinner.success("Successfully authorized");
        } catch (AuthException e) {
            shellHelper.printError(e.getMessage());
            auth(null, null, false);
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

        try (Spinner spinner = shellHelper.spinner("Updating the platforms")) {
            gameGrabberService.grabPlatforms();
            spinner.success("Platforms updated");
        } catch (AuthException e) {
            shellHelper.printWarning("Unauthorized. Logging in:");
            auth(null, null, false);
            grabPlatforms();
            return;
        }
        shellHelper.println();
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
        try (Spinner spinner = shellHelper.spinner("Preparing platform list")) {
            platforms = gameGrabberService.getPlatforms(false);
        }

        List<Platform> chosenPlatforms = shellHelper.chooseMany("Select platforms to use", platforms, Platform::getName, Platform::getActive);

        try (Spinner spinner = shellHelper.spinner("Saving the selection")) {
            gameGrabberService.setActivePlatforms(chosenPlatforms.stream()
                    .map(Platform::getId)
                    .collect(Collectors.toSet())
            );
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

        try (Spinner spinner = shellHelper.spinner("Updating the games")) {
            gameGrabberService.grabGames(spinner::setMessage);
            String activePlatforms = gameGrabberService.getPlatforms(true).stream()
                    .map(Platform::getShortName)
                    .collect(Collectors.joining(", "));
            spinner.success("Games updated for platforms: " + activePlatforms);
        } catch (AuthException e) {
            shellHelper.printWarning("Unauthorized. Logging in:");
            auth(null, null, false);
            grabGames();
            return;
        }
        shellHelper.println();
    }

    @ShellMethod(key = "output changelog", value = "Store all platforms and games as SQL insert file")
    public void generateChangelog() throws AppException {
        String changelogFile = liquibaseService.getChangelogFile();
        try (Spinner spinner = shellHelper.spinner("Generating changelog file: " + changelogFile)) {
            liquibaseService.generateDataChangelog();
            spinner.success("DB changelog generated: " + changelogFile);
        }
        shellHelper.println();
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

        try (Spinner spinner = shellHelper.spinner("Converting: '%s' -> '%s'"
                .formatted(converterCfg.getInputFile(), converterCfg.getOutputFile()))) {
            converterService.convert(converterName, spinner::setMessage);
            spinner.success("Changelog converted. Result file: " + converterCfg.getOutputFile());
        }
        shellHelper.println();
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
