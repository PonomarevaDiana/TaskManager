package org.example.authapp.repository;

import org.example.authapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository  extends JpaRepository<User, String> {
    User findByUsername(String username);
    Optional<User> findById(String id);
    User findByVerificationCode(String code);
    User findByEmail(String email);
}
