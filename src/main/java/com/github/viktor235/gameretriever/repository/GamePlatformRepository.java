package com.github.viktor235.gameretriever.repository;

import com.github.viktor235.gameretriever.model.entity.GamePlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GamePlatformRepository extends JpaRepository<GamePlatform, Long> {

    List<GamePlatform> findAllByPlatform_Id(long platformId);

    @Modifying
    @Query(
            value = "TRUNCATE TABLE GAME_RETRIEVER.GAME_PLATFORM RESTART IDENTITY",
            nativeQuery = true
    )
    void truncateTable();
}
