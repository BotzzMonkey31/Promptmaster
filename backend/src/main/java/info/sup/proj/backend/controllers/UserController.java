package info.sup.proj.backend.controllers;

import info.sup.proj.backend.exceptions.UserAlreadyExistsException;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.services.UserService;
import info.sup.proj.backend.dto.ApiResponse;
import info.sup.proj.backend.dto.UpdateEloRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/email")
    public ResponseEntity<User> getUserByEmailParam(@RequestParam String email) {
        logger.info("Received request for email: {}", email);
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/check/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkUser(@PathVariable String email) {
        boolean exists = userService.checkUserExists(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "User check completed", exists));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody UserRegistrationDto registrationDto) {
        try {
            User user = userService.registerUser(registrationDto);
            return new ResponseEntity<>(new ApiResponse<>(true, "User created successfully", user), HttpStatus.CREATED);
        } catch (UserAlreadyExistsException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(false, "User already exists"));
        }
    }
    
    /**
     * Update a user's ELO score by adding the puzzle completion score
     */
    @PostMapping("/update-elo")
    public ResponseEntity<ApiResponse<Integer>> updateUserElo(@RequestBody UpdateEloRequestDto request) {
        try {
            User updatedUser = userService.updateUserElo(request.getUserId(), request.getScoreToAdd());
            return ResponseEntity.ok(new ApiResponse<>(
                true,
                "ELO updated successfully",
                updatedUser.getElo()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Failed to update ELO: " + e.getMessage()));
        }
    }
}