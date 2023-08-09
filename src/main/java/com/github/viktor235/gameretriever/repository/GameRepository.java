package com.github.viktor235.gameretriever.repository;

import com.github.viktor235.gameretriever.model.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    @Modifying
    @Query(
            value = "TRUNCATE TABLE GAME_RETRIEVER.GAME RESTART IDENTITY",
            nativeQuery = true
    )
    void truncateTable();
}
