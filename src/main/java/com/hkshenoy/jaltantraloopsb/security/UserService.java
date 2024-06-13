package com.hkshenoy.jaltantraloopsb.security;



import java.io.UnsupportedEncodingException;
import java.util.List;

public interface UserService {
    void saveUser(UserDto userDto,String siteURL) throws UnsupportedEncodingException;

    User findUserByEmail(String email);

    List<UserDto> findAllUsers();

    public boolean verify(String verificationCode);
}