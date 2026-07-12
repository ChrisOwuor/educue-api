package com.owuor.educue.common.controller;

import com.owuor.educue.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/{folder}/{applicationId}/{filename:.+}")
    public ResponseEntity<Resource> viewFile(
            @PathVariable String folder,
            @PathVariable String applicationId,
            @PathVariable String filename
    ) {

        String key =
                folder +
                        "/" +
                        applicationId +
                        "/" +
                        filename;

        Resource resource =
                fileStorageService.load(key);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline"
                )
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        fileStorageService.getContentType(key)
                )
                .body(resource);
    }
}
