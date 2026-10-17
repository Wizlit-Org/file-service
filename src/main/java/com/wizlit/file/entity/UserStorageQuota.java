package com.wizlit.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("user_storage_quota")
public class UserStorageQuota {

    @Id
    @Column("user_id")
    private Long userId;

    @Column("year_month")
    private String yearMonth;

    @Column("used_bytes")
    private Long usedBytes;

    @Column("max_bytes")
    private Long maxBytes;

    @Column("updated_timestamp")
    private Instant updatedTimestamp;
}
