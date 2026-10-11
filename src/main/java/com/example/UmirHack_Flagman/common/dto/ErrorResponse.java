package com.example.UmirHack_Flagman.common.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorResponse {
    private ErrorBody error;

    public static ErrorResponse of(String code, String message, Object details) {
        ErrorBody body = new ErrorBody();
        body.setCode(code);
        body.setMessage(message);
        body.setDetails(details);
        body.setRequestId("req-" + UUID.randomUUID());

        ErrorResponse response = new ErrorResponse();
        response.setError(body);
        return response;
    }

    @Setter
    @Getter
    public static class ErrorBody {
        private String code;
        private String message;
        private Object details;
        private String requestId;
    }
}
