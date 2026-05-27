package com.e_health_care.web.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentStatsDTO {
    private long total;
    private long pending;
    private long confirmed;
    private long completed;
    private long cancelled;
}
