package info.sup.proj.backend.controllers;

import info.sup.proj.backend.model.User;
import info.sup.proj.backend.model.UserRegistrationDto;
import info.sup.proj.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
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
        return ResponseEntity.ok(user);
    }
}