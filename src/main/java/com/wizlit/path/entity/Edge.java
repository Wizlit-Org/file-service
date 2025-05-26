package com.wizlit.path.entity;

import lombok.*;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("edge")
public class Edge {
    @Column("origin_point")
    private Long originPoint;

    @Column("destination_point")
    private Long destinationPoint;

    // Composite primary key
    public static class EdgeId {
        private Long originPoint;
        private Long destinationPoint;
    }
}