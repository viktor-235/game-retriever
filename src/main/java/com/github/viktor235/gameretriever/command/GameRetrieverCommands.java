package com.github.viktor235.gameretriever.command;

import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.exception.AuthException;
import com.github.viktor235.gameretriever.model.converter.Converter;
import com.github.viktor235.gameretriever.model.entity.Platform;
import com.github.viktor235.gameretriever.service.AuthService;
import com.github.viktor235.gameretriever.service.ConverterService;
import com.github.viktor235.gameretriever.service.GameGrabberService;
import com.github.viktor235.gameretriever.service.LiquibaseService;
import com.github.viktor235.gameretriever.service.helper.FormatHelper;
import com.github.viktor235.gameretriever.shell.ShellHelper;
import com.github.viktor235.gameretriever.shell.Spinner;
import liquibase.repackaged.org.apache.commons.lang3.BooleanUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static liquibase.repackaged.org.apache.commons.collections4.MapUtils.isEmpty;

@ShellComponent
@RequiredArgsConstructor
public class GameRetrieverCommands {

    private final AuthService authService;
    private final GameGrabberService gameGrabberService;
    private final LiquibaseService liquibaseService;
    private final ConverterService converterService;
    private final ShellHelper shellHelper;
    private final FormatHelper formatHelper;

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

        clientId = shellHelper.promptIfEmpty(clientId, "Enter client-id:");
        clientSecret = shellHelper.promptPasswordIfEmpty(clientSecret, "Enter client-secret:");
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
            shellHelper.printInfo("Platform updating skipped\n");
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
        shellHelper.println(formatHelper.getPlatformList(platforms, activeOnly));
    }

    @ShellMethod(key = "games update", value = "Grab games from selected platforms into local DB")
    public void grabGames() throws AppException {
        List<Platform> platforms = shellHelper.withSpinner("Preparing platform list",
                () -> gameGrabberService.getPlatforms(false));

        showPlatforms(true);

        List<Platform> chosenPlatforms = shellHelper.chooseMany("Select platforms to update games", platforms, Platform::getName, Platform::getActive);
        shellHelper.withSpinner("Saving the selection",
                () -> gameGrabberService.setActivePlatforms(chosenPlatforms.stream()
                        .map(Platform::getId)
                        .collect(Collectors.toSet())));

        try (Spinner spinner = shellHelper.spinner("Updating the games")) {
            gameGrabberService.grabGames(spinner::setMessage);
        } catch (AuthException e) {
            shellHelper.printWarning("Unauthorized. Logging in:");
            auth(null, null, false);
            grabGames();
            return;
        }

        List<Platform> activePlatforms = gameGrabberService.getPlatforms(true);
        if (activePlatforms.isEmpty()) {
            shellHelper.printWarning("No platforms selected\n");
        } else {
            shellHelper.printSuccess("Games updated for platforms: %s%n".formatted(
                    formatHelper.getPlatformShortNames(activePlatforms)));
        }
    }

    @ShellMethod(key = "output changelog", value = "Store all platforms and games as SQL insert file")
    public void generateChangelog() throws AppException {
        String changelogFile = liquibaseService.getChangelogFile();
        shellHelper.withSpinner("Generating changelog file: " + changelogFile,
                liquibaseService::generateDataChangelog,
                "DB changelog generated: " + changelogFile);
        shellHelper.println(formatHelper.getPlatformStats(gameGrabberService.getStats()));
    }

    @ShellMethod(key = "output convert", value = "Convert changelog SQL file into result file. Converters located in 'converters/'")
    public void convertSql() throws AppException {
        Map<String, Converter> converters = converterService.getConverters();
        if (isEmpty(converters)) {
            shellHelper.printWarning("No converters found in the folder 'converters/'. Converting skipped");
            return;
        }

        converters.put("[don't convert]", null);
        String converterName = shellHelper.chooseOne("Select SQL converter (located in 'converters/')", converters.keySet());
        Converter converterCfg = converters.get(converterName);
        if (converterCfg == null) {
            shellHelper.printWarning("Conversion canceled\n");
            return;
        }

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
        grabGames();
        generateChangelog();
        convertSql();
    }
}
