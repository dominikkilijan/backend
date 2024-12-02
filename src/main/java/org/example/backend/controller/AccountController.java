package org.example.backend.controller;

import org.example.backend.dto.MusicFileResponseDTO;
import org.example.backend.model.MusicFile;
import org.example.backend.model.User;
import org.example.backend.repository.MusicFileRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final UserRepository userRepository;
    private final MusicFileRepository musicFileRepository;

    public AccountController(UserRepository userRepository, MusicFileRepository musicFileRepository) {
        this.userRepository = userRepository;
        this.musicFileRepository = musicFileRepository;
    }

    @GetMapping
    public ResponseEntity<?> getUserFiles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) authentication.getPrincipal();

        Optional<User> userOptional = userRepository.findById(UUID.fromString(userId));
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<MusicFileResponseDTO> userFiles = user.getMusicFiles()
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(userFiles);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found.");
        }
    }

    @DeleteMapping("/delete-file")
    public ResponseEntity<?> deleteFile(@RequestParam("fileId") UUID fileId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) authentication.getPrincipal();

        Optional<MusicFile> fileOptional = musicFileRepository.findById(fileId);

        if (fileOptional.isPresent()) {
            MusicFile musicFile = fileOptional.get();

            if (!musicFile.getUser().getId().toString().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to delete this file.");
            }

            try {
                String uploadsDirectory = "C:/Users/domin/OneDrive/Pulpit/Inzynierka/backend/uploads";
                String relativePath = musicFile.getUrl().split("/process/files")[1];
                Path filePath = Paths.get(uploadsDirectory, relativePath);

                Files.deleteIfExists(filePath);

                musicFileRepository.delete(musicFile);

                return ResponseEntity.ok("File deleted successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete the file from the filesystem.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File with the provided ID does not exist.");
        }
    }


    private MusicFileResponseDTO convertToDTO(MusicFile musicFile) {
        return new MusicFileResponseDTO(
                musicFile.getId(),
                musicFile.getName(),
                musicFile.getUrl(),
                musicFile.getDate()
        );
    }
}
