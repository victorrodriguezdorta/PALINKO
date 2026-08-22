package com.kawser.cleanspringbootproject.user.infrastructure.adapter.out.persistence;

import com.kawser.cleanspringbootproject.user.application.port.out.UserRepository;
import com.kawser.cleanspringbootproject.user.domain.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public JpaUserRepository(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = jpaRepository.save(UserMapper.toEntity(user));
        return UserMapper.toDomain(saved);
    }
}
