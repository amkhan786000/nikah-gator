package org.nikahgator.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AdminDtos {
    private AdminDtos() {
    }

    /** A new masjid and the imam who will manage it. */
    public record CreateMasjidRequest(
            @NotBlank String name,
            String addressLine, String city, String state, String zip, String phone, String email,
            String timezone,
            @NotBlank String imamName,
            @NotBlank @Email String imamEmail,
            String imamPhone) {
    }

    public record MasjidView(Long id, String name, String city, String state, boolean active, LocalDateTime createdAt,
                             Long imamId, String imamName, String imamEmail, boolean imamMustChangePassword,
                             boolean credentialsEmailed) {
    }
}
