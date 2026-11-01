package org.nikahgator.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A nikah as the imam enters and sees it. {@code id}, {@code status} and {@code createdAt} are ignored on create. */
public record NikahDto(
        Long id,
        String status,
        @NotBlank String groomName, String groomFatherName, String groomEmail, String groomPhone,
        @NotBlank String brideName, String brideFatherName, String brideEmail, String bridePhone,
        String waliName, String waliRelation, String witness1Name, String witness2Name,
        String mahrDescription,
        @NotNull LocalDateTime nikahAt,
        String notes,
        LocalDateTime createdAt) {
}
