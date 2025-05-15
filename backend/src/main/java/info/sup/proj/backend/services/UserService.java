package info.sup.proj.backend.services;


import info.sup.proj.backend.exceptions.UserAlreadyExistsException;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean checkUserExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
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
    
    /**
     * Updates a user's ELO score by adding the specified score amount
     * @param userId The ID of the user whose ELO score is being updated
     * @param scoreToAdd The score amount to add to the current ELO
     * @return An updated User object with the new ELO score
     * @throws RuntimeException if the user is not found
     */
    @Transactional
    public User updateUserElo(Long userId, Integer scoreToAdd) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        // Add the score to the current ELO
        int currentElo = user.getElo();
        int newElo = currentElo + scoreToAdd;
        
        // Update user's ELO
        user.setElo(newElo);
        
        // Save and return the updated user
        return userRepository.save(user);
    }
}