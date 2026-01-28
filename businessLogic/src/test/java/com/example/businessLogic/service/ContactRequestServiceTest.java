package com.example.businessLogic.service;

import com.example.businessLogic.dto.NotificationRequest;
import com.example.businessLogic.entity.ContactRequest;
import com.example.businessLogic.entity.Notification;
import com.example.businessLogic.entity.User;
import com.example.businessLogic.repository.ContactRequestRepository;
import com.example.businessLogic.repository.NotificationRepository;
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

@ExtendWith(MockitoExtension.class)
class ContactRequestServiceTest {

    @Mock
    private ContactRequestRepository contactRequestRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private ContactRequestService contactRequestService;

    @Test
    void createContactRequest_WithValidData_ShouldCreateRequest() {
        String owner = "user1";
        String recipient = "user2";
        ContactRequest contactRequest = new ContactRequest();
        contactRequest.setOwner(owner);
        contactRequest.setRecipient(recipient);

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(false);
        when(userService.findByUserId(recipient)).thenReturn(new User());
        when(contactRequestRepository.save(any(ContactRequest.class))).thenReturn(contactRequest);

        ContactRequest result = contactRequestService.createContactRequest(owner, recipient);

        assertNotNull(result);
        verify(contactRequestRepository).save(any(ContactRequest.class));
        verify(notificationService).sendNotification(eq(recipient), any(NotificationRequest.class));
    }

    @Test
    void createContactRequest_WhenRequestAlreadyExists_ShouldThrowException() {
        String owner = "user1";
        String recipient = "user2";

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> contactRequestService.createContactRequest(owner, recipient));
        assertEquals("Contact request already exists", exception.getMessage());

