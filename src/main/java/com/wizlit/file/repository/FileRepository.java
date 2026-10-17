package com.wizlit.file.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.wizlit.file.entity.File;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;

public interface FileRepository extends ReactiveCrudRepository<File, String> {
    Mono<File> findByFileHash(String fileHash);
    Flux<File> findAllByMemoId(Long memoId);

    @Modifying
    @Query("UPDATE file SET memo_id = :memoId, status = 1 WHERE file_id IN (:fileIds)")
    Mono<Integer> updateAttachFiles(Long memoId, Collection<String> fileIds);

    @Modifying
    @Query("DELETE FROM file WHERE memo_id = :memoId")
    Mono<Integer> deleteAllByMemoId(Long memoId);

    @Query("SELECT * FROM file WHERE status = 0 AND file_created_timestamp < :cutoff")
    Flux<File> findPendingFilesBefore(Instant cutoff);

    @Modifying
    @Query("DELETE FROM file WHERE file_id IN (:fileIds)")
    Mono<Integer> deleteFilesByIds(Collection<String> fileIds);
}
