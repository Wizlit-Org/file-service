package com.wizlit.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;

import com.wizlit.file.model.ResponseWithChange;
import com.wizlit.file.model.domain.FileDto;
import com.wizlit.file.service.FileService;
import com.wizlit.file.service.S3Service;
import com.wizlit.file.utils.PrivateAccess;
import com.wizlit.file.exception.ErrorResponse;

import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/file")
@Tag(name = "File", description = "File management APIs")
public class FileController {

    /**
     * Controller 규칙:
     * 1. service 만 호출
     */

    private final FileService fileService;
    private final S3Service s3Service;
    
    @Value("${aws.s3.url}")
    private String s3Url;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    @Operation(
        summary = "Upload a file",
        description = "Uploads a file and associates it with a user"
    )
    @ApiResponse(responseCode = "201", description = "File uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, MIN_LENGTH, MAX_LENGTH)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "File too large (ErrorCode: FILE_SIZE_LIMIT_EXCEEDED)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PrivateAccess
    public Mono<ResponseEntity<ResponseWithChange<FileDto>>> uploadFile(
        @Parameter(description = "ID of the user uploading the file") @RequestParam("uploader") Long uploaderUserId,
        @Parameter(description = "File to be uploaded") @RequestPart("file") FilePart file
    ) {
        return fileService.upload(file, uploaderUserId)
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.CREATED));
    }

    @Operation(
        summary = "Get and view a file",
        description = "Retrieves a file by ID, marks it as viewed, and redirects to the S3 bucket"
    )
    @ApiResponse(responseCode = "302", description = "Redirect to file in S3 bucket")
    @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT, INVALID_FILE_ID)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "File not found (ErrorCode: FILE_NOT_FOUND)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
        content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{fileId}")
    public Mono<ResponseEntity<Void>> getFile(
        @Parameter(description = "ID of the file to retrieve") @PathVariable String fileId
    ) {
        return fileService.getFileMetadataMarkViewed(fileId)
            .flatMap(dto -> Mono.just(ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(s3Service.generatePresignedUrl(s3Bucket, null, dto.getFullName(), dto.getFileType())))
                .build()
            ));
    }

}
