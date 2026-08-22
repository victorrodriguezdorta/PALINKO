package com.kawser.cleanspringbootproject.user.application.port.out;

import com.kawser.cleanspringbootproject.user.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User save(User user);
}
