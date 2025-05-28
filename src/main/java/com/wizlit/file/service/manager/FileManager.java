package com.wizlit.file.service.manager;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.wizlit.file.entity.File;
import com.wizlit.file.entity.View;
import com.wizlit.file.repository.FileRepository;
import com.wizlit.file.repository.ViewRepository;
import com.wizlit.file.utils.Validator;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;

/**
 * ProjectManager is a component responsible for managing project-related operations.
 * It handles project retrieval, point management, and project updates in a reactive manner.
 * 
 * This manager follows strict rules for repository usage and error handling:
 * 1. Each repository should be used exclusively within this manager
 * 2. Repository operations should be called exactly once per method
 * 3. All repository operations must include error mapping using Validator
 * 4. No direct calls to other helpers or services are allowed
 * 5. Methods should not return DTOs directly
 * 
 * Manager 규칙:
 * 1. 동일한 repository 가 다른 곳에서도 쓰이면 안됨
 * 2. repository 의 각 기능은 반드시 한 번만 호출
 * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
 * 4. 다른 helper 나 service 호출 금지
 * 5. DTO 반환 금지
 */
@Component
@RequiredArgsConstructor
public class FileManager {

    private final FileRepository fileRepository;
    private final ViewRepository viewRepository;

    /**
     * Find file by hash
     * 
     * @param fileHash file hash
     * @return file
     */
    public Mono<File> findByHash(String fileHash) {
        return fileRepository.findByFileHash(fileHash);
    }

    /**
     * Insert file metadata
     * 
     * @param file file
     * @param uploader uploader
     * @return file metadata
     */
    public Mono<File> insertFileMetadata(
        Long fileSize,
        String fileType,
        String fileExtension,
        String fileHash,
        Long uploader
    ) {
        File file = File.builder()
            .fileSize(fileSize)
            .fileType(fileType)
            .fileUploader(uploader)
            .fileType(fileType)
            .fileExtension(fileExtension)
            .fileHash(fileHash)
            .build();

        return fileRepository.save(file)
            .flatMap(f -> fileRepository.findById(f.getFileId()))
            .onErrorMap(error -> Validator.from(error)
                .toException());
    }

    /**
     * Get file metadata
     * 
     * @param fileId file id
     * @return file metadata
     */
    public Mono<File> getFileMetadata(String fileId) {
        return fileRepository.findById(fileId)
            .onErrorMap(error -> Validator.from(error)
                .toException());
    }

    /**
     * Increment view count and update last viewed timestamp
     * 
     * @param fileId file id
     * @return file view
     */
    public Mono<View> fileViewed(String fileId) {
        return viewRepository.findById(fileId)
            .flatMap(view -> {
                view.setViewCount(view.getViewCount() + 1);
                view.setLastViewedTimestamp(Instant.now());
                view.setNewEntry(false);  // Mark as existing record
                return viewRepository.save(view);
            })
            .switchIfEmpty(
                viewRepository.save(
                    View.builder()
                        .fileId(fileId)
                        .viewCount(1L)
                        .lastViewedTimestamp(Instant.now())
                        .isNewEntry(true)  // Mark as new record
                        .build()
                )
            )
            .onErrorMap(error -> Validator.from(error)
                .toException());
    }

}