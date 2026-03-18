package com.hotelvista.controller;

import com.hotelvista.entity.User;
import com.hotelvista.repository.UserRepository;
import com.hotelvista.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String firstName = body.get("first_name");
        String lastName  = body.get("last_name");
        String email     = body.get("email");
        String password  = body.get("password");

        if (firstName == null || lastName == null || email == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("message", "All required fields must be provided."));
        if (password.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters."));
        if (userRepository.existsByEmail(email))
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already in use."));

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(body.get("phone"));
        user.setCountry(body.get("country"));
        user.setRole(User.Role.CLIENT);
        userRepository.save(user);

        String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(email));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Account created successfully.", "token", token, "user", buildUserMap(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email    = body.get("email");
        String password = body.get("password");
        if (email == null || password == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Email and password are required."));
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid credentials."));
        }
        User user = userRepository.findByEmail(email).orElseThrow();
        String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(email));
        return ResponseEntity.ok(Map.of("message", "Login successful.", "token", token, "user", buildUserMap(user)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(buildUserMap(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestBody Map<String, String> body) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (body.get("first_name") != null) user.setFirstName(body.get("first_name"));
        if (body.get("last_name")  != null) user.setLastName(body.get("last_name"));
        if (body.get("phone")      != null) user.setPhone(body.get("phone"));
        if (body.get("address")    != null) user.setAddress(body.get("address"));
        if (body.get("city")       != null) user.setCity(body.get("city"));
        if (body.get("country")    != null) user.setCountry(body.get("country"));
        if (body.get("date_of_birth") != null && !body.get("date_of_birth").isEmpty())
            user.setDateOfBirth(LocalDate.parse(body.get("date_of_birth")));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully.", "user", buildUserMap(user)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody Map<String, String> body) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        if (!passwordEncoder.matches(body.get("current_password"), user.getPassword()))
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect."));
        String newPass = body.get("new_password");
        if (newPass == null || newPass.length() < 6)
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 6 characters."));
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    private Map<String, Object> buildUserMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",         user.getId());
        map.put("first_name", user.getFirstName());
        map.put("last_name",  user.getLastName());
        map.put("email",      user.getEmail());
        map.put("phone",      user.getPhone()    != null ? user.getPhone()    : "");
        map.put("address",    user.getAddress()  != null ? user.getAddress()  : "");
        map.put("city",       user.getCity()     != null ? user.getCity()     : "");
        map.put("country",    user.getCountry()  != null ? user.getCountry()  : "");
        map.put("role",       user.getRole().name().toLowerCase());
        map.put("created_at", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return map;
    }
}
