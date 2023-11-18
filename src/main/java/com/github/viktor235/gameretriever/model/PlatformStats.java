package com.github.viktor235.gameretriever.model;

import lombok.Builder;

import java.util.List;

@Builder
public record PlatformStats(
        long activePlatformCount,
        long gameCount,
        long gamePlatformCount,
        List<Platform> platformStats
) {

    public record Platform(
            String name,
            long gameCount
    ) {
    }
}
