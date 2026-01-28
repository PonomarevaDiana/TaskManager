package org.example.authapp.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.example.authapp.entity.User;
import org.example.authapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSenderImpl mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if(user == null) {
            throw new UsernameNotFoundException(username);
        }

        return user;
    }

    public User loadUserById(String id) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()) {
            throw new UsernameNotFoundException(id);
        }
        return user.get();
    }

    public boolean saveUser(User user) throws MessagingException, UnsupportedEncodingException {
        User userFromDb = userRepository.findByUsername(user.getUsername());
        if(userFromDb != null) {
            return false;
        }

        userFromDb = userRepository.findByEmail(user.getEmail());
        if(userFromDb != null) {
            return false;
        }

        user.setUsername(user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmail(user.getEmail());
        user.setFirstName(user.getFirstName());
        user.setLastName(user.getLastName());
        user.setVerificationCode(UUID.randomUUID().toString());
        user.setEnabled(false);
        user.setRoles(user.getRoles());
        userRepository.save(user);

        sendVerificationEmail(user);

        return true;
    }

    public boolean updateUser(User user) {
        Optional<User> userFromDb = userRepository.findById(user.getId());

        if(userFromDb.isPresent()) {
            User updatedUser = userFromDb.get();
            updatedUser.setFirstName(user.getFirstName());
            updatedUser.setLastName(user.getLastName());
            updatedUser.setUsername(user.getUsername());

            userRepository.save(updatedUser);
            return true;
        } else {
            return false;
        }
    }

    public boolean changePassword(User user, String oldPassword, String newPassword) {
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return true;
        }

        return false;
    }

    public boolean deleteUser(String userId) {
        if (userRepository.findById(userId).isPresent()) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    private void sendVerificationEmail(User user) throws MessagingException, UnsupportedEncodingException {
        String toAddress = user.getEmail();
        String fromAddress = "task-manager.sup@yandex.ru";
        String senderName = "Менеджер задач";
        String subject = "Подтверждение регистрации в Менеджере задач";
        String content = "Уважаемый [[name]], <br>" +
                "Вы зарегестрировались в Менеджере задач. <br>" +
                "Чтобы подтвердить регистрацию перейдите по ссылке ниже: <br>" +
                "<h3><a href=\"[[URL]]\" target=\"_self\"> Подтвердить </a></h3>" +
                "<br>" +
                "С уважением, <br>" +
                "служба поддержки Менеджер задач. <br>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", user.getFirstName() + " " + user.getLastName());

        String verifyURL = frontendUrl + "/verify?code=" + user.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        mailSender.send(message);
    }

    public User verify(String code) {
        User user = userRepository.findByVerificationCode(code);

        if(user == null || user.isEnabled()) {
            return null;
        } else {
            user.setVerificationCode(null);
            user.setEnabled(true);
            userRepository.save(user);

            return user;
        }
    }
}