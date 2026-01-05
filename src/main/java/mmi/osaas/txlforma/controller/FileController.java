package mmi.osaas.txlforma.controller;

import lombok.RequiredArgsConstructor;
import mmi.osaas.txlforma.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/users")
    public ResponseEntity<String> uploadUserImage(@RequestParam("file") MultipartFile file) {
        String filePath = fileStorageService.storeFile(file, "users");
        String fileUrl = fileStorageService.getFileUrl(filePath);
        return ResponseEntity.ok(fileUrl);
    }

    @PostMapping("/upload/formations")
    public ResponseEntity<String> uploadFormationImage(@RequestParam("file") MultipartFile file) {
        String filePath = fileStorageService.storeFile(file, "formations");
        String fileUrl = fileStorageService.getFileUrl(filePath);
        return ResponseEntity.ok(fileUrl);
    }

    @PostMapping("/upload/categories")
    public ResponseEntity<String> uploadCategoryImage(@RequestParam("file") MultipartFile file) {
        String filePath = fileStorageService.storeFile(file, "categories");
        String fileUrl = fileStorageService.getFileUrl(filePath);
        return ResponseEntity.ok(fileUrl);
    }

    @GetMapping("/{subdirectory}/{filename:.+}")
    public ResponseEntity<Resource> getFile(
            @PathVariable String subdirectory,
            @PathVariable String filename) {
        String filePath = subdirectory + "/" + filename;
        Resource resource = fileStorageService.loadFileAsResource(filePath);
        
        String contentType = "application/octet-stream";
        try {
            String originalContentType = resource.getURL().openConnection().getContentType();
            if (originalContentType != null) {
                contentType = originalContentType;
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}



