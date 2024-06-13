package com.hkshenoy.jaltantraloopsb.controllers;



import com.hkshenoy.jaltantraloopsb.helper.*;


import com.hkshenoy.jaltantraloopsb.security.User;
import com.hkshenoy.jaltantraloopsb.security.UserRepository;
import com.hkshenoy.jaltantraloopsb.security.UserRequestDetail;
import com.hkshenoy.jaltantraloopsb.security.UserSessionDetail;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


@Controller
@RequestMapping("/")
@CrossOrigin("*")
public class JaltantraLoopController {

    @Autowired
    private OptimizationPerformer optimizationPerformer;

    @Autowired
    private NonOptimizationRelatedActionPerformer nonOptimizationRelatedActionPerformer;

    @Autowired
    private CustomLogger customLogger;

    @Autowired
    private UserHistoryTracker userHistoryTracker;



    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${JALTANTRA_VERSION}")
    private String version;


    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails user, Model model){
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        }
        return "front_controller";
    }
    @GetMapping("/loop")
    public String loop_home(Model model){
        model.addAttribute("contextPath",contextPath);
        model.addAttribute("version",version);
        return "loop";
    }





    //handle request from JalTantra site
    @PostMapping("/optimizer")
    protected void doPost(@AuthenticationPrincipal UserDetails user,HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        customLogger.logd("doPost() called");

        if(user != null){
            userHistoryTracker.saveUserRequest(user,request);
        }



        //action refers to uploading some file or optimization of network
        final String action = request.getParameter("action");
        customLogger.logd("doPost() action=" + action);

        String requestBody = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);


        System.out.println("Request Parameters:");
        request.getParameterMap().forEach((key, value) -> {
            customLogger.logd(key + ": " + Arrays.toString(value));
        });

        if (action != null) {
            // Some input output related action is needed. Nothing to do with Optimization
            nonOptimizationRelatedActionPerformer.performNonOptimizationRelatedAction(request, response, action);
            return;
        }

        final String runTime=request.getParameter("time");
        System.out.println(runTime);

        optimizationPerformer.performOptimization(request, response);
    }
}
