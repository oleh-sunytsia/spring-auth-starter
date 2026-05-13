package com.benatti.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest implements Serializable {
    
    /** Username */
    private String username;
    
    /** Password */
    private String password;
}
