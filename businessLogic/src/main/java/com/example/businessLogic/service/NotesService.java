package com.example.businessLogic.service;

import com.example.businessLogic.entity.Note;
import com.example.businessLogic.repository.NotesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotesService {
    private final NotesRepository notesRepository;

    public List<Note> getNotesByObjectId(String objectId, Boolean isProject) {
        if (isProject == null) {
            return notesRepository.findByObjectIdAndIsProjectFalse(objectId);
        } else if (isProject) {
            return notesRepository.findByObjectIdAndIsProjectTrue(objectId);
        } else {
            return notesRepository.findByObjectIdAndIsProjectFalse(objectId);
        }
    }

    public Note createNote(Note note) {
        return notesRepository.save(note);
    }
}