package com.hkshenoy.jaltantraloopsb.controllers;

import com.hkshenoy.jaltantraloopsb.helper.CustomLogger;
import com.hkshenoy.jaltantraloopsb.security.User;
import com.hkshenoy.jaltantraloopsb.security.UserDto;
import com.hkshenoy.jaltantraloopsb.security.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@Controller
@RequestMapping("/register")
@CrossOrigin("*")
public class RegisterController {

    @Autowired
    private CustomLogger customLogger;

    @Autowired
    private UserService userService;

    @GetMapping("")
    public String showRegistrationForm(Model model){
        // create model object to store form data
        UserDto user = new UserDto();
        model.addAttribute("user", user);
        return "register";
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "");
    }




    @PostMapping("/save")
    public String registration(@Valid @ModelAttribute("user") UserDto userDto,
                               BindingResult result,
                               Model model,
                               HttpServletRequest request) throws UnsupportedEncodingException {

        User existingUser = userService.findUserByEmail(userDto.getEmail());

        if(existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()){
            result.rejectValue("email", null,
                    "There is already an account registered with the same email");
        }

        if(result.hasErrors()){
            model.addAttribute("user", userDto);
            return "/register";
        }

        userService.saveUser(userDto,getSiteURL(request));
        return "redirect:/register?success";
    }
}
