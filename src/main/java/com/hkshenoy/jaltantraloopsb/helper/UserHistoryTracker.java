package com.hkshenoy.jaltantraloopsb.helper;


import com.hkshenoy.jaltantraloopsb.security.User;
import com.hkshenoy.jaltantraloopsb.security.UserRepository;
import com.hkshenoy.jaltantraloopsb.security.UserRequestDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/* Helper class to store the user request history*/

@Component
public class UserHistoryTracker {

    @Autowired
    UserRepository userRepository;

    public void saveUserRequest(@AuthenticationPrincipal UserDetails userDetail, HttpServletRequest request){

        String email = userDetail.getUsername();

        User user = userRepository.findByEmail(email);

        UserRequestDetail userRequestDetail=new UserRequestDetail();

        userRequestDetail.setRequestUrl(request.getRequestURI());
        userRequestDetail.setSessionId(request.getSession().getId());


        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        userRequestDetail.setRequestDatetime(timeStamp);


        if(user.getRequestDetail()==null){
            user.setRequestDetail(new ArrayList<>());
        }


        user.getRequestDetail().add(userRequestDetail);

        userRepository.save(user);
    }
}
