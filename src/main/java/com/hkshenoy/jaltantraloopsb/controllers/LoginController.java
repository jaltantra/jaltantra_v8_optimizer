package com.hkshenoy.jaltantraloopsb.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/login")
@CrossOrigin("*")
public class LoginController {
    @GetMapping("")
    public String login(){
        return "login";
    }

}
