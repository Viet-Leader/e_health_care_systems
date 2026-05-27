package com.e_health_care.web.patient.repository;

import com.e_health_care.web.patient.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PatientAppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndScheduleTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctor.id = :doctorId
              AND a.scheduleTime >= :start AND a.scheduleTime < :end
              AND a.status IN ('PENDING', 'CONFIRMED')
            """)
    List<Appointment> findActiveConflicts(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByStatus(String status);

    List<Appointment> findByPatientIdAndStatus(Long patientId, String status);

    List<Appointment> findByDoctorIdAndStatus(Long doctorId, String status);

    List<Appointment> findByPatientIdAndScheduleTimeAfter(Long patientId, LocalDateTime time);

    List<Appointment> findByPatientIdAndScheduleTimeBefore(Long patientId, LocalDateTime time);

    long countByStatus(String status);
}
