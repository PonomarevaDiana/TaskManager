package com.example.businessLogic.repository;

import com.example.businessLogic.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotesRepository extends JpaRepository<Note, Long> {
    List<Note> findByObjectIdAndIsProjectTrue(String objectId);
    List<Note> findByObjectIdAndIsProjectFalse(String objectId);
}