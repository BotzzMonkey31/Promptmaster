package info.sup.proj.backend.controllers;

import info.sup.proj.backend.exceptions.UserAlreadyExistsException;
import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.services.UserService;
import info.sup.proj.backend.dto.ApiResponse;
import info.sup.proj.backend.dto.UpdateEloRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(@RequestParam String query) {
        try {
            List<User> users = userService.searchUsersByUsername(query);
            return ResponseEntity.ok(new ApiResponse<>(true, "Users found", users));
        } catch (Exception e) {
            logger.error("Error searching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Error searching users"));
        }
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

    /**
     * Get global rankings with pagination
     */
    @GetMapping("/rankings/global")
    public ResponseEntity<Page<User>> getGlobalRankings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> rankings = userService.getGlobalRankings(pageable);
            return ResponseEntity.ok(rankings);
        } catch (Exception e) {
            logger.error("Error fetching global rankings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get local rankings (by country) with pagination
     */
    @GetMapping("/rankings/local")
    public ResponseEntity<Page<User>> getLocalRankings(
            @RequestParam String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> rankings = userService.getLocalRankings(country, pageable);
            return ResponseEntity.ok(rankings);
        } catch (Exception e) {
            logger.error("Error fetching local rankings for country: {}", country, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}