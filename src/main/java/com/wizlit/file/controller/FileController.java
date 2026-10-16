package com.wizlit.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;

import com.wizlit.file.model.ResponseWithChange;
import com.wizlit.file.model.domain.AttachFilesRequest;
import com.wizlit.file.model.domain.FileDto;
import com.wizlit.file.model.domain.UserQuotaResponse;
import com.wizlit.file.service.FileService;
import com.wizlit.file.service.S3Service;
import com.wizlit.file.utils.PrivateAccess;
import com.wizlit.file.exception.ErrorResponse;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/file", "/api/files"})
@Tag(name = "File", description = "File management APIs")
public class FileController {

    /**
     * Controller 규칙:
     * 1. service 만 호출
     */

    private final FileService fileService;
    private final S3Service s3Service;

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

    @Operation(
        summary = "Download raw file binary",
        description = "Streams raw binary content of the file with appropriate Content-Type"
    )
    @ApiResponse(responseCode = "200", description = "Raw file binary returned successfully")
    @ApiResponse(responseCode = "404", description = "File not found")
    @GetMapping(value = {"/{fileId}/download", "/{fileId}/raw"})
    public Mono<ResponseEntity<byte[]>> downloadFile(
        @Parameter(description = "ID of the file to download") @PathVariable String fileId
    ) {
        return fileService.getFileMetadataMarkViewed(fileId)
            .publishOn(Schedulers.boundedElastic())
            .map(dto -> {
                byte[] data = s3Service.download(s3Bucket, dto.getFullName());
                MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
                if (dto.getFileType() != null && !dto.getFileType().isBlank()) {
                    try {
                        mediaType = MediaType.parseMediaType(dto.getFileType());
                    } catch (Exception ignored) {}
                }
                return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(data.length)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(data);
            });
    }

    @Operation(
        summary = "Attach files to memo",
        description = "Associates uploaded files with a memo and transitions their status to ACTIVE (1)"
    )
    @ApiResponse(responseCode = "200", description = "Files attached successfully")
    @PostMapping(
        value = "/attach",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Map<String, Object>>> attachFiles(
        @RequestBody AttachFilesRequest request
    ) {
        return fileService.attachFiles(request.getMemoId(), request.getFileIds())
            .thenReturn(ResponseEntity.ok(Map.of("success", true, "memoId", request.getMemoId())));
    }

    @Operation(
        summary = "Delete files by memo ID",
        description = "Deletes all files associated with a memo from S3/R2 and database"
    )
    @ApiResponse(responseCode = "200", description = "Files deleted successfully")
    @DeleteMapping(
        value = "/memo/{memoId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Map<String, Object>>> deleteFilesByMemoId(
        @Parameter(description = "ID of the memo") @PathVariable Long memoId
    ) {
        return fileService.deleteFilesByMemoId(memoId)
            .thenReturn(ResponseEntity.ok(Map.of("success", true, "deletedMemoId", memoId)));
    }

    @Operation(
        summary = "Run orphan file GC",
        description = "Immediately runs orphan file cleanup for PENDING files older than 24 hours"
    )
    @ApiResponse(responseCode = "200", description = "GC executed successfully")
    @PostMapping(
        value = "/gc/run",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<Map<String, Object>>> runOrphanGc() {
        return fileService.cleanupOrphanFiles(null)
            .map(deletedCount -> ResponseEntity.ok(Map.of("deletedCount", (Object) deletedCount, "success", true)));
    }

    @Operation(
        summary = "Get user storage quota for current month",
        description = "Retrieves current month storage usage and remaining quota for a user"
    )
    @ApiResponse(responseCode = "200", description = "User quota retrieved successfully")
    @GetMapping(
        value = "/quota/{userId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<UserQuotaResponse>> getUserQuota(
        @Parameter(description = "ID of the user") @PathVariable Long userId
    ) {
        return fileService.getUserQuota(userId)
            .map(ResponseEntity::ok);
    }

}
