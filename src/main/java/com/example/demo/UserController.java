package com.example.demo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    UserRepository userRepo;

    @GetMapping("/demo")
    public String demo() {
        return "Hello World!";
    }

    @GetMapping("/users")
    public Iterable<Users> getUsers() {
        return userRepo.findAll();
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, Object> payload) {
        String username = firstNonBlank(
                payload,
                "username",
                "name",
                "userName",
                "user_name"
        );
        String email = firstNonBlank(
                payload,
                "email",
                "mail",
                "emailId",
                "email_id"
        );
        String password = firstNonBlank(
                payload,
                "password",
                "pass",
                "pwd"
        );

        if (username == null || email == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("username, email and password are required");
        }

        Users user = new Users();
        user.setUsername(username.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(password);

        Users u = this.userRepo.findByEmail(user.getEmail());
        if (u != null) {
            return ResponseEntity.ok("Email already exists");
        }
        this.userRepo.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    private String firstNonBlank(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = value.toString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
