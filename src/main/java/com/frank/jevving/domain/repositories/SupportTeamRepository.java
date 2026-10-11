package com.frank.jevving.domain.repositories;

import com.frank.jevving.domain.entities.SupportTeam;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface SupportTeamRepository {

    Uni<List<SupportTeam>> findAll();

    /** Emits {@code null} when no team has the given id. */
    default Uni<SupportTeam> findById(String id) {
        return findAll().map(teams -> teams.stream()
                .filter(team -> team.id().equals(id))
                .findFirst()
                .orElse(null));
    }
}
