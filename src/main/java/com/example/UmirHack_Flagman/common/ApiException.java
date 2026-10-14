package com.example.UmirHack_Flagman.common;

import org.springframework.http.HttpStatus;

import lombok.Getter;

// Бизнес-ошибка, которая отдается клиенту в формате ErrorResponse
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final transient Object details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(HttpStatus status, String code, String message, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }
}
