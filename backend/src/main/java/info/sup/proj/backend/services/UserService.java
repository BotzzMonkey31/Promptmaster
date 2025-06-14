package info.sup.proj.backend.services;


import info.sup.proj.backend.exceptions.UserAlreadyExistsException;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
      @Transactional
    public User updateUserElo(Long userId, Integer scoreToAdd) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        int currentElo = user.getElo();
        int newElo = currentElo + scoreToAdd;
        
        user.setElo(newElo);
        
        return userRepository.save(user);
    }

    /**
     * Get global rankings ordered by ELO
     */
    public Page<User> getGlobalRankings(Pageable pageable) {
        return userRepository.findAllByOrderByEloDesc(pageable);
    }    /**
     * Get local rankings by country ordered by ELO
     */
    public Page<User> getLocalRankings(String country, Pageable pageable) {
        return userRepository.findByCountryOrderByEloDesc(country, pageable);
    }
    
    /**
     * Search users by username (partial match)
     */
    public List<User> searchUsersByUsername(String query) {
        String searchTerm = "%" + query.toLowerCase() + "%";
        return userRepository.findByUsernameContainingIgnoreCase(searchTerm);
    }
}