package com.e_health_care.web.appointment.controller;

import com.e_health_care.web.admin.model.Admin;
import com.e_health_care.web.admin.model.AdminPrinciple;
import com.e_health_care.web.admin.repository.AdminRepository;
import com.e_health_care.web.appointment.dto.AppointmentBookRequestDTO;
import com.e_health_care.web.appointment.dto.AppointmentResponseDTO;
import com.e_health_care.web.appointment.dto.AppointmentStatsDTO;
import com.e_health_care.web.appointment.dto.AppointmentStatusUpdateDTO;
import com.e_health_care.web.appointment.exception.AppointmentException;
import com.e_health_care.web.doctor.model.Doctor;
import com.e_health_care.web.doctor.model.DoctorPrinciple;
import com.e_health_care.web.doctor.repository.DoctorRepository;
import com.e_health_care.web.patient.model.PatientPrinciple;
import com.e_health_care.web.patient.service.PatientAppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Appointment Flow API (cross-role) — PAT-05/06, DOC-03, ADM-02 (SRS).
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {

    @Autowired
    private PatientAppointmentService appointmentService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AdminRepository adminRepository;

    /** PAT-05: Đặt lịch khám */
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> book(@Valid @RequestBody AppointmentBookRequestDTO request) {
        Long patientId = requirePatientId();
        var saved = appointmentService.bookAppointment(patientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppointmentResponseDTO.from(saved));
    }

    /** ADM-02: Thống kê cuộc hẹn trên dashboard */
    @GetMapping("/stats")
    public AppointmentStatsDTO stats() {
        return appointmentService.getStats();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("service", "appointment-flow-api");
        body.put("status", "ok");
        return body;
    }

    /** PAT-06 / DOC-03 / ADM-02: Danh sách cuộc hẹn theo vai trò */
    @GetMapping
    public List<AppointmentResponseDTO> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String filter) {
        RoleContext ctx = resolveRoleContext();
        return switch (ctx.role()) {
            case PATIENT -> appointmentService.listForPatient(ctx.patientId(), status, filter);
            case DOCTOR -> appointmentService.listForDoctor(ctx.doctorId(), status);
            case ADMIN -> appointmentService.listAll(status);
        };
    }

    /** Chi tiết một cuộc hẹn (chỉ chủ sở hữu hoặc admin) */
    @GetMapping("/{id}")
    public AppointmentResponseDTO getById(@PathVariable Long id) {
        RoleContext ctx = resolveRoleContext();
        appointmentService.assertCanAccess(id, ctx.patientId(), ctx.doctorId(), ctx.role() == Role.ADMIN);
        return appointmentService.getById(id);
    }

    /** DOC-03 / PAT-06: Cập nhật trạng thái */
    @PatchMapping("/{id}/status")
    public AppointmentResponseDTO updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateDTO body) {
        RoleContext ctx = resolveRoleContext();
        return switch (ctx.role()) {
            case PATIENT -> appointmentService.updateStatusAsPatient(id, ctx.patientId(), body.getStatus());
            case DOCTOR -> AppointmentResponseDTO.from(
                    appointmentService.updateStatusAsDoctor(id, ctx.doctorId(), body.getStatus()));
            default -> throw new AppointmentException("Only patient or doctor can update status", HttpStatus.FORBIDDEN);
        };
    }

    /** ADM: Hủy cuộc hẹn (giám sát) */
    @DeleteMapping("/{id}")
    public AppointmentResponseDTO cancelByAdmin(@PathVariable Long id) {
        if (resolveRoleContext().role() != Role.ADMIN) {
            throw new AppointmentException("Admin only", HttpStatus.FORBIDDEN);
        }
        return appointmentService.cancelAsAdmin(id);
    }

    // --- Auth helpers ---

    private Long requirePatientId() {
        RoleContext ctx = resolveRoleContext();
        if (ctx.role() != Role.PATIENT || ctx.patientId() == null) {
            throw new AppointmentException("Patient authentication required", HttpStatus.UNAUTHORIZED);
        }
        return ctx.patientId();
    }

    private RoleContext resolveRoleContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AppointmentException("Authentication required", HttpStatus.UNAUTHORIZED);
        }

        if (auth.getPrincipal() instanceof PatientPrinciple patientPrinciple) {
            return new RoleContext(Role.PATIENT, patientPrinciple.getPatient().getId(), null);
        }
        if (auth.getPrincipal() instanceof DoctorPrinciple doctorPrinciple) {
            return new RoleContext(Role.DOCTOR, null, doctorPrinciple.getDoctor().getId());
        }
        if (auth.getPrincipal() instanceof AdminPrinciple) {
            return new RoleContext(Role.ADMIN, null, null);
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_ADMIN".equals(a));
        if (isAdmin) {
            return new RoleContext(Role.ADMIN, null, null);
        }

        // Doctor đăng nhập qua email (fallback giống MVC controller)
        String email = auth.getName();
        Doctor doctor = doctorRepository.findByEmail(email);
        if (doctor != null) {
            return new RoleContext(Role.DOCTOR, null, doctor.getId());
        }
        Admin admin = adminRepository.findByEmail(email);
        if (admin != null) {
            return new RoleContext(Role.ADMIN, null, null);
        }

        throw new AppointmentException("Unsupported role for appointment API", HttpStatus.FORBIDDEN);
    }

    private enum Role {
        PATIENT, DOCTOR, ADMIN
    }

    private record RoleContext(Role role, Long patientId, Long doctorId) {
    }
}
