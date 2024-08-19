package com.hkshenoy.jaltantraloopsb.security;



import java.io.UnsupportedEncodingException;
import java.util.List;

public interface UserService {

    User findUserByEmail(String email);

    public User authenticateUser(String email, String password);

    List<UserDto> findAllUsers();

    public boolean verify(String verificationCode);

    public String registerUser(UserDto userDto,String siteURL) throws UnsupportedEncodingException;
}