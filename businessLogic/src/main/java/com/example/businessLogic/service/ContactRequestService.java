package com.example.businessLogic.service;

import com.example.businessLogic.dto.NotificationRequest;
import com.example.businessLogic.entity.ContactRequest;
import com.example.businessLogic.entity.Notification;
import com.example.businessLogic.entity.NotificationType;
import com.example.businessLogic.repository.ContactRequestRepository;
import com.example.businessLogic.repository.NotificationRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestService {

    private final ContactRequestRepository contactRequestRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public ContactRequest createContactRequest(String owner, String recipient) {
        if (requestExists(owner, recipient)) {
            throw new EntityExistsException("Contact request already exists");
        }

        if (userService.findByUserId(recipient) == null) {
            throw new EntityNotFoundException("Recipient user not found");
        }

        ContactRequest request = new ContactRequest();
        request.setOwner(owner);
        request.setRecipient(recipient);
        request.setCreate_at(String.valueOf(System.currentTimeMillis()));

        ContactRequest savedRequest = contactRequestRepository.save(request);

        createContactRequestNotification(owner, recipient);

        return savedRequest;
    }

    public boolean requestExists(String owner, String recipient) {
        return contactRequestRepository.existsByOwnerAndRecipient(owner, recipient);
    }

    public List<ContactRequest> getIncomingRequests(String recipient) {
        return contactRequestRepository.findByRecipient(recipient);
    }

    public List<ContactRequest> getOutgoingRequests(String owner) {
        return contactRequestRepository.findByOwner(owner);
    }

    @Transactional
    public void deleteRequestById(Long requestId) {
        Optional<ContactRequest> requestOpt = contactRequestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            ContactRequest request = requestOpt.get();
            deleteContactRequestNotification(request.getOwner(), request.getRecipient());
        }
        contactRequestRepository.deleteById(requestId);
    }

    private void createContactRequestNotification(String senderId, String recipientId) {
        try {
            String senderName = getSenderName(senderId);

            NotificationRequest notification = NotificationRequest.builder()
                    .title("Новый запрос в контакты")
                    .message(senderName + " хочет добавить вас в контакты")
                    .senderId(senderId)
                    .type(NotificationType.CONTACT_REQUEST)
                    .build();

            notificationService.sendNotification(recipientId, notification);
        } catch (Exception e) {
            log.error("Failed to create notification: {}", e.getMessage());
        }
    }

    @Transactional
    protected void deleteContactRequestNotification(String senderId, String recipientId) {
        try {
            List<Notification> contactRequestNotifications = notificationRepository
                    .findByOwnerIdAndTypeAndSenderId(recipientId, NotificationType.CONTACT_REQUEST, senderId);

            notificationRepository.deleteAll(contactRequestNotifications);

            log.info("Deleted contact request notifications for recipient: {}, sender: {}", recipientId, senderId);

        } catch (Exception e) {
            log.error("Failed to delete contact request notification: {}", e.getMessage());
        }
    }

    private String getSenderName(String senderId) {
        try {
            var user = userService.findByUserId(senderId);
            if (user != null && user.getUsername() != null) {
                return user.getUsername();
            }
            return "Пользователь";
        } catch (Exception e) {
            return "Пользователь";
        }
    }

    @Transactional
    public void acceptRequestAndNotify(String owner, String recipient) {
        contactRequestRepository.deleteByOwnerAndRecipient(owner, recipient);
        deleteContactRequestNotification(owner, recipient);

        try {
            String recipientName = getSenderName(recipient);

            NotificationRequest notification = NotificationRequest.builder()
                    .title("Запрос в контакты принят")
                    .message(recipientName + " принял ваш запрос в контакты")
                    .senderId(recipient)
                    .type(NotificationType.CONTACT_REQUEST_ACCEPTED)
                    .build();

            notificationService.sendNotification(owner, notification);
        } catch (Exception e) {
            log.error("Failed to create acceptance notification: {}", e.getMessage());
        }
    }

    @Transactional
    public void declineRequestAndNotify(String owner, String recipient) {
        contactRequestRepository.deleteByOwnerAndRecipient(owner, recipient);
        deleteContactRequestNotification(owner, recipient);

        log.info("Contact request declined and notifications cleaned for owner: {}, recipient: {}", owner, recipient);
    }

    public List<Notification> getContactRequestNotifications(String userId) {
        return notificationRepository.findByOwnerIdAndType(userId, NotificationType.CONTACT_REQUEST);
    }
}