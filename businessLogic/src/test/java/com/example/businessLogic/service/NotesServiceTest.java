package com.example.businessLogic.service;

import com.example.businessLogic.entity.Note;
import com.example.businessLogic.repository.NotesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotesServiceTest {

    @Mock
    private NotesRepository notesRepository;

    @InjectMocks
    private NotesService notesService;

    @Test
    void getNotesByObjectId_WhenIsProjectTrue_ShouldReturnProjectNotes() {
        String objectId = "project-123";
        Boolean isProject = true;

        Note note1 = new Note();
        note1.setId(1L);
        note1.setObjectId(objectId);

        Note note2 = new Note();
        note2.setId(2L);
        note2.setObjectId(objectId);

        List<Note> expectedNotes = Arrays.asList(note1, note2);

        when(notesRepository.findByObjectIdAndIsProjectTrue(objectId)).thenReturn(expectedNotes);

        List<Note> result = notesService.getNotesByObjectId(objectId, isProject);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedNotes, result);
        verify(notesRepository).findByObjectIdAndIsProjectTrue(objectId);
        verify(notesRepository, never()).findByObjectIdAndIsProjectFalse(anyString());
    }

    @Test
    void getNotesByObjectId_WhenIsProjectFalse_ShouldReturnNonProjectNotes() {
        String objectId = "task-123";
        Boolean isProject = false;

        Note note1 = new Note();
        note1.setId(1L);
        note1.setObjectId(objectId);

        Note note2 = new Note();
        note2.setId(2L);
        note2.setObjectId(objectId);

        List<Note> expectedNotes = Arrays.asList(note1, note2);

        when(notesRepository.findByObjectIdAndIsProjectFalse(objectId)).thenReturn(expectedNotes);

        List<Note> result = notesService.getNotesByObjectId(objectId, isProject);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedNotes, result);
        verify(notesRepository).findByObjectIdAndIsProjectFalse(objectId);
        verify(notesRepository, never()).findByObjectIdAndIsProjectTrue(anyString());
    }

    @Test
    void getNotesByObjectId_WhenIsProjectNull_ShouldReturnNonProjectNotes() {
        String objectId = "task-123";
        Boolean isProject = null;

        Note note1 = new Note();
        note1.setId(1L);
        note1.setObjectId(objectId);

        List<Note> expectedNotes = Arrays.asList(note1);

        when(notesRepository.findByObjectIdAndIsProjectFalse(objectId)).thenReturn(expectedNotes);

        List<Note> result = notesService.getNotesByObjectId(objectId, isProject);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notesRepository).findByObjectIdAndIsProjectFalse(objectId);
        verify(notesRepository, never()).findByObjectIdAndIsProjectTrue(anyString());
    }

    @Test
    void getNotesByObjectId_WhenNoNotesFound_ShouldReturnEmptyList() {
        String objectId = "non-existent-id";
        Boolean isProject = true;

        List<Note> expectedNotes = List.of();

        when(notesRepository.findByObjectIdAndIsProjectTrue(objectId)).thenReturn(expectedNotes);

        List<Note> result = notesService.getNotesByObjectId(objectId, isProject);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(notesRepository).findByObjectIdAndIsProjectTrue(objectId);
    }

    @Test
    void createNote_WithValidNote_ShouldSaveAndReturnNote() {
        Note noteToSave = new Note();
        noteToSave.setContent("Test note content");
        noteToSave.setObjectId("object-123");
        noteToSave.setIsProject(true);

        Note savedNote = new Note();
        savedNote.setId(1L);
        savedNote.setContent("Test note content");
        savedNote.setObjectId("object-123");
        savedNote.setIsProject(true);

        when(notesRepository.save(noteToSave)).thenReturn(savedNote);

        Note result = notesService.createNote(noteToSave);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test note content", result.getContent());
        verify(notesRepository).save(noteToSave);
    }

    @Test
    void createNote_WithMinimalData_ShouldSaveSuccessfully() {
        Note minimalNote = new Note();
        minimalNote.setContent("Minimal note");

        Note savedNote = new Note();
        savedNote.setId(1L);
        savedNote.setContent("Minimal note");

        when(notesRepository.save(minimalNote)).thenReturn(savedNote);

        Note result = notesService.createNote(minimalNote);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(notesRepository).save(minimalNote);
    }

    @Test
    void getNotesByObjectId_WithEmptyObjectId_ShouldHandleGracefully() {
        String emptyObjectId = "";
        Boolean isProject = false;

        List<Note> expectedNotes = List.of();

        when(notesRepository.findByObjectIdAndIsProjectFalse(emptyObjectId)).thenReturn(expectedNotes);

        List<Note> result = notesService.getNotesByObjectId(emptyObjectId, isProject);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(notesRepository).findByObjectIdAndIsProjectFalse(emptyObjectId);
    }

    @Test
    void getNotesByObjectId_WithNullObjectId_ShouldHandleGracefully() {
        String nullObjectId = null;
        Boolean isProject = true;

        List<Note> expectedNotes = List.of();

        when(notesRepository.findByObjectIdAndIsProjectTrue(nullObjectId)).thenReturn(expectedNotes);

        List<Note> result = notesService.getNotesByObjectId(nullObjectId, isProject);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(notesRepository).findByObjectIdAndIsProjectTrue(nullObjectId);
    }

    @Test
    void createNote_ShouldReturnNoteWithGeneratedId() {
        Note noteWithoutId = new Note();
        noteWithoutId.setContent("Note without ID");

        Note noteWithId = new Note();
        noteWithId.setId(100L);
        noteWithId.setContent("Note without ID");

        when(notesRepository.save(noteWithoutId)).thenReturn(noteWithId);

        Note result = notesService.createNote(noteWithoutId);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Note without ID", result.getContent());
    }
}