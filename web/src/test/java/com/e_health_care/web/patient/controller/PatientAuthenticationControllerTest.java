package com.e_health_care.web.patient.controller;

import com.e_health_care.web.patient.dto.PatientDTO;
import com.e_health_care.web.patient.service.PatientAuthenticationService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class PatientAuthenticationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PatientAuthenticationService authServicePatient;

    @InjectMocks
    private PatientAuthenticationController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // UTCID01: valid email + valid password → redirect:/patient/index + cookie set
    @Test
    void UTCID01_validEmail_validPassword_shouldRedirectToIndex() throws Exception {
        when(authServicePatient.verify(any(PatientDTO.class))).thenReturn("jwt-token-abc");

        mockMvc.perform(post("/patient/login")
                        .param("email", "test@gmail.com")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/index"))
                .andExpect(cookie().exists("jwt-patient-token"))
                .andExpect(cookie().value("jwt-patient-token", "jwt-token-abc"));
    }

    // UTCID02: valid email + wrong password → redirect:/patient/login?error
    @Test
    void UTCID02_validEmail_wrongPassword_shouldRedirectToLoginError() throws Exception {
        when(authServicePatient.verify(any(PatientDTO.class))).thenReturn(null);

        mockMvc.perform(post("/patient/login")
                        .param("email", "test@gmail.com")
                        .param("password", "wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/login?error"));
    }

    // UTCID03: not exist email + any password → redirect:/patient/login?error
    @Test
    void UTCID03_notExistEmail_anyPassword_shouldRedirectToLoginError() throws Exception {
        when(authServicePatient.verify(any(PatientDTO.class))).thenReturn(null);

        mockMvc.perform(post("/patient/login")
                        .param("email", "notexist@gmail.com")
                        .param("password", "anypassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/login?error"));
    }

    // UTCID04: blank email + valid password → redirect:/patient/login?error
    @Test
    void UTCID04_blankEmail_validPassword_shouldRedirectToLoginError() throws Exception {
        when(authServicePatient.verify(any(PatientDTO.class))).thenReturn(null);

        mockMvc.perform(post("/patient/login")
                        .param("email", "")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/login?error"));
    }

    // UTCID05: valid email + blank password → redirect:/patient/login?error
    @Test
    void UTCID05_validEmail_blankPassword_shouldRedirectToLoginError() throws Exception {
        when(authServicePatient.verify(any(PatientDTO.class))).thenReturn(null);

        mockMvc.perform(post("/patient/login")
                        .param("email", "test@gmail.com")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/login?error"));
    }

    // UTCID06: blank email + blank password → redirect:/patient/login?error
    @Test
    void UTCID06_blankEmail_blankPassword_shouldRedirectToLoginError() throws Exception {
        when(authServicePatient.verify(any(PatientDTO.class))).thenReturn(null);

        mockMvc.perform(post("/patient/login")
                        .param("email", "")
                        .param("password", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/login?error"));
    }
}