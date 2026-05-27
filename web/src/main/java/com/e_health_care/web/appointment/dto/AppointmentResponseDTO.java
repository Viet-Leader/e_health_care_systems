package com.e_health_care.web.appointment.dto;

import com.e_health_care.web.patient.model.Appointment;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentResponseDTO {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientEmail;
    private Long doctorId;
    private String doctorName;
    private String doctorEmail;
    private LocalDateTime scheduleTime;
    private String status;
    private boolean upcoming;

    public static AppointmentResponseDTO from(Appointment appointment) {
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setId(appointment.getId());
        dto.setScheduleTime(appointment.getScheduleTime());
        dto.setStatus(appointment.getStatus());
        dto.setUpcoming(appointment.getScheduleTime().isAfter(LocalDateTime.now()));

        if (appointment.getPatient() != null) {
            dto.setPatientId(appointment.getPatient().getId());
            dto.setPatientName(appointment.getPatient().getFirstName() + " "
                    + appointment.getPatient().getLastName());
            dto.setPatientEmail(appointment.getPatient().getEmail());
        }
        if (appointment.getDoctor() != null) {
            dto.setDoctorId(appointment.getDoctor().getId());
            dto.setDoctorName(appointment.getDoctor().getFirstName() + " "
                    + appointment.getDoctor().getLastName());
            dto.setDoctorEmail(appointment.getDoctor().getEmail());
        }
        return dto;
    }
}
