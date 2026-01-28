package com.example.businessLogic.service;

import com.example.businessLogic.entity.User;
import com.example.businessLogic.entity.Contact;
import com.example.businessLogic.repository.ContactRepository;
import com.example.businessLogic.dto.ContactResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private static final SecureRandom random = new SecureRandom();
    private final ContactRepository contactRepository;

    public String getNewSecretFriendshipKey() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int index = random.nextInt(characters.length());
            key.append(characters.charAt(index));
        }
        String result = key.toString();
        log.debug("Сгенерирован новый секретный ключ дружбы: {}", result);
        return result;
    }

    public List<ContactResponse> getUserContacts(User user) {
        List<Contact> contacts = contactRepository.findByOwner(user);

        return contacts.stream()
                .map(this::convertToContactResponse)
                .toList();
    }

    public boolean contactExists(User owner, User contactUser) {
        return contactRepository.existsByOwnerAndContactUser(owner, contactUser);
    }

    @Transactional
    public Contact createContact(User owner, User contactUser) {
        Contact contact = new Contact();
        contact.setOwner(owner);
        contact.setContactUser(contactUser);
        contact.setIsPinned(false);

        Contact savedContact = contactRepository.save(contact);
        log.info("Создан новый контакт: owner={}, contactUser={}",
                owner.getId(), contactUser.getId());

        return savedContact;
    }

    @Transactional
    public boolean deleteContact(Long contactId) {
        Optional<Contact> contact = contactRepository.findById(contactId);

        if (contact.isPresent()) {
            User user1 = contact.get().getOwner();
            User user2 = contact.get().getContactUser();

            int deleted = contactRepository.deleteByOwnerIdAndContactUserId(user1.getId(), user2.getId()) +
                    contactRepository.deleteByOwnerIdAndContactUserId(user2.getId(), user1.getId());

            return deleted == 2;
        }

        return false;
    }

    public Contact getContactByOwnerAndContactUser(User owner, User contactUser) {
        return contactRepository.findByOwnerAndContactUser(owner, contactUser)
                .orElse(null);
    }

    @Transactional
    public Contact updateContactPinStatus(Long contactId, Boolean isPinned, String ownerId) {
        Contact contact = contactRepository.findByIdAndOwnerId(contactId, ownerId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        contact.setIsPinned(isPinned);
        return contactRepository.save(contact);
    }

    private ContactResponse convertToContactResponse(Contact contact) {
        User contactUser = contact.getContactUser();

        return ContactResponse.builder()
                .contactId(contact.getId())
                .contactUserId(contactUser.getId())
                .contactUserName(contactUser.getUsername())
                .isPinned(contact.getIsPinned())
                .build();
    }
}