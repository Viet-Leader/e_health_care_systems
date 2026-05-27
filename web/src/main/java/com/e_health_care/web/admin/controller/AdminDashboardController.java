package com.e_health_care.web.admin.controller;

import com.e_health_care.web.admin.model.Admin;
import com.e_health_care.web.admin.repository.AdminRepository;
import com.e_health_care.web.admin.service.AdminManagementService; // NEW
import com.e_health_care.web.doctor.model.Doctor;
import com.e_health_care.web.doctor.repository.DoctorRepository;
import com.e_health_care.web.appointment.dto.AppointmentStatsDTO;
import com.e_health_care.web.patient.model.Patient;
import com.e_health_care.web.patient.repository.PatientRepository;
import com.e_health_care.web.patient.service.PatientAppointmentService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute; // NEW
import org.springframework.web.bind.annotation.PathVariable; // NEW
import org.springframework.web.bind.annotation.PostMapping; // NEW
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // NEW

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AdminRepository adminRepository;
    
    // NEW SERVICE INJECTION
    @Autowired
    private AdminManagementService adminManagementService;

    @Autowired
    private PatientAppointmentService patientAppointmentService; 

    private Admin getCurrentAdmin(Principal principal) {
        if (principal == null) return null;
        return adminRepository.findByEmail(principal.getName());
    }

    // Existing Dashboard Method
    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpServletRequest request, Principal principal) {
        Admin admin = getCurrentAdmin(principal);
        if (admin == null) return "redirect:admin/login";
        List<Doctor> doctors = doctorRepository.findAll();
        List<Patient> patients = patientRepository.findAll();
        model.addAttribute("doctors", doctors);
        model.addAttribute("patients", patients);
        AppointmentStatsDTO appointmentStats = patientAppointmentService.getStats();
        model.addAttribute("appointmentStats", appointmentStats);
        model.addAttribute("adminToken", request.getAttribute("adminToken"));
        model.addAttribute("admin", admin);
        return "admin/admin-dashboard";
    }

    // --- DOCTOR MANAGEMENT ENDPOINTS ---

    // Delete Doctor
    @PostMapping("/doctor/delete/{id}")
    public String deleteDoctor(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            adminManagementService.deleteDoctor(id);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor with ID " + id + " deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting doctor with ID " + id + ".");
        }
        return "redirect:/admin/dashboard";
    }

    // Show Doctor Edit Form
    @GetMapping("/doctor/edit/{id}")
    public String showEditDoctorForm(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        return adminManagementService.getDoctorById(id).map(doctor -> {
            model.addAttribute("doctor", doctor);
            return "admin/edit-doctor";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Doctor not found.");
            return "redirect:/admin/dashboard";
        });
    }
    
    // Handle Doctor Info Update (Non-password fields)
    @PostMapping("/doctor/update/info")
    public String updateDoctorInformation(@ModelAttribute Doctor doctor, RedirectAttributes redirectAttributes) {
        try {
            // Fetch existing doctor to preserve password and role
            Doctor existingDoctor = adminManagementService.getDoctorById(doctor.getId()).orElse(null);
            if (existingDoctor != null) {
                doctor.setPassword(existingDoctor.getPassword());
                doctor.setROLE(existingDoctor.getROLE());
                adminManagementService.updateDoctorInformation(doctor);
                redirectAttributes.addFlashAttribute("successMessage", "Doctor information updated successfully.");
            } else {
                 redirectAttributes.addFlashAttribute("errorMessage", "Doctor not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating doctor information.");
        }
        return "redirect:/admin/dashboard";
    }

    // Handle Doctor Password Update
    @PostMapping("/doctor/update/password/{id}")
    public String updateDoctorPassword(@PathVariable long id, @ModelAttribute("newPassword") String newPassword, RedirectAttributes redirectAttributes) {
        if (adminManagementService.updateDoctorPassword(id, newPassword)) {
            redirectAttributes.addFlashAttribute("successMessage", "Doctor password updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Doctor not found or error updating password.");
        }
        // Redirect back to the edit page to show status
        return "redirect:/admin/doctor/edit/" + id;
    }


    // --- PATIENT MANAGEMENT ENDPOINTS ---

    // Delete Patient
    @PostMapping("/patient/delete/{id}")
    public String deletePatient(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            adminManagementService.deletePatient(id);
            redirectAttributes.addFlashAttribute("successMessage", "Patient with ID " + id + " deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting patient with ID " + id + ".");
        }
        return "redirect:/admin/dashboard";
    }

    // Show Patient Edit Form
    @GetMapping("/patient/edit/{id}")
    public String showEditPatientForm(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        return adminManagementService.getPatientById(id).map(patient -> {
            model.addAttribute("patient", patient);
            return "admin/edit-patient";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Patient not found.");
            return "redirect:/admin/dashboard";
        });
    }

    // Handle Patient Info Update (Non-password fields)
    @PostMapping("/patient/update/info")
    public String updatePatientInformation(@ModelAttribute Patient patient, RedirectAttributes redirectAttributes) {
        try {
            // Fetch existing patient to preserve password
            Patient existingPatient = adminManagementService.getPatientById(patient.getId()).orElse(null);
            if (existingPatient != null) {
                patient.setPassword(existingPatient.getPassword());
                adminManagementService.updatePatientInformation(patient);
                redirectAttributes.addFlashAttribute("successMessage", "Patient information updated successfully.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Patient not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating patient information.");
        }
        return "redirect:/admin/dashboard";
    }

    // Handle Patient Password Update
    @PostMapping("/patient/update/password/{id}")
    public String updatePatientPassword(@PathVariable long id, @ModelAttribute("newPassword") String newPassword, RedirectAttributes redirectAttributes) {
        if (adminManagementService.updatePatientPassword(id, newPassword)) {
            redirectAttributes.addFlashAttribute("successMessage", "Patient password updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Patient not found or error updating password.");
        }
        // Redirect back to the edit page to show status
        return "redirect:/admin/patient/edit/" + id;
    }
}