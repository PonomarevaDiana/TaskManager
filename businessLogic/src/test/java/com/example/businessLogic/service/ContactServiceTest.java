package com.example.businessLogic.service;

import com.example.businessLogic.entity.User;
import com.example.businessLogic.entity.Contact;
import com.example.businessLogic.repository.ContactRepository;
import com.example.businessLogic.dto.ContactResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@Transactional
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void getNewSecretFriendshipKey_ShouldReturnValidKey() {
        String result = contactService.getNewSecretFriendshipKey();

        assertNotNull(result);
        assertEquals(5, result.length());
        assertTrue(result.matches("[A-Za-z0-9]+"));
    }

    @Test
    void getUserContacts_ShouldReturnContactResponses() {
        User owner = new User();
        owner.setId("user1");

        User contactUser1 = new User();
        contactUser1.setId("user2");
        contactUser1.setUsername("User2");

        User contactUser2 = new User();
        contactUser2.setId("user3");
        contactUser2.setUsername("User3");

        Contact contact1 = new Contact();
        contact1.setId(1L);
        contact1.setOwner(owner);
        contact1.setContactUser(contactUser1);
        contact1.setIsPinned(false);

        Contact contact2 = new Contact();
        contact2.setId(2L);
        contact2.setOwner(owner);
        contact2.setContactUser(contactUser2);
        contact2.setIsPinned(true);

        List<Contact> contacts = Arrays.asList(contact1, contact2);

        when(contactRepository.findByOwner(owner)).thenReturn(contacts);

        List<ContactResponse> result = contactService.getUserContacts(owner);

        assertNotNull(result);
        assertEquals(2, result.size());

        ContactResponse response1 = result.get(0);
        assertEquals(1L, response1.getContactId());
        assertEquals("user2", response1.getContactUserId());
        assertEquals("User2", response1.getContactUserName());
        assertFalse(response1.getIsPinned());

        ContactResponse response2 = result.get(1);
        assertEquals(2L, response2.getContactId());
        assertEquals("user3", response2.getContactUserId());
        assertEquals("User3", response2.getContactUserName());
        assertTrue(response2.getIsPinned());

        verify(contactRepository).findByOwner(owner);
    }

    @Test
    void getUserContacts_WhenNoContacts_ShouldReturnEmptyList() {
        User owner = new User();
        owner.setId("user1");

        when(contactRepository.findByOwner(owner)).thenReturn(Arrays.asList());

        List<ContactResponse> result = contactService.getUserContacts(owner);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contactRepository).findByOwner(owner);
    }

    @Test
    void contactExists_WhenContactExists_ShouldReturnTrue() {
        User owner = new User();
        User contactUser = new User();

        when(contactRepository.existsByOwnerAndContactUser(owner, contactUser)).thenReturn(true);

        boolean result = contactService.contactExists(owner, contactUser);

        assertTrue(result);
        verify(contactRepository).existsByOwnerAndContactUser(owner, contactUser);
    }

    @Test
    void contactExists_WhenContactNotExists_ShouldReturnFalse() {
        User owner = new User();
        User contactUser = new User();

        when(contactRepository.existsByOwnerAndContactUser(owner, contactUser)).thenReturn(false);

        boolean result = contactService.contactExists(owner, contactUser);

        assertFalse(result);
        verify(contactRepository).existsByOwnerAndContactUser(owner, contactUser);
    }

    @Test
    void createContact_ShouldCreateAndReturnContact() {
        User owner = new User();
        owner.setId("user1");
        User contactUser = new User();
        contactUser.setId("user2");

        Contact contact = new Contact();
        contact.setOwner(owner);
        contact.setContactUser(contactUser);
        contact.setIsPinned(false);

        when(contactRepository.save(any(Contact.class))).thenReturn(contact);

        Contact result = contactService.createContact(owner, contactUser);

        assertNotNull(result);
        assertEquals(owner, result.getOwner());
        assertEquals(contactUser, result.getContactUser());
        assertFalse(result.getIsPinned());
        verify(contactRepository).save(any(Contact.class));
    }

    @Test
    void deleteContact_ShouldDeleteContact() {
        Long contactId = 1L;

        contactService.deleteContact(contactId);

        verify(contactRepository).deleteById(contactId);
    }

    @Test
    void getContactByOwnerAndContactUser_WhenContactExists_ShouldReturnContact() {
        User owner = new User();
        User contactUser = new User();
        Contact expectedContact = new Contact();

        when(contactRepository.findByOwnerAndContactUser(owner, contactUser))
                .thenReturn(Optional.of(expectedContact));

        Contact result = contactService.getContactByOwnerAndContactUser(owner, contactUser);

        assertEquals(expectedContact, result);
        verify(contactRepository).findByOwnerAndContactUser(owner, contactUser);
    }

    @Test
    void getContactByOwnerAndContactUser_WhenContactNotExists_ShouldReturnNull() {
        User owner = new User();
        User contactUser = new User();

        when(contactRepository.findByOwnerAndContactUser(owner, contactUser))
                .thenReturn(Optional.empty());

        Contact result = contactService.getContactByOwnerAndContactUser(owner, contactUser);

        assertNull(result);
        verify(contactRepository).findByOwnerAndContactUser(owner, contactUser);
    }

    @Test
    void updateContactPinStatus_WhenContactExists_ShouldUpdatePinStatus() {
        Long contactId = 1L;
        String ownerId = "user1";
        Boolean newPinStatus = true;

        Contact existingContact = new Contact();
        existingContact.setId(contactId);
        existingContact.setIsPinned(false);

        Contact updatedContact = new Contact();
        updatedContact.setId(contactId);
        updatedContact.setIsPinned(newPinStatus);

        when(contactRepository.findByIdAndOwnerId(contactId, ownerId))
                .thenReturn(Optional.of(existingContact));
        when(contactRepository.save(existingContact)).thenReturn(updatedContact);

        Contact result = contactService.updateContactPinStatus(contactId, newPinStatus, ownerId);

        assertNotNull(result);
        assertEquals(newPinStatus, result.getIsPinned());
        verify(contactRepository).findByIdAndOwnerId(contactId, ownerId);
        verify(contactRepository).save(existingContact);
    }

    @Test
    void updateContactPinStatus_WhenContactNotExists_ShouldThrowException() {
        Long contactId = 1L;
        String ownerId = "user1";
        Boolean newPinStatus = true;

        when(contactRepository.findByIdAndOwnerId(contactId, ownerId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> contactService.updateContactPinStatus(contactId, newPinStatus, ownerId));
        assertEquals("Contact not found", exception.getMessage());

        verify(contactRepository).findByIdAndOwnerId(contactId, ownerId);
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void updateContactPinStatus_WhenSettingToFalse_ShouldUpdateCorrectly() {
        Long contactId = 1L;
        String ownerId = "user1";
        Boolean newPinStatus = false;

        Contact existingContact = new Contact();
        existingContact.setId(contactId);
        existingContact.setIsPinned(true);

        when(contactRepository.findByIdAndOwnerId(contactId, ownerId))
                .thenReturn(Optional.of(existingContact));
        when(contactRepository.save(existingContact)).thenReturn(existingContact);

        Contact result = contactService.updateContactPinStatus(contactId, newPinStatus, ownerId);

        assertNotNull(result);
        assertEquals(newPinStatus, result.getIsPinned());
        verify(contactRepository).save(existingContact);
    }

    @Test
    void convertToContactResponse_ShouldConvertCorrectly() {
        User owner = new User();
        owner.setId("user1");

        User contactUser = new User();
        contactUser.setId("user2");
        contactUser.setUsername("TestUser");

        Contact contact = new Contact();
        contact.setId(1L);
        contact.setOwner(owner);
        contact.setContactUser(contactUser);
        contact.setIsPinned(true);

        when(contactRepository.findByOwner(owner)).thenReturn(Arrays.asList(contact));

        List<ContactResponse> result = contactService.getUserContacts(owner);

        assertEquals(1, result.size());
        ContactResponse response = result.get(0);
        assertEquals(1L, response.getContactId());
        assertEquals("user2", response.getContactUserId());
        assertEquals("TestUser", response.getContactUserName());
        assertTrue(response.getIsPinned());
    }

    @Test
    void convertToContactResponse_WhenContactUserHasNullUsername_ShouldHandleGracefully() {
        User owner = new User();
        owner.setId("user1");

        User contactUser = new User();
        contactUser.setId("user2");
        contactUser.setUsername(null);

        Contact contact = new Contact();
        contact.setId(1L);
        contact.setOwner(owner);
        contact.setContactUser(contactUser);
        contact.setIsPinned(false);

        when(contactRepository.findByOwner(owner)).thenReturn(Arrays.asList(contact));

        List<ContactResponse> result = contactService.getUserContacts(owner);

        assertEquals(1, result.size());
        ContactResponse response = result.get(0);
        assertEquals("user2", response.getContactUserId());
        assertNull(response.getContactUserName());
        assertFalse(response.getIsPinned());
    }
}