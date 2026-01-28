package com.example.businessLogic.repository;
import com.example.businessLogic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByFriendshipKeyAndUsername(String friendshipKey, String name);
    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);
}
