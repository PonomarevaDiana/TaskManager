package org.example.authapp.repository;

import org.example.authapp.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    Session findByAuthToken(String token);
    Session findByRefreshToken(String token);
    void deleteAllByUserId(String userId);

    List<Session> findByUserId(String userId);
}
