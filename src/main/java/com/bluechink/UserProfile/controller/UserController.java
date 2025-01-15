package com.bluechink.UserProfile.controller;

import com.bluechink.UserProfile.config.security.JwtUtil;
import com.bluechink.UserProfile.dto.JwtRequest;
import com.bluechink.UserProfile.dto.JwtResponse;
import com.bluechink.UserProfile.entity.User;
import com.bluechink.UserProfile.helper.UserNotFoundException;
import com.bluechink.UserProfile.service.UserDetailsService;
import com.bluechink.UserProfile.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsServiceImpl;

    @Autowired
    private JwtUtil jwtUtils;

    // Generate token
    @PostMapping("/generate-token")
    public ResponseEntity<?> generateToken(@RequestBody JwtRequest jwtRequest) {
        try {
            authenticate(jwtRequest.getEmail(), jwtRequest.getPassword());
            String token = this.jwtUtils.generateToken(jwtRequest.getEmail());
            return ResponseEntity.ok(new JwtResponse(token));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed: " + e.getMessage());
        }
    }

    private void authenticate(String username, String password) throws Exception {
        // Check if the user exists
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(username);
        if (userDetails == null) {
            throw new UserNotFoundException("User with username " + username + " not found");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (DisabledException e) {
            throw new Exception("User disabled: " + e.getMessage());
        } catch (BadCredentialsException e) {
            throw new Exception("Invalid credentials: " + e.getMessage());
        }
    }


    @PostMapping("/")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        try {
            User user = userService.updateUser(id, updatedUser);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
