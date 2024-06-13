package com.hkshenoy.jaltantraloopsb.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto
{
    private Long id;

    @NotEmpty(message = "Name should not be empty")
    private String Name;

    @NotEmpty(message = "Country should not be empty")
    private String country;

    private String state;

    @NotEmpty(message = "Organization/institution should not be empty")
    private String organization;

    @NotEmpty(message = "Designation should not be empty")
    private String designation;


    @NotEmpty(message = "Email should not be empty")
    @Email
    private String email;



    @Size(min = 8,message = "Password must be atleast 8 character")
    private String password;


}
