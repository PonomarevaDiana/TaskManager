package com.example.businessLogic.service;

import com.example.businessLogic.dto.UserInfo;
import com.example.businessLogic.entity.User;
import com.example.businessLogic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ContactService contactService;
    private final AuthServiceClient authServiceClient;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean saveUser(UserInfo userInfo) {
        Optional<User> userFromDB = userRepository.findById(userInfo.getUserId());
        if (userFromDB.isPresent()) {
            return false;
        }

        User newUser = new User();
        newUser.setId(userInfo.getUserId());
        newUser.setUsername(userInfo.getUsername());
        newUser.setEmail(userInfo.getEmail());
        newUser.setFirstName(userInfo.getFirstName());
        newUser.setLastName(userInfo.getLastName());
        newUser.setFriendshipKey(contactService.getNewSecretFriendshipKey());
        newUser.setRoles(userInfo.getRoles());

        userRepository.save(newUser);
        return true;
    }

    public boolean updateUser(User user, UserInfo userInfo) {
        Optional<User> userFromDB = userRepository.findById(user.getId());
        if (userFromDB.isPresent()) {
            User updatedUser = userFromDB.get();
            updatedUser.setUsername(userInfo.getUsername());
            updatedUser.setFirstName(userInfo.getFirstName());
            updatedUser.setLastName(userInfo.getLastName());

            ResponseEntity<Void> response = authServiceClient.updateProfile(updatedUser);
            if (response.getStatusCode().is2xxSuccessful()) {
                userRepository.save(updatedUser);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public User findByUserId(String userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.orElse(null);
    }

    public User findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.orElse(null);
    }

    public User findByFriendshipKeyAndUsername(String friendshipKey, String username) {
        return userRepository.findByFriendshipKeyAndUsername(friendshipKey, username)
                .orElse(null);
    }
}