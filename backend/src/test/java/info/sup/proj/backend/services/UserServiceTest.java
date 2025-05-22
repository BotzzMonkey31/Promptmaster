package info.sup.proj.backend.services;

import info.sup.proj.backend.exceptions.UserAlreadyExistsException;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRegistrationDto registrationDto;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPicture("http://example.com/pic.jpg");
        testUser.setCountry("Test Country");
        testUser.setElo(1000);

        // Setup registration DTO
        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("new@example.com");
        registrationDto.setName("New User");
        registrationDto.setPicture("http://example.com/newpic.jpg");
        registrationDto.setCountry("New Country");
    }

    @Test
    void testCheckUserExists_Exists() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean exists = userService.checkUserExists(email);

        // Assert
        assertTrue(exists);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void testCheckUserExists_DoesNotExist() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // Act
        boolean exists = userService.checkUserExists(email);

        // Assert
        assertFalse(exists);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void testGetUserByEmail_Found() {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByEmail(email);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testGetUserByEmail_NotFound() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserByEmail(email);

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.registerUser(registrationDto);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(true);

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> userService.registerUser(registrationDto)
        );
        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registrationDto.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registrationDto.getUsername())).thenReturn(true);

        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
            UserAlreadyExistsException.class,
            () -> userService.registerUser(registrationDto)
        );
        assertEquals("Username already taken", exception.getMessage());
        verify(userRepository).existsByEmail(registrationDto.getEmail());
        verify(userRepository).existsByUsername(registrationDto.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserElo_Success() {
        // Arrange
        Long userId = 1L;
        Integer scoreToAdd = 25;
        int expectedNewElo = testUser.getElo() + scoreToAdd;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUserElo(userId, scoreToAdd);

        // Assert
        assertNotNull(result);
        assertEquals(expectedNewElo, result.getElo());
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    void testUpdateUserElo_UserNotFound() {
        // Arrange
        Long userId = 999L;
        Integer scoreToAdd = 25;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.updateUserElo(userId, scoreToAdd)
        );
        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }
}