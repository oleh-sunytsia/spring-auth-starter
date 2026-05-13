package com.benatti.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidateTokenResponse implements Serializable {
    
    /** Is token valid */
    private boolean valid;
    
    /** User ID */
    private String userId;
    
    /** User roles */
    private List<String> roles;
    
    /** User permissions */
    private List<String> permissions;
}
