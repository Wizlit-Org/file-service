package com.wizlit.file.repository;

import com.wizlit.file.entity.UserStorageQuota;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserStorageQuotaRepository extends ReactiveCrudRepository<UserStorageQuota, Long> {

    @Query("SELECT * FROM user_storage_quota WHERE user_id = :userId AND year_month = :yearMonth")
    Mono<UserStorageQuota> findByUserIdAndYearMonth(Long userId, String yearMonth);

    @Modifying
    @Query("INSERT INTO user_storage_quota (user_id, year_month, used_bytes, max_bytes, updated_timestamp) " +
           "VALUES (:userId, :yearMonth, :addBytes, :maxBytes, CURRENT_TIMESTAMP) " +
           "ON CONFLICT (user_id, year_month) " +
           "DO UPDATE SET used_bytes = user_storage_quota.used_bytes + EXCLUDED.used_bytes, updated_timestamp = CURRENT_TIMESTAMP")
    Mono<Integer> upsertQuota(Long userId, String yearMonth, Long addBytes, Long maxBytes);
}
