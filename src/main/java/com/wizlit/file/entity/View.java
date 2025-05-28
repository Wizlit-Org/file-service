package com.wizlit.file.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("view")
public class View implements Persistable<String> {

    @Id
    @Column("file_id")
    private String fileId; // pk
    
    @Column("view_count")
    private Long viewCount;

    @Column("last_viewed_timestamp")
    private Instant lastViewedTimestamp;

    // This new field tells us if the record is new
    @Transient
    @Builder.Default
    private boolean isNewEntry = true;

    @Override
    public String getId() {
        return fileId;
    }

    // This method checks if the record is new
    public boolean isNew() {
        return isNewEntry;
    }
}