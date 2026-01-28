package com.example.businessLogic.repository;

import com.example.businessLogic.entity.Contact;
import com.example.businessLogic.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Optional<Contact> findByOwnerAndContactUser(User owner, User contactUser);
    List<Contact> findByOwner(User owner);
    List<Contact> findByOwnerAndIsPinnedTrue(User owner);
    boolean existsByOwnerAndContactUser(User owner, User contactUser);
    boolean existsByOwnerIdAndContactUserId(String ownerId, String contactUserId);
    int deleteByOwnerIdAndContactUserId(String ownerId, String contactUserId);
    Optional<Contact> findByIdAndOwnerId(Long id, String ownerId);
}