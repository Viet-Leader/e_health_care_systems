package com.e_health_care.web.security;

import com.e_health_care.web.admin.service.AdminDetailsService;
import com.e_health_care.web.admin.service.AdminJwtService;
import com.e_health_care.web.doctor.service.DoctorDetailsService;
import com.e_health_care.web.doctor.service.DoctorJwtService;
import com.e_health_care.web.patient.service.PatientDetailsService;
import com.e_health_care.web.patient.service.PatientJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Xác thực JWT đa vai trò cho Appointment Flow API (Patient / Doctor / Admin).
 */
@Component
public class AppointmentApiAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private PatientJwtService patientJwtService;

    @Autowired
    private DoctorJwtService doctorJwtService;

    @Autowired
    private AdminJwtService adminJwtService;

    @Autowired
    private PatientDetailsService patientDetailsService;

    @Autowired
    private DoctorDetailsService doctorDetailsService;

    @Autowired
    private AdminDetailsService adminDetailsService;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/appointments");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> token = JwtTokenResolver.resolve(
                request,
                "jwt-patient-token",
                "jwt-doctor-token",
                "jwt-admin-token");

        token.ifPresent(jwt -> {
            if (!tryPatientAuth(request, jwt) && !tryDoctorAuth(request, jwt)) {
                tryAdminAuth(request, jwt);
            }
        });
        filterChain.doFilter(request, response);
    }

    private boolean tryPatientAuth(HttpServletRequest request, String jwt) {
        try {
            String email = patientJwtService.extractEmail(jwt);
            UserDetails user = patientDetailsService.loadUserByUsername(email);
            if (patientJwtService.validateToken(jwt, user)) {
                setAuthentication(request, user);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean tryDoctorAuth(HttpServletRequest request, String jwt) {
        try {
            String email = doctorJwtService.extractEmail(jwt);
            UserDetails user = doctorDetailsService.loadUserByUsername(email);
            if (doctorJwtService.validateToken(jwt, user)) {
                setAuthentication(request, user);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private boolean tryAdminAuth(HttpServletRequest request, String jwt) {
        try {
            String email = adminJwtService.extractEmail(jwt);
            UserDetails user = adminDetailsService.loadUserByUsername(email);
            if (adminJwtService.validateToken(jwt, user)) {
                setAuthentication(request, user);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
