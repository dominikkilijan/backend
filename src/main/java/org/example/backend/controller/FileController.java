package org.example.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/process/files")
public class FileController {

    private final String baseDirectory = "C:/Users/domin/OneDrive/Pulpit/Inzynierka/backend/uploads";

    @GetMapping("/{folderId}/{fileName}")
    public ResponseEntity<?> getFile(
            @PathVariable String folderId,
            @PathVariable String fileName) {

        Path filePath = Paths.get(baseDirectory, folderId, fileName);

        if (!Files.exists(filePath)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found.");
        }

        try {
            byte[] fileContent = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading file.");
        }
    }
}
