package com.e_health_care.web.patient.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.e_health_care.web.patient.dto.PatientDTO;
import com.e_health_care.web.patient.service.PatientAuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/patient")
public class PatientAuthenticationController {

    @Autowired
    private PatientAuthenticationService authServicePatient;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("patient", new PatientDTO());
        // SỬA: Thêm "patient/" vào trước tên file
        return "patient/patient-register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("patient") PatientDTO patientDTO, Model model) {
        try {
            authServicePatient.register(patientDTO);
            return "redirect:/patient/login";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("errorMessage", "Email này đã được đăng ký!");
            return "patient/patient-register";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Không thể đăng ký: " + e.getMessage());
            return "patient/patient-register";
        }
    }

    @GetMapping("/success")
    public String successForm() {
        // SỬA: Thêm "patient/"
        return "patient/patient-index";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("patient", new PatientDTO());
        // SỬA: Thêm "patient/" để tìm file templates/patient/patient-login.html
        return "patient/patient-login";
    }

    @PostMapping("/login")
    public String handleLogin(@ModelAttribute PatientDTO patientDTO, HttpServletResponse response) {
        String token = authServicePatient.verify(patientDTO);

        if (token != null) {
            Cookie cookie = new Cookie("jwt-patient-token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);
            return "redirect:/patient/index";
        } else {
            return "redirect:/patient/login?error";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt-patient-token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/patient/login?logout";
    }
}