package com.github.viktor235.gameretriever.service;

import com.api.igdb.request.TwitchAuthenticator;
import com.api.igdb.utils.TwitchToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.exception.AuthException;
import com.github.viktor235.gameretriever.model.AuthData;
import com.github.viktor235.gameretriever.shell.ShellHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final IgdbService igdbService;
    private final ShellHelper shellHelper;
    private final ObjectMapper objectMapper;

    private static final String AUTH_FILE_PATH = "auth.json";

    private AuthData authData;

    @PostConstruct
    private void init() throws AppException {
        try {
            readFile();
        } catch (AppException e) {
            shellHelper.printError(e.getMessage());
        }
    }

    public void auth(String clientId, String clientSecret) throws AppException {
        logout();

        TwitchAuthenticator tAuth = TwitchAuthenticator.INSTANCE;
        TwitchToken token = tAuth.requestTwitchToken(clientId, clientSecret);

        if (token == null)
            throw new AuthException("Error while requesting Twitch token. Try again");

        authData = new AuthData(clientId, token.getAccess_token());
        igdbService.setCredentials(authData.clientId(), authData.accessToken());
        saveFile();
    }

    public void logout() {
        deleteFile();
        igdbService.setCredentials("", "");
    }

    private void readFile() throws AppException {
        try {
            File file = new File(AUTH_FILE_PATH);
            if (file.exists()) {
                authData = objectMapper.readValue(file, AuthData.class);
                igdbService.setCredentials(authData.clientId(), authData.accessToken());
            } else {
                authData = null;
                igdbService.setCredentials("", "");
            }
        } catch (IOException e) {
            throw new AppException("Error while reading auth data file: " + e.getMessage(), e);
        }
    }

    private void saveFile() throws AppException {
        if (authData.accessToken() == null)
            throw new AppException("No tokens to write to the auth data file");

        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(Paths.get(AUTH_FILE_PATH).toFile(), authData);
        } catch (IOException e) {
            throw new AppException("Error while auth data file saving: " + e.getMessage(), e);
        }
    }

    private void deleteFile() throws AppException {
        try {
            java.nio.file.Files.deleteIfExists(Path.of(AUTH_FILE_PATH));
        } catch (IOException e) {
            throw new AppException("Error while deleting auth data file: " + e.getMessage(), e);
        } finally {
            authData = null;
        }
    }
}
