package com.e_health_care.web.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppointmentStatusUpdateDTO {
    @NotBlank(message = "status is required")
    private String status;
}
