package info.sup.proj.backend.services;


import info.sup.proj.backend.exceptions.UserAlreadyExistsException;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean checkUserExists(String email) {
        return userRepository.existsByEmail(email);
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setName(registrationDto.getName());
        user.setUsername(registrationDto.getUsername());
        user.setPicture(registrationDto.getPicture());
        user.setCountry(registrationDto.getCountry());

        return userRepository.save(user);
    }
}