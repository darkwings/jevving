package com.frank.jevving.infrastructure.persistence.repositories;

import com.frank.jevving.domain.entities.SupportTeam;
import com.frank.jevving.domain.repositories.SupportTeamRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class InMemorySupportTeamRepository implements SupportTeamRepository {

    @Override
    public Uni<List<SupportTeam>> findAll() {
        return Uni.createFrom().item(() ->
                List.of(
                        new SupportTeam("devops", "AWS Dev-ops support for infrastructure problems"),
                        new SupportTeam("developers", "Support on software bugs"),
                        new SupportTeam("business", "Business problems, big customer relationship"),
                        new SupportTeam("sales", "Marketing support")
                ));
    }
}
