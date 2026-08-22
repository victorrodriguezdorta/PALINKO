package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    List<UserJpaEntity> findByBotTrue();
}
