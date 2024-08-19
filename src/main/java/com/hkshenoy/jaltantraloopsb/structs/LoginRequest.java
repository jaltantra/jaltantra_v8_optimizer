package com.hkshenoy.jaltantraloopsb.structs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    // Getters and Setters
    private String email;
    private String password;

}

