package info.sup.proj.backend.controllers;

import info.sup.proj.backend.exceptions.UnauthorizedException;
import info.sup.proj.backend.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class UserController {

    @GetMapping
    public User getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated via JWT
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();

            // In a real implementation, you would fetch user details from a database
            // For now, assuming JWT contains all necessary user info
            return new User(username, "https://graph.facebook.com/" + username + "/picture?type=large");
        } else {
            throw new UnauthorizedException("User not authenticated");
        }
    }

    // Custom exception for unauthorized access (optional, can be handled as per your error strategy)
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();  // Invalidates the session
        return "redirect:/";  // Redirects to homepage
    }

}
