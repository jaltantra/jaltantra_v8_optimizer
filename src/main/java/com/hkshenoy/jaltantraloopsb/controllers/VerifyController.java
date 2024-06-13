package com.hkshenoy.jaltantraloopsb.controllers;

import com.hkshenoy.jaltantraloopsb.helper.CustomLogger;
import com.hkshenoy.jaltantraloopsb.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@CrossOrigin("*")
public class VerifyController {

    @Autowired
    private UserService userService;

    @GetMapping("/verify")
    public String verifyUser(@Param("code") String code) {
        System.out.println("Received Verify request");
        if (userService.verify(code)) {
            return "verify_success";
        } else {
            return "verify_fail";
        }
    }
}
