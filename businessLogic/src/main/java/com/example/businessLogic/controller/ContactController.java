package com.example.businessLogic.controller;

import com.example.businessLogic.entity.ContactRequest;
import com.example.businessLogic.service.ContactRequestService;
import com.example.businessLogic.entity.User;
import com.example.businessLogic.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.businessLogic.service.UserService;
import com.example.businessLogic.dto.ContactResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contact")
@RequiredArgsConstructor
public class ContactController {

    private final UserService userService;
    private final ContactService contactService;
    private final ContactRequestService contactRequestService;

    @GetMapping("/getMyFriendshipKey")
    public ResponseEntity<String> getFriendshipKeyByUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.findByUserId(authentication.getPrincipal().toString());

        return ResponseEntity.ok(user.getFriendshipKey());
    }

    @GetMapping("/getMyContactList")
    public ResponseEntity<List<ContactResponse>> getMyContactList(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.findByUserId(authentication.getPrincipal().toString());

        try {
            List<ContactResponse> contacts = contactService.getUserContacts(user);
            return ResponseEntity.ok(contacts);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/sendRequest")
    public ResponseEntity<String> sendRequest(@RequestBody Map<String, String> request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        User currentUser = userService.findByUserId(authentication.getPrincipal().toString());

        String key = request.get("key");
        if (key == null || key.isEmpty()) {
            return ResponseEntity.badRequest().body("Key is required");
        }

        String[] keyParts = key.split("@");
        if (keyParts.length != 2) {
            return ResponseEntity.badRequest().body("Invalid key format. Expected format: friendshipKey@username");
        }

        String friendshipKey = keyParts[0];
        String targetUsername = keyParts[1];

        // Ищем пользователя по коду дружбы и имени
        User targetUser = userService.findByFriendshipKeyAndUsername(friendshipKey, targetUsername);
        if (targetUser == null) {
            return ResponseEntity.status(404).body("User not found with provided key and username");
        }

        // Проверяем, не пытаемся ли добавить самого себя
        if (targetUser.equals(currentUser)) {
            return ResponseEntity.badRequest().body("Cannot send contact request to yourself");
        }

        // Проверяем, не существует ли уже контакт
        if (contactService.contactExists(currentUser, targetUser)) {
            return ResponseEntity.badRequest().body("Contact already exists");
        }

        // Создаем запрос на добавление в контакты
        contactRequestService.createContactRequest(currentUser.getId(), targetUser.getId());

        return ResponseEntity.ok().body("Contact request sent successfully");
    }

    @PostMapping("/acceptRequest/{targetUserId}")
    public ResponseEntity<String> acceptRequest(@PathVariable String targetUserId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        // Удаляем запрос и создаем уведомление для отправителя
        contactRequestService.acceptRequestAndNotify(targetUserId, userId);

        User currentUser = userService.findByUserId(userId);
        User targetUser = userService.findByUserId(targetUserId);

        contactService.createContact(currentUser, targetUser);
        contactService.createContact(targetUser, currentUser);

        return ResponseEntity.ok().body("Contact request accepted");
    }

    @PostMapping("/declineRequest/{targetUserId}")
    public ResponseEntity<String> declineRequest(@PathVariable String targetUserId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        // Удаляем запрос (опционально с уведомлением)
        contactRequestService.declineRequestAndNotify(targetUserId, userId);
        return ResponseEntity.ok().body("Contact request declined");
    }

    @GetMapping("/incomingRequests")
    public ResponseEntity<List<ContactRequest>> getIncomingRequests(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        List<ContactRequest> requests = contactRequestService.getIncomingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/outgoingRequests")
    public ResponseEntity<List<ContactRequest>> getOutgoingRequests(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String userId = userService.findByUserId(authentication.getPrincipal().toString()).getId();

        List<ContactRequest> requests = contactRequestService.getOutgoingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @DeleteMapping("/delete-contact/{contactId}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long contactId) {
        if (contactService.deleteContact(contactId)) {
            return ResponseEntity.status(204).build();
        }

        return ResponseEntity.status(200).build();
    }
}