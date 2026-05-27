package com.e_health_care.web.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Order(-1)
public class AppointmentApiSecurityConfiguration {

    @Autowired
    private AppointmentApiAuthenticationFilter appointmentApiAuthenticationFilter;

    @Bean
    public SecurityFilterChain appointmentApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/appointments/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/appointments/health").permitAll()
                        .requestMatchers("/api/appointments/stats").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/appointments")
                        .hasAuthority("PATIENT")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/appointments/*/status")
                        .hasAnyAuthority("PATIENT", "ROLE_DOCTOR")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/appointments/*")
                        .hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/appointments/**")
                        .hasAnyAuthority("PATIENT", "ROLE_DOCTOR", "ROLE_ADMIN"))
                .addFilterBefore(appointmentApiAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
