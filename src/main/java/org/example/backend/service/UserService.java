package org.example.backend.service;

import org.example.backend.dto.UserRegistrationDTO;
import org.example.backend.model.User;
import org.example.backend.repository.UserRepository;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository
//                       ,PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(UserRegistrationDTO userDTO) {
        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());

        return userRepository.save(user);
    }
}
