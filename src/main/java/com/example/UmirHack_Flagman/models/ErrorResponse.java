package com.example.UmirHack_Flagman.models;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
public class ErrorResponse {
    private ErrorBody error;

    @Setter 
    @Getter 
    public static class ErrorBody {
        private String code;
        private String message;
        private Object details;
        private String requestId;
    }
}
