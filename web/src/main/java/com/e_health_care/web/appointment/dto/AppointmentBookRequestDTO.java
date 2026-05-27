package com.e_health_care.web.appointment.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentBookRequestDTO {
    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @NotNull(message = "scheduleTime is required")
    @Future(message = "scheduleTime must be in the future")
    private LocalDateTime scheduleTime;
}
