package com.e_health_care.web.appointment.exception;

import org.springframework.http.HttpStatus;

public class AppointmentException extends RuntimeException {
    private final HttpStatus status;

    public AppointmentException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
