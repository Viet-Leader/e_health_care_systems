package com.e_health_care.web.patient.service;

import com.e_health_care.web.appointment.AppointmentStatus;
import com.e_health_care.web.appointment.dto.AppointmentBookRequestDTO;
import com.e_health_care.web.appointment.dto.AppointmentResponseDTO;
import com.e_health_care.web.appointment.dto.AppointmentStatsDTO;
import com.e_health_care.web.appointment.exception.AppointmentException;
import com.e_health_care.web.doctor.model.Doctor;
import com.e_health_care.web.doctor.repository.DoctorRepository;
import com.e_health_care.web.patient.dto.AppointmentRequestDTO;
import com.e_health_care.web.patient.model.Appointment;
import com.e_health_care.web.patient.model.Patient;
import com.e_health_care.web.patient.repository.PatientAppointmentRepository;
import com.e_health_care.web.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PatientAppointmentService {

    private static final int SLOT_HOURS = 1;

    @Autowired
    private PatientAppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    // --- MVC (legacy DTO) ---

    public Appointment bookAppointment(AppointmentRequestDTO request) {
        AppointmentBookRequestDTO apiRequest = new AppointmentBookRequestDTO();
        apiRequest.setDoctorId(request.getDoctorId());
        apiRequest.setScheduleTime(request.getScheduleTime());
        return bookAppointment(request.getPatientId(), apiRequest);
    }

    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public Appointment updateAppointmentStatus(Long appointmentId, String status) {
        return updateStatusAsDoctor(appointmentId, null, status);
    }

    // --- REST API (SRS cross-role) ---

    @Transactional
    public Appointment bookAppointment(Long patientId, AppointmentBookRequestDTO request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new AppointmentException("Patient not found", HttpStatus.NOT_FOUND));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new AppointmentException("Doctor not found", HttpStatus.NOT_FOUND));

        LocalDateTime scheduleTime = request.getScheduleTime();
        if (scheduleTime.isBefore(LocalDateTime.now())) {
            throw new AppointmentException("Cannot book appointment in the past", HttpStatus.BAD_REQUEST);
        }

        assertDoctorAvailable(doctor.getId(), scheduleTime);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setScheduleTime(scheduleTime);
        appointment.setStatus(AppointmentStatus.PENDING.name());
        return appointmentRepository.save(appointment);
    }

    public List<AppointmentResponseDTO> listForPatient(Long patientId, String status, String filter) {
        List<Appointment> appointments = resolvePatientList(patientId, status, filter);
        return appointments.stream()
                .sorted(Comparator.comparing(Appointment::getScheduleTime).reversed())
                .map(AppointmentResponseDTO::from)
                .toList();
    }

    public List<AppointmentResponseDTO> listForDoctor(Long doctorId, String status) {
        List<Appointment> appointments = status == null || status.isBlank()
                ? appointmentRepository.findByDoctorId(doctorId)
                : appointmentRepository.findByDoctorIdAndStatus(doctorId, normalizeStatus(status));
        return appointments.stream()
                .sorted(Comparator.comparing(Appointment::getScheduleTime).reversed())
                .map(AppointmentResponseDTO::from)
                .toList();
    }

    public List<AppointmentResponseDTO> listAll(String status) {
        List<Appointment> appointments = status == null || status.isBlank()
                ? appointmentRepository.findAll()
                : appointmentRepository.findByStatus(normalizeStatus(status));
        return appointments.stream()
                .sorted(Comparator.comparing(Appointment::getScheduleTime).reversed())
                .map(AppointmentResponseDTO::from)
                .toList();
    }

    public AppointmentResponseDTO getById(Long appointmentId) {
        return AppointmentResponseDTO.from(findOrThrow(appointmentId));
    }

    @Transactional
    public AppointmentResponseDTO updateStatusAsPatient(Long appointmentId, Long patientId, String statusRaw) {
        Appointment appointment = findOrThrow(appointmentId);
        assertOwnershipPatient(appointment, patientId);
        AppointmentStatus target = AppointmentStatus.fromString(statusRaw);
        applyTransition(appointment, target, AppointmentStatus.AppointmentActor.PATIENT);
        return AppointmentResponseDTO.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public Appointment updateStatusAsDoctor(Long appointmentId, Long doctorId, String statusRaw) {
        Appointment appointment = findOrThrow(appointmentId);
        if (doctorId != null) {
            assertOwnershipDoctor(appointment, doctorId);
        }
        AppointmentStatus target = AppointmentStatus.fromString(statusRaw);
        applyTransition(appointment, target, AppointmentStatus.AppointmentActor.DOCTOR);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public AppointmentResponseDTO cancelAsAdmin(Long appointmentId) {
        Appointment appointment = findOrThrow(appointmentId);
        applyTransition(appointment, AppointmentStatus.CANCELLED, AppointmentStatus.AppointmentActor.ADMIN);
        return AppointmentResponseDTO.from(appointmentRepository.save(appointment));
    }

    public AppointmentStatsDTO getStats() {
        return new AppointmentStatsDTO(
                appointmentRepository.count(),
                appointmentRepository.countByStatus(AppointmentStatus.PENDING.name()),
                appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED.name()),
                appointmentRepository.countByStatus(AppointmentStatus.COMPLETED.name()),
                appointmentRepository.countByStatus(AppointmentStatus.CANCELLED.name())
        );
    }

    public void assertCanAccess(Long appointmentId, Long patientId, Long doctorId, boolean isAdmin) {
        Appointment appointment = findOrThrow(appointmentId);
        if (isAdmin) {
            return;
        }
        if (patientId != null && appointment.getPatient().getId() == patientId) {
            return;
        }
        if (doctorId != null && appointment.getDoctor().getId() == doctorId) {
            return;
        }
        throw new AppointmentException("Access denied to this appointment", HttpStatus.FORBIDDEN);
    }

    // --- Internal ---

    private List<Appointment> resolvePatientList(Long patientId, String status, String filter) {
        if (status != null && !status.isBlank()) {
            return appointmentRepository.findByPatientIdAndStatus(patientId, normalizeStatus(status));
        }
        if ("upcoming".equalsIgnoreCase(filter)) {
            return appointmentRepository.findByPatientIdAndScheduleTimeAfter(patientId, LocalDateTime.now());
        }
        if ("past".equalsIgnoreCase(filter)) {
            return appointmentRepository.findByPatientIdAndScheduleTimeBefore(patientId, LocalDateTime.now());
        }
        return appointmentRepository.findByPatientId(patientId);
    }

    private void assertDoctorAvailable(Long doctorId, LocalDateTime scheduleTime) {
        LocalDateTime end = scheduleTime.plusHours(SLOT_HOURS);
        List<Appointment> conflicts = appointmentRepository.findActiveConflicts(doctorId, scheduleTime, end);
        if (!conflicts.isEmpty()) {
            throw new AppointmentException("Doctor is not available at this time", HttpStatus.CONFLICT);
        }
    }

    private void applyTransition(
            Appointment appointment,
            AppointmentStatus target,
            AppointmentStatus.AppointmentActor actor) {
        AppointmentStatus current = AppointmentStatus.fromString(appointment.getStatus());
        if (!current.canTransitionTo(target, actor)) {
            throw new AppointmentException(
                    "Cannot change status from " + current + " to " + target + " as " + actor,
                    HttpStatus.BAD_REQUEST);
        }
        appointment.setStatus(target.name());
    }

    private Appointment findOrThrow(Long id) {
        Optional<Appointment> appointment = appointmentRepository.findById(id);
        return appointment.orElseThrow(
                () -> new AppointmentException("Appointment not found", HttpStatus.NOT_FOUND));
    }

    private void assertOwnershipPatient(Appointment appointment, Long patientId) {
        if (appointment.getPatient().getId() != patientId) {
            throw new AppointmentException("Access denied to this appointment", HttpStatus.FORBIDDEN);
        }
    }

    private void assertOwnershipDoctor(Appointment appointment, Long doctorId) {
        if (appointment.getDoctor().getId() != doctorId) {
            throw new AppointmentException("Access denied to this appointment", HttpStatus.FORBIDDEN);
        }
    }

    private String normalizeStatus(String status) {
        return AppointmentStatus.fromString(status).name();
    }
}