        verify(contactRequestRepository, never()).save(any(ContactRequest.class));
        verify(notificationService, never()).sendNotification(anyString(), any(NotificationRequest.class));
    }

    @Test
    void createContactRequest_WhenRecipientNotFound_ShouldThrowException() {
        String owner = "user1";
        String recipient = "non-existent-user";

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(false);
        when(userService.findByUserId(recipient)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> contactRequestService.createContactRequest(owner, recipient));
        assertEquals("Recipient user not found", exception.getMessage());

        verify(contactRequestRepository, never()).save(any(ContactRequest.class));
        verify(notificationService, never()).sendNotification(anyString(), any(NotificationRequest.class));
    }

    @Test
    void createContactRequest_WhenNotificationFails_ShouldStillReturnRequest() {
        String owner = "user1";
        String recipient = "user2";
        ContactRequest contactRequest = new ContactRequest();
        contactRequest.setOwner(owner);
        contactRequest.setRecipient(recipient);

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(false);
        when(userService.findByUserId(recipient)).thenReturn(new User());
        when(contactRequestRepository.save(any(ContactRequest.class))).thenReturn(contactRequest);
        doThrow(new RuntimeException("Notification service unavailable"))
                .when(notificationService).sendNotification(anyString(), any(NotificationRequest.class));

        ContactRequest result = contactRequestService.createContactRequest(owner, recipient);

        assertNotNull(result);
        verify(contactRequestRepository).save(any(ContactRequest.class));
    }

    @Test
    void requestExists_WhenRequestExists_ShouldReturnTrue() {
        String owner = "user1";
        String recipient = "user2";

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(true);

        boolean result = contactRequestService.requestExists(owner, recipient);

        assertTrue(result);
        verify(contactRequestRepository).existsByOwnerAndRecipient(owner, recipient);
    }

    @Test
    void requestExists_WhenRequestNotExists_ShouldReturnFalse() {
        String owner = "user1";
        String recipient = "user2";

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(false);

        boolean result = contactRequestService.requestExists(owner, recipient);

        assertFalse(result);
        verify(contactRequestRepository).existsByOwnerAndRecipient(owner, recipient);
    }

    @Test
    void getIncomingRequests_ShouldReturnRequests() {
        String recipient = "user2";
        List<ContactRequest> expectedRequests = Arrays.asList(new ContactRequest(), new ContactRequest());

        when(contactRequestRepository.findByRecipient(recipient)).thenReturn(expectedRequests);

        List<ContactRequest> result = contactRequestService.getIncomingRequests(recipient);

        assertEquals(expectedRequests, result);
        verify(contactRequestRepository).findByRecipient(recipient);
    }

    @Test
    void getOutgoingRequests_ShouldReturnRequests() {
        String owner = "user1";
        List<ContactRequest> expectedRequests = Arrays.asList(new ContactRequest(), new ContactRequest());

        when(contactRequestRepository.findByOwner(owner)).thenReturn(expectedRequests);

        List<ContactRequest> result = contactRequestService.getOutgoingRequests(owner);

        assertEquals(expectedRequests, result);
        verify(contactRequestRepository).findByOwner(owner);
    }

    @Test
    void deleteRequest_ShouldDeleteRequestAndNotifications() {
        String owner = "user1";
        String recipient = "user2";

        contactRequestService.deleteRequest(owner, recipient);

        verify(contactRequestRepository).deleteByOwnerAndRecipient(owner, recipient);
        verify(notificationRepository).findByOwnerIdAndTypeAndSenderId(recipient, "CONTACT_REQUEST", owner);
    }

    @Test
    void deleteRequest_WhenNotificationDeletionFails_ShouldStillDeleteRequest() {
        String owner = "user1";
        String recipient = "user2";

        doThrow(new RuntimeException("DB error"))
                .when(notificationRepository).findByOwnerIdAndTypeAndSenderId(anyString(), anyString(), anyString());

        contactRequestService.deleteRequest(owner, recipient);

        verify(contactRequestRepository).deleteByOwnerAndRecipient(owner, recipient);
    }

    @Test
    void deleteRequestById_WhenRequestExists_ShouldDeleteRequest() {
        Long requestId = 1L;
        ContactRequest contactRequest = new ContactRequest();
        contactRequest.setOwner("user1");
        contactRequest.setRecipient("user2");

        when(contactRequestRepository.findById(requestId)).thenReturn(Optional.of(contactRequest));

        contactRequestService.deleteRequestById(requestId);

        verify(contactRequestRepository).deleteById(requestId);
        verify(notificationRepository).findByOwnerIdAndTypeAndSenderId("user2", "CONTACT_REQUEST", "user1");
    }

    @Test
    void deleteRequestById_WhenRequestNotExists_ShouldStillCallDelete() {
        Long requestId = 1L;

        when(contactRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        contactRequestService.deleteRequestById(requestId);

        verify(contactRequestRepository).deleteById(requestId);
        verify(notificationRepository, never()).findByOwnerIdAndTypeAndSenderId(anyString(), anyString(), anyString());
    }

    @Test
    void acceptRequestAndNotify_ShouldDeleteRequestAndSendNotification() {
        String owner = "user1";
        String recipient = "user2";
        User recipientUser = new User();
        recipientUser.setUsername("TestUser");

        when(userService.findByUserId(recipient)).thenReturn(recipientUser);

        contactRequestService.acceptRequestAndNotify(owner, recipient);

        verify(contactRequestRepository).deleteByOwnerAndRecipient(owner, recipient);
        verify(notificationService).sendNotification(eq(owner), any(NotificationRequest.class));
    }

    @Test
    void acceptRequestAndNotify_WhenNotificationFails_ShouldStillDeleteRequest() {
        String owner = "user1";
        String recipient = "user2";

        when(userService.findByUserId(recipient)).thenReturn(new User());
        doThrow(new RuntimeException("Notification failed"))
                .when(notificationService).sendNotification(anyString(), any(NotificationRequest.class));

        contactRequestService.acceptRequestAndNotify(owner, recipient);

        verify(contactRequestRepository).deleteByOwnerAndRecipient(owner, recipient);
    }

    @Test
    void declineRequestAndNotify_ShouldDeleteRequest() {
        String owner = "user1";
        String recipient = "user2";

        contactRequestService.declineRequestAndNotify(owner, recipient);

        verify(contactRequestRepository).deleteByOwnerAndRecipient(owner, recipient);
    }

    @Test
    void getContactRequestNotifications_ShouldReturnNotifications() {
        String userId = "user1";
        List<Notification> expectedNotifications = Arrays.asList(new Notification(), new Notification());

        when(notificationRepository.findByOwnerIdAndType(userId, "CONTACT_REQUEST")).thenReturn(expectedNotifications);

        List<Notification> result = contactRequestService.getContactRequestNotifications(userId);

        assertEquals(expectedNotifications, result);
        verify(notificationRepository).findByOwnerIdAndType(userId, "CONTACT_REQUEST");
    }

    @Test
    void createContactRequest_WhenUserHasUsername_ShouldUseUsernameInNotification() {
        String owner = "user1";
        String recipient = "user2";
        User user = new User();
        user.setUsername("JohnDoe");

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(false);
        when(userService.findByUserId(recipient)).thenReturn(new User());
        when(userService.findByUserId(owner)).thenReturn(user);
        when(contactRequestRepository.save(any(ContactRequest.class))).thenReturn(new ContactRequest());

        contactRequestService.createContactRequest(owner, recipient);

        verify(notificationService).sendNotification(eq(recipient), any(NotificationRequest.class));
    }

    @Test
    void createContactRequest_WhenUserNotFound_ShouldUseDefaultNameInNotification() {
        String owner = "user1";
        String recipient = "user2";

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(false);
        when(userService.findByUserId(recipient)).thenReturn(new User());
        when(userService.findByUserId(owner)).thenReturn(null);
        when(contactRequestRepository.save(any(ContactRequest.class))).thenReturn(new ContactRequest());

        contactRequestService.createContactRequest(owner, recipient);

        verify(notificationService).sendNotification(eq(recipient), any(NotificationRequest.class));
    }

    @Test
    void createContactRequest_WhenUserServiceThrowsException_ShouldUseDefaultName() {
        String owner = "user1";
        String recipient = "user2";

        when(contactRequestRepository.existsByOwnerAndRecipient(owner, recipient)).thenReturn(false);
        when(userService.findByUserId(recipient)).thenReturn(new User());
        when(userService.findByUserId(owner)).thenThrow(new RuntimeException("Service unavailable"));
        when(contactRequestRepository.save(any(ContactRequest.class))).thenReturn(new ContactRequest());

        contactRequestService.createContactRequest(owner, recipient);

        verify(notificationService).sendNotification(eq(recipient), any(NotificationRequest.class));
    }

    @Test
    void deleteContactRequestNotification_WhenMultipleNotificationsExist_ShouldDeleteAll() {
        String senderId = "user1";
        String recipientId = "user2";
        List<Notification> notifications = Arrays.asList(new Notification(), new Notification());

        when(notificationRepository.findByOwnerIdAndTypeAndSenderId(recipientId, "CONTACT_REQUEST", senderId))
                .thenReturn(notifications);

        contactRequestService.deleteRequest(senderId, recipientId);

        verify(notificationRepository).deleteAll(notifications);
    }
}