package com.github.viktor235.gameretriever.service;

import com.github.viktor235.gameretriever.exception.AppException;
import com.github.viktor235.gameretriever.model.entity.Game;
import com.github.viktor235.gameretriever.model.entity.GamePlatform;
import com.github.viktor235.gameretriever.model.entity.Platform;
import com.github.viktor235.gameretriever.repository.GamePlatformRepository;
import com.github.viktor235.gameretriever.repository.GameRepository;
import com.github.viktor235.gameretriever.repository.PlatformRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@Service
@RequiredArgsConstructor
public class GameGrabberService {

    private final IgdbService igdbService;
    private final PlatformRepository platformRepository;
    private final GameRepository gameRepository;
    private final GamePlatformRepository gamePlatformRepository;

    @Transactional
    public void grabPlatforms() throws AppException {
        igdbService.getPlatforms((buffer) -> {
            for (proto.Platform apiPlatform : buffer) {
                boolean active = platformRepository.findById(apiPlatform.getId())
                        .map(Platform::getActive)
                        .orElse(false);

                Platform dbPlatform = Platform.builder()
                        .id(apiPlatform.getId())
                        .name(apiPlatform.getName())
                        .shortName(apiPlatform.getAbbreviation())
                        .active(active).build();
                platformRepository.save(dbPlatform);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Platform> getPlatforms(boolean activeOnly) {
        return activeOnly ?
                platformRepository.findByActiveTrue() :
                platformRepository.findAll();
    }

    @Transactional
    public void setPlatformsStatus(long[] platformIds, boolean newActiveStatus) throws AppException {
        for (long id : platformIds) {
            Platform dbPlatform = platformRepository.findById(id)
                    .orElseThrow(() -> new AppException("Platform with id '%d' not found".formatted(id)));

            dbPlatform.setActive(newActiveStatus);
            platformRepository.save(dbPlatform);
            System.out.printf("'%s' activation status is '%s'%n", dbPlatform.getName(), newActiveStatus);
        }
    }

    @Transactional
    public void setActivePlatforms(Set<Long> platformIds) throws AppException {
        platformRepository.findAll()
                .forEach(p -> p.setActive(platformIds.contains(p.getId())));
    }

    @Transactional
    public void grabGames(Consumer<String> progressCallback) throws AppException {
        progressCallback.accept("Erasing saved games");
        eraseTable(gamePlatformRepository);
        eraseTable(gameRepository);

        List<Platform> activePlatforms = platformRepository.findByActiveTrue();
        if (activePlatforms.isEmpty()) {
            progressCallback.accept("No active platforms");
            return;
        }

        int platformIndex = 0;
        for (Platform dbPlatform : activePlatforms) {
            platformIndex++;

            AtomicInteger handled = new AtomicInteger();
            int finalPlatformIndex = platformIndex;
            igdbService.getGames(dbPlatform.getId(), (buffer) -> {
                for (proto.Game apiGame : buffer) {
                    Game dbGame = Game.builder()
                            .id(apiGame.getId())
                            .name(apiGame.getName())
                            .infoLink(apiGame.getUrl()).build();
                    gameRepository.save(dbGame);

                    gamePlatformRepository.save(GamePlatform.builder()
                            .game(dbGame)
                            .platform(dbPlatform).build()
                    );
                }

                progressCallback.accept("(platform %d/%d) %s: handled %d games".formatted(finalPlatformIndex,
                        activePlatforms.size(), dbPlatform.getName(), handled.addAndGet(buffer.size())));
            });
        }
    }

    @Deprecated
    private <T> void eraseTable(JpaRepository<T, ?> repository) {
        //TODO replace with truncate (but some issue with H2).
//        gamePlatformRepository.truncateTable();
//        .truncateTable();

        repository.findAll()
                .forEach(repository::delete);
    }
}
