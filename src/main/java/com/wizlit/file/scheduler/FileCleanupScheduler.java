package com.wizlit.file.scheduler;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wizlit.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final FileService fileService;

    /**
     * Run orphan file GC every day at 04:00 AM.
     * Deletes PENDING (status=0) files older than 24 hours.
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanupOrphanFiles() {
        log.info("[OrphanGC] Scheduled cleanup started for orphan files older than 24 hours.");
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        fileService.cleanupOrphanFiles(cutoff)
            .subscribe(
                deletedCount -> log.info("[OrphanGC] Scheduled cleanup completed. Deleted {} orphan files.", deletedCount),
                error -> log.error("[OrphanGC] Scheduled cleanup failed: {}", error.getMessage(), error)
            );
    }
}
