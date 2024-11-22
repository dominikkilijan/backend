package org.example.backend.controller;

import org.example.backend.model.MusicFile;
import org.example.backend.model.User;
import org.example.backend.repository.MusicFileRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/process")
public class ProcessController {

    private final UserRepository userRepository;
    private final MusicFileRepository musicFileRepository;

    private static final String UPLOAD_DIR = "uploads";

    // Constructor for dependency injection
    public ProcessController(UserRepository userRepository, MusicFileRepository musicFileRepository) {
        this.userRepository = userRepository;
        this.musicFileRepository = musicFileRepository;
    }

    @PostMapping
    public ResponseEntity<byte[]> processFile(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "userId", required = false) UUID userId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String audiverisUrl = "http://localhost:8000/process";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            File uploadedFile = convertMultipartToFile(file);
            body.add("file", new FileSystemResource(uploadedFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Resource> response = restTemplate.exchange(
                    audiverisUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Resource.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                InputStream inputStream = response.getBody().getInputStream();
                byte[] fileContent = inputStream.readAllBytes();

                // Generate a unique ID for the file
                UUID fileId = UUID.randomUUID();
                String fileExtension = ".mxl";
                String fileNameWithoutExtension = getFileNameWithoutExtension(file.getOriginalFilename());
                String outputFileName = fileId + fileExtension;

                // Save file only if userId is provided and valid
                if (userId != null) {
                    Optional<User> userOptional = userRepository.findById(userId);
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();

                        // Save file to user-specific folder
                        Path uploadPath = Paths.get(UPLOAD_DIR, userId.toString());
                        if (!Files.exists(uploadPath)) {
                            Files.createDirectories(uploadPath);
                        }
                        Path outputPath = uploadPath.resolve(outputFileName);
                        Files.write(outputPath, fileContent);

                        // Generate full file URL
                        String fileUrl = "http://localhost:8080/process/files/" + userId + "/" + outputFileName;

                        // Save metadata to the database
                        MusicFile musicFile = new MusicFile();
                        musicFile.setName(fileNameWithoutExtension);
                        musicFile.setUrl(fileUrl);
                        musicFile.setDate(LocalDateTime.now());
                        musicFile.setUser(user);

                        musicFileRepository.save(musicFile);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(("Invalid userId").getBytes());
                    }
                }

                // Return the processed file as response
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                responseHeaders.setContentDisposition(ContentDisposition.builder("attachment")
                        .filename(outputFileName)
                        .build());
                responseHeaders.add("Access-Control-Expose-Headers", "Content-Disposition");

                return new ResponseEntity<>(fileContent, responseHeaders, HttpStatus.OK);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/files/{userId}/{fileName}")
    public ResponseEntity<Resource> serveFile(@PathVariable UUID userId, @PathVariable String fileName) {
        try {
            // Lokalizacja pliku
            Path filePath = Paths.get(UPLOAD_DIR, userId.toString(), fileName);

            // Sprawdzenie, czy plik istnieje
            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Tworzenie zasobu pliku
            Resource fileResource = new FileSystemResource(filePath);

            // Nagłówki odpowiedzi
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(fileName)
                    .build());
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            return new ResponseEntity<>(fileResource, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/HT")
    public String healthCheck() {
        return "OK";
    }

    private File convertMultipartToFile(MultipartFile file) throws IOException {
        File convFile = new File(Objects.requireNonNull(file.getOriginalFilename()));
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    private String getFileNameWithoutExtension(String originalFileName) {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        }
        return originalFileName;
    }
}
