package com.example.businessLogic.repository;

import com.example.businessLogic.entity.ContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {

    List<ContactRequest> findByRecipient(String recipient);

    List<ContactRequest> findByOwner(String owner);

    boolean existsByOwnerAndRecipient(String owner, String recipient);

    void deleteByOwnerAndRecipient(String owner, String recipient);
}