package com.hkshenoy.jaltantraloopsb.controllers;

import com.hkshenoy.jaltantraloopsb.helper.CustomLogger;
import com.hkshenoy.jaltantraloopsb.security.JwtTokenUtil;
import com.hkshenoy.jaltantraloopsb.security.User;
import com.hkshenoy.jaltantraloopsb.security.UserDto;
import com.hkshenoy.jaltantraloopsb.security.UserService;
import com.hkshenoy.jaltantraloopsb.structs.JwtResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@RestController
@RequestMapping("/register")
@CrossOrigin("*")
public class RegisterController {

    @Autowired
    private CustomLogger customLogger;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping("")
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

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }

}
