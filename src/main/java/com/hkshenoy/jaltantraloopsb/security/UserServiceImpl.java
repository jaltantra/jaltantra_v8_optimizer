package com.hkshenoy.jaltantraloopsb.security;


import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    private final JwtTokenUtil jwtTokenUtil;


    @Value("${spring.mail.username}")
    private String mailSenderUsername;

    @Value("${sendgrid.api-key}")
    private String apiKey;



    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenUtil jwtTokenUtil){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }


    @Override
    public String registerUser(UserDto userDto,String siteURL) throws UnsupportedEncodingException {

        User user = new User();
        //user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setCountry(userDto.getCountry());
        user.setOrganization(userDto.getOrganization());
        user.setState(userDto.getState());
        user.setDesignation(userDto.getDesignation());

        // encrypt the password using spring security
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        String randomCode = RandomStringGenerator.generateRandomString(64);
        user.setVerificationCode(randomCode);


        //user.setEnabled(false);
        user.setEnabled(true);



        userRepository.save(user);
        return jwtTokenUtil.generateToken(user);
    }

    public User authenticateUser(String email, String password) {
        // Validate the username and password
        User user = userRepository.findByEmail(email);
        System.out.println(password);
        System.out.println(passwordEncoder.encode(password));
        System.out.println(user.getPassword());
        System.out.println(user.getEmail());

        if (user != null && passwordEncoder.matches(password, user.getPassword()) ){
            return user;
        }
        return null;
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map((user) -> mapToUserDto(user))
                .collect(Collectors.toList());
    }

    private UserDto mapToUserDto(User user){
        UserDto userDto = new UserDto();
        //String[] str = user.getName().split(" ");
        //userDto.setFirstName(str[0]);
        //userDto.setLastName(str[1]);
        userDto.setEmail(user.getEmail());
        return userDto;
    }



    //currently not being used
    private void sengridEmailSender(User user, String siteURL){

        Email from = new Email(mailSenderUsername);
        String subject = "Please verify your registration ";

        String toAddress = user.getEmail();
        Email to = new Email(toAddress);

        String Mailcontent = "Dear [[name]],<br>"+ "Please click the link below to verify your registration:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>"
                + "Thank you,<br>" + "Team Jaltantra.";

        Mailcontent = Mailcontent.replace("[[name]]", user.getName());
        String verifyURL = siteURL + "/verify?code=" + user.getVerificationCode();

        Mailcontent = Mailcontent.replace("[[URL]]", verifyURL);

        Content content = new Content("text/html", Mailcontent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }

    }
    public void register(User userDto, String siteURL)  {
            String encodedPassword = passwordEncoder.encode(userDto.getPassword());
            userDto.setPassword(encodedPassword);


    }

    public boolean verify(String verificationCode) {
            User user = userRepository.findByVerificationCode(verificationCode);

            if (user == null || user.isEnabled()) {
                return false;
            } else {
                user.setVerificationCode(null);
                user.setEnabled(true);
                userRepository.save(user);
                return true;
            }
    }
}
