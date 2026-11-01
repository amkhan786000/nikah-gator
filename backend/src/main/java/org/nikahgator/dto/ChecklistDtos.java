package org.nikahgator.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChecklistDtos {
    public record ItemDto(Long id, String code, String label, boolean required, boolean done, LocalDateTime doneAt, boolean custom) {}

    public record NewItem(@NotBlank @Size(max = 200) String label) {}

    /** Only the fields that are set are changed. */
    public record ItemUpdate(Boolean done, Boolean required) {}

    public record DocumentDto(Long id, String fileName, String contentType, Long sizeBytes, Long checklistId, LocalDateTime createdAt) {}

    public record Progress(long nikahId, int done, int total) {}
}
