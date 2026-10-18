package com.shopsphere.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timeStamp;
    private int status;
    private String message;
    private Map<String, String> errors;

    public ErrorResponseDto(LocalDateTime timeStamp, int status, String message) {
        this.timeStamp = timeStamp;
        this.status = status;
        this.message = message;
    }

    public ErrorResponseDto(LocalDateTime timeStamp, int status, String message,
                            Map<String, String> errors) {
        this(timeStamp, status, message);
        this.errors = errors;
    }

    public ErrorResponseDto(LocalDateTime timeStamp, int status, String message, String requestURI) {
        this(timeStamp, status, message);
    }
}
