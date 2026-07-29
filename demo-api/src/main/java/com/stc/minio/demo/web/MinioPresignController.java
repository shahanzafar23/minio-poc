package com.stc.minio.demo.web;

import com.stc.minio.demo.dto.PresignRequest;
import com.stc.minio.demo.dto.PresignResponse;
import com.stc.minio.demo.service.PresignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MinioPresignController {

    private static final String PUBLIC_PREFIX = "public/";
    private static final String PRIVATE_PREFIX = "private/";

    private final PresignService presignService;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/v1/public/minio/upload")
    public ResponseEntity<PresignResponse> publicUpload(@Valid @RequestBody PresignRequest request) {
        return ResponseEntity.ok(presignService.presignPut(request, PUBLIC_PREFIX, true));
    }

    @PostMapping("/v1/public/minio/download")
    public ResponseEntity<PresignResponse> publicDownload(@Valid @RequestBody PresignRequest request) {
        return ResponseEntity.ok(presignService.presignGet(request, PUBLIC_PREFIX, true));
    }

    @PostMapping("/v1/public/minio/delete")
    public ResponseEntity<PresignResponse> publicDelete(@Valid @RequestBody PresignRequest request) {
        return ResponseEntity.ok(presignService.presignDelete(request, PUBLIC_PREFIX, true));
    }

    @PostMapping("/v1/private/minio/upload")
    public ResponseEntity<PresignResponse> privateUpload(@Valid @RequestBody PresignRequest request) {
        return ResponseEntity.ok(presignService.presignPut(request, PRIVATE_PREFIX, false));
    }

    @PostMapping("/v1/private/minio/download")
    public ResponseEntity<PresignResponse> privateDownload(@Valid @RequestBody PresignRequest request) {
        return ResponseEntity.ok(presignService.presignGet(request, PRIVATE_PREFIX, false));
    }

    @PostMapping("/v1/private/minio/delete")
    public ResponseEntity<PresignResponse> privateDelete(@Valid @RequestBody PresignRequest request) {
        return ResponseEntity.ok(presignService.presignDelete(request, PRIVATE_PREFIX, false));
    }
}
