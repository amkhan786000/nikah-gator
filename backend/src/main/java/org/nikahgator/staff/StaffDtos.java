package org.nikahgator.staff;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class StaffDtos {
    private StaffDtos() {
    }

    /** {@code permissions} maps a service key to VIEW or MANAGE; a service that is missing (or NONE) is not granted. */
    public record CreateStaff(@NotBlank @Size(max = 120) String fullName, @NotBlank @Email String email,
                              @Size(max = 30) String phone, @Size(max = 80) String jobTitle,
                              Map<String, String> permissions) {
    }

    /** The e-mail (the sign-in) cannot be changed. */
    public record UpdateStaff(@NotBlank @Size(max = 120) String fullName, @Size(max = 30) String phone,
                              @Size(max = 80) String jobTitle, Map<String, String> permissions) {
    }

    public record StaffView(Long id, String fullName, String email, String phone, String jobTitle, boolean enabled,
                            boolean mustChangePassword, Map<String, String> permissions, boolean credentialsEmailed,
                            LocalDateTime createdAt) {
    }
}
