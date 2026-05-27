package com.e_health_care.web.appointment;

import java.util.Set;

/**
 * Trạng thái cuộc hẹn theo SRS (PAT-06, DOC-03):
 * PENDING (Chờ duyệt), CONFIRMED (Đã duyệt), COMPLETED (Đã hoàn thành), CANCELLED (Đã hủy).
 */
public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    public static AppointmentStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        return AppointmentStatus.valueOf(value.trim().toUpperCase());
    }

    public boolean canTransitionTo(AppointmentStatus target, AppointmentActor actor) {
        if (this == CANCELLED || this == COMPLETED) {
            return false;
        }
        return switch (actor) {
            case PATIENT -> this == PENDING && target == CANCELLED;
            case DOCTOR -> switch (this) {
                case PENDING -> target == CONFIRMED || target == CANCELLED;
                case CONFIRMED -> target == COMPLETED || target == CANCELLED;
                default -> false;
            };
            case ADMIN -> target == CANCELLED;
        };
    }

    public enum AppointmentActor {
        PATIENT, DOCTOR, ADMIN
    }

    public static final Set<AppointmentStatus> ACTIVE_STATUSES = Set.of(PENDING, CONFIRMED);
}
