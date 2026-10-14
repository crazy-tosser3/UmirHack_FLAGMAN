package com.example.UmirHack_Flagman.common.dto;

import lombok.Getter;
import lombok.Setter;

// элемент error.details для 400 VALIDATION_ERROR
@Setter 
@Getter 
public class ValidationErrorDetail {
    private String field;
    private String reason;
}
