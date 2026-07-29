package com.stc.minio.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PresignRequest {

    @NotBlank
    private String objectKey;

    private String contentType;

    @Positive
    private Long expirySeconds;
}
