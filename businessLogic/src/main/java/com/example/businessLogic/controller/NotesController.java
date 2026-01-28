package com.example.businessLogic.controller;

import com.example.businessLogic.entity.Note;
import com.example.businessLogic.service.NotesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NotesController {
    private final NotesService notesService;

    @GetMapping("/{objectId}")
    public ResponseEntity<List<Note>> getNotesByProject(@PathVariable String objectId,
                                                        @RequestParam Boolean isProject) {
        return ResponseEntity.ok(notesService.getNotesByObjectId(objectId, isProject));
    }

    @PostMapping("/{objectId}")
    public ResponseEntity<Note> createNoteForObject(@PathVariable String objectId, @RequestBody Note note) {
        note.setObjectId(objectId);

        return ResponseEntity.ok(notesService.createNote(note));
    }
}