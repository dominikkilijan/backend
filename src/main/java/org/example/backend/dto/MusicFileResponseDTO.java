package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class MusicFileResponseDTO {
    @NotNull
    @NotBlank
    private UUID id;
    @NotNull
    @NotBlank
    private String name;
    @NotNull
    @NotBlank
    private String url;
    @NotNull
    @NotBlank
    private LocalDateTime date;
}
