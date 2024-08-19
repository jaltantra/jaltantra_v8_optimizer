package com.hkshenoy.jaltantraloopsb.controllers;

import com.hkshenoy.jaltantraloopsb.security.JwtTokenUtil;
import com.hkshenoy.jaltantraloopsb.security.User;
import com.hkshenoy.jaltantraloopsb.security.UserDto;
import com.hkshenoy.jaltantraloopsb.security.UserService;
import com.hkshenoy.jaltantraloopsb.structs.JwtResponse;
import com.hkshenoy.jaltantraloopsb.structs.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class UserController {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;


    // Endpoint for register
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserDto userDto, HttpServletRequest request) throws UnsupportedEncodingException {
        User existingUser = userService.findUserByEmail(userDto.getEmail());

        if (existingUser != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("There is already an account registered with the same email sayantan");
        }

        String jwtToken = userService.registerUser(userDto, getSiteURL(request));
        return ResponseEntity.ok(new JwtResponse(jwtToken));
    }

    //Endpoint for login
    @GetMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
        if (user != null) {
            String token = jwtTokenUtil.generateToken(user);
            return ResponseEntity.ok(new JwtResponse(token));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

    //Endpoint to get User Details
    @GetMapping("/user")
    public ResponseEntity<User> getUserDetails(@RequestHeader("Authorization") String authHeader) {
        // Extract token from the header
        String token = authHeader.replace("Bearer ", "");

        // Get email from the token
        String email = jwtTokenUtil.getEmailFromToken(token);

        // Find user by email
        User user = userService.findUserByEmail(email);

        if (user != null) {
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
