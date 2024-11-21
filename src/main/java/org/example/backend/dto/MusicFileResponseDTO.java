package org.example.backend.dto;

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
    private UUID id;
    @NotNull
    private String name;
    @NotNull
    private String url;
    @NotNull
    private LocalDateTime date;
}
