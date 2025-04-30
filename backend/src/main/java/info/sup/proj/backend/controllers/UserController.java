package info.sup.proj.backend.controllers;

import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

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
        System.out.println("Received request for email: " + email);
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/check/{email}")
    public ResponseEntity<Map<String, Boolean>> checkUser(@PathVariable String email) {
        boolean exists = userService.checkUserExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody UserRegistrationDto registrationDto) {
        User user = userService.registerUser(registrationDto);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }
    
    /**
     * Update a user's ELO score by adding the puzzle completion score
     */
    @PostMapping("/update-elo")
    public ResponseEntity<Map<String, Object>> updateUserElo(@RequestBody Map<String, Object> request) {
        try {
            // Extract parameters from the request
            Long userId = Long.parseLong(request.get("userId").toString());
            Integer scoreToAdd = Integer.parseInt(request.get("scoreToAdd").toString());
            
            // Call service to update the user's ELO
            User updatedUser = userService.updateUserElo(userId, scoreToAdd);
            
            // Return success response with the new ELO
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "ELO updated successfully",
                "newElo", updatedUser.getElo()
            ));
        } catch (Exception e) {
            // Return error response
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Failed to update ELO: " + e.getMessage()
            ));
        }
    }
}