package com.example.businessLogic.service;

import com.example.businessLogic.dto.UserInfo;
import com.example.businessLogic.entity.User;
import com.example.businessLogic.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContactService contactService;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        User user1 = createUser("user1", "user1", "user1@test.com");
        User user2 = createUser("user2", "user2", "user2@test.com");
        List<User> expectedUsers = Arrays.asList(user1, user2);

        when(userRepository.findAll()).thenReturn(expectedUsers);

        List<User> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedUsers, result);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.getAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void saveUser_WhenUserDoesNotExist_ShouldSaveAndReturnTrue() {
        UserInfo userInfo = createUserInfo("new-user", "newuser", "new@example.com", "John", "Doe");

        when(userRepository.findById("new-user")).thenReturn(Optional.empty());
        when(contactService.getNewSecretFriendshipKey()).thenReturn("friendship-key-123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = userService.saveUser(userInfo);

        assertTrue(result);
        verify(userRepository, times(1)).findById("new-user");
        verify(contactService, times(1)).getNewSecretFriendshipKey();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void saveUser_WhenUserAlreadyExists_ShouldReturnFalse() {
        UserInfo userInfo = createUserInfo("existing-user", "existinguser", "existing@test.com", "Jane", "Smith");
        User existingUser = createUser("existing-user", "existinguser", "existing@test.com");

        when(userRepository.findById("existing-user")).thenReturn(Optional.of(existingUser));

        boolean result = userService.saveUser(userInfo);

        assertFalse(result);
        verify(userRepository, times(1)).findById("existing-user");
        verify(contactService, never()).getNewSecretFriendshipKey();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserExistsAndAuthServiceSuccess_ShouldUpdateAndReturnTrue() {
        User existingUser = createUser("user1", "oldUsername", "old@test.com");
        existingUser.setFirstName("OldFirstName");
        existingUser.setLastName("OldLastName");

        UserInfo userInfo = createUserInfo("user1", "newUsername", "new@test.com", "NewFirstName", "NewLastName");

        when(userRepository.findById("user1")).thenReturn(Optional.of(existingUser));
        when(authServiceClient.updateProfile(any(User.class))).thenReturn(ResponseEntity.ok().build());
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        boolean result = userService.updateUser(existingUser, userInfo);

        assertTrue(result);
        verify(userRepository, times(1)).findById("user1");
        verify(authServiceClient, times(1)).updateProfile(any(User.class));
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    void updateUser_WhenUserExistsButAuthServiceFails_ShouldReturnFalse() {
        User existingUser = createUser("user1", "username", "user@test.com");
        UserInfo userInfo = createUserInfo("user1", "newUsername", "new@test.com", "First", "Last");

        when(userRepository.findById("user1")).thenReturn(Optional.of(existingUser));
        when(authServiceClient.updateProfile(any(User.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());

        boolean result = userService.updateUser(existingUser, userInfo);

        assertFalse(result);
        verify(userRepository, times(1)).findById("user1");
        verify(authServiceClient, times(1)).updateProfile(any(User.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserDoesNotExist_ShouldReturnFalse() {
        User user = createUser("non-existent", "username", "user@test.com");
        UserInfo userInfo = createUserInfo("non-existent", "username", "user@test.com", "First", "Last");

        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        boolean result = userService.updateUser(user, userInfo);

        assertFalse(result);
        verify(userRepository, times(1)).findById("non-existent");
        verify(authServiceClient, never()).updateProfile(any(User.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_ShouldUpdateCorrectFields() {
        User existingUser = createUser("user1", "oldUsername", "old@test.com");
        existingUser.setFirstName("OldFirstName");
        existingUser.setLastName("OldLastName");

        UserInfo userInfo = createUserInfo("user1", "newUsername", "new@test.com", "NewFirstName", "NewLastName");

        when(userRepository.findById("user1")).thenReturn(Optional.of(existingUser));
        when(authServiceClient.updateProfile(any(User.class))).thenReturn(ResponseEntity.ok().build());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = userService.updateUser(existingUser, userInfo);

        assertTrue(result);
        verify(userRepository).save(argThat(updatedUser ->
                "newUsername".equals(updatedUser.getUsername()) &&
                        "NewFirstName".equals(updatedUser.getFirstName()) &&
                        "NewLastName".equals(updatedUser.getLastName()) &&
                        "user1".equals(updatedUser.getId()) // ID не должен меняться
        ));
    }

    @Test
    void findByUserId_WhenUserExists_ShouldReturnUser() {
        User expectedUser = createUser("user1", "testuser", "test@test.com");
        when(userRepository.findById("user1")).thenReturn(Optional.of(expectedUser));

        User result = userService.findByUserId("user1");

        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository, times(1)).findById("user1");
    }

    @Test
    void findByUserId_WhenUserDoesNotExist_ShouldReturnNull() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());

        User result = userService.findByUserId("non-existent");

        assertNull(result);
        verify(userRepository, times(1)).findById("non-existent");
    }

    @Test
    void findByUserId_WithNullId_ShouldReturnNull() {
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        User result = userService.findByUserId(null);

        assertNull(result);
        verify(userRepository, times(1)).findById(null);
    }

    @Test
    void findByUsername_WhenUserExists_ShouldReturnUser() {
        User expectedUser = createUser("user1", "testuser", "test@test.com");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(expectedUser));

        User result = userService.findByUsername("testuser");

        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void findByUsername_WhenUserDoesNotExist_ShouldReturnNull() {
        when(userRepository.findByUsername("non-existent")).thenReturn(Optional.empty());

        User result = userService.findByUsername("non-existent");

        assertNull(result);
        verify(userRepository, times(1)).findByUsername("non-existent");
    }

    @Test
    void findByUsername_WithNullUsername_ShouldReturnNull() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        User result = userService.findByUsername(null);

        assertNull(result);
        verify(userRepository, times(1)).findByUsername(null);
    }

    @Test
    void findByFriendshipKeyAndUsername_WhenUserExists_ShouldReturnUser() {
        User expectedUser = createUser("user1", "testuser", "test@test.com");
        expectedUser.setFriendshipKey("friend-key");

        when(userRepository.findByFriendshipKeyAndUsername("friend-key", "testuser"))
                .thenReturn(Optional.of(expectedUser));

        User result = userService.findByFriendshipKeyAndUsername("friend-key", "testuser");

        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository, times(1)).findByFriendshipKeyAndUsername("friend-key", "testuser");
    }

    @Test
    void findByFriendshipKeyAndUsername_WhenUserDoesNotExist_ShouldReturnNull() {
        when(userRepository.findByFriendshipKeyAndUsername("wrong-key", "testuser"))
                .thenReturn(Optional.empty());

        User result = userService.findByFriendshipKeyAndUsername("wrong-key", "testuser");

        assertNull(result);
        verify(userRepository, times(1)).findByFriendshipKeyAndUsername("wrong-key", "testuser");
    }

    @Test
    void findByFriendshipKeyAndUsername_WithNullParameters_ShouldReturnNull() {
        when(userRepository.findByFriendshipKeyAndUsername(null, null))
                .thenReturn(Optional.empty());

        User result = userService.findByFriendshipKeyAndUsername(null, null);

        assertNull(result);
        verify(userRepository, times(1)).findByFriendshipKeyAndUsername(null, null);
    }

    @Test
    void saveUser_WithNullUserInfo_ShouldThrowException() {
        assertThrows(NullPointerException.class, () -> userService.saveUser(null));
    }

    @Test
    void updateUser_WithNullUser_ShouldThrowException() {
        UserInfo userInfo = createUserInfo("user1", "username", "email@test.com", "First", "Last");

        assertThrows(NullPointerException.class, () -> userService.updateUser(null, userInfo));
    }

    private User createUser(String id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setFriendshipKey("test-key");
        return user;
    }

    private UserInfo createUserInfo(String userId, String username, String email, String firstName, String lastName) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setUsername(username);
        userInfo.setEmail(email);
        userInfo.setFirstName(firstName);
        userInfo.setLastName(lastName);
        return userInfo;
    }
}