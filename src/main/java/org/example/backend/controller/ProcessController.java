package org.example.backend.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Objects;

@RestController
@RequestMapping("/process")
public class ProcessController {
    @PostMapping
    public ResponseEntity<byte[]> processFile(@RequestParam("file") MultipartFile file) {
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

                String originalFileName = file.getOriginalFilename();
                String outputFileName = getFileNameWithExtension(originalFileName); // TODO: argument might be null

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

    private String getFileNameWithExtension(String originalFileName) {
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex != -1) {
            return originalFileName.substring(0, dotIndex) + ".mxl";
        }
        return originalFileName + ".mxl";
    }
}
