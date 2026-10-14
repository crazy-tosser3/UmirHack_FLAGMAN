package com.example.UmirHack_Flagman.common;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public interface IdCount {
    UUID getId();

    long getCnt();

    static Map<UUID, Long> toMap(List<IdCount> rows) {
        return rows.stream().collect(Collectors.toMap(IdCount::getId, IdCount::getCnt));
    }
}
