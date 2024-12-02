package org.example.backend.controller;

import org.example.backend.model.MusicFile;
import org.example.backend.model.User;
import org.example.backend.repository.MusicFileRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public ProcessController(UserRepository userRepository, MusicFileRepository musicFileRepository) {
        this.userRepository = userRepository;
        this.musicFileRepository = musicFileRepository;
    }

    @PostMapping
    public ResponseEntity<byte[]> processFile(@RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = null;

            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                userId = (String) authentication.getPrincipal();
            }

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

                if (userId != null) {
                    Optional<User> userOptional = userRepository.findById(UUID.fromString(userId));
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();

                        UUID fileId = UUID.randomUUID();
                        String fileExtension = ".mxl";
                        String fileNameWithoutExtension = getFileNameWithoutExtension(file.getOriginalFilename());
                        String outputFileName = fileId + fileExtension;

                        Path uploadPath = Paths.get(UPLOAD_DIR, userId);
                        if (!Files.exists(uploadPath)) {
                            Files.createDirectories(uploadPath);
                        }
                        Path outputPath = uploadPath.resolve(outputFileName);
                        Files.write(outputPath, fileContent);

                        String fileUrl = "http://localhost:8080/process/files/" + userId + "/" + outputFileName;

                        MusicFile musicFile = new MusicFile();
                        musicFile.setName(fileNameWithoutExtension);
                        musicFile.setUrl(fileUrl);
                        musicFile.setDate(LocalDateTime.now());
                        musicFile.setUser(user);

                        musicFileRepository.save(musicFile);
                    }
                }

                String fileNameWithoutExtension = getFileNameWithoutExtension(file.getOriginalFilename());
                String fileExtension = ".mxl";
                String outputFileName = fileNameWithoutExtension + fileExtension;

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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/files/{fileName}")
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = (String) authentication.getPrincipal();

            Path filePath = Paths.get(UPLOAD_DIR, userId, fileName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Resource fileResource = new FileSystemResource(filePath);

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
