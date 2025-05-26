package com.wizlit.path.service.manager;

import java.time.Instant;
import java.util.List;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.wizlit.path.entity.Point;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.model.domain.PointDto;
import com.wizlit.path.repository.PointRepository;
import com.wizlit.path.utils.Validator;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * PointManager is a component responsible for managing point-related operations.
 * It handles point creation, updates, deletion, and memo management in a reactive manner.
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
public class PointManager {

    private final PointRepository pointRepository;
    
    @Value("${app.point.max-memos:15}")
    private Integer MAX_MEMOS_PER_POINT;
    
    /**
     * Gets multiple points with their ordered memos.
     *
     * @param pointIds List of point IDs to get
     * @param updatedAfter The timestamp to check against
     * @return A Flux of PointDto with ordered memos
     */
    public Flux<PointDto> getFullPoints(List<Long> pointIds, Instant updatedAfter) {
        if (pointIds == null || pointIds.isEmpty()) {
            return Flux.empty();
        }
        return pointRepository.findFullPointsByIds(pointIds, updatedAfter)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.POINT_NOT_FOUND, pointIds),
                    "foreign", "key", "point"
                )
                .toException());
    }

    public Mono<PointDto> getFullPoint(Long pointId, Instant updatedAfter) {
        return pointRepository.existsById(pointId)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new ApiException(ErrorCode.POINT_NOT_FOUND, pointId));
                }
                return getFullPoints(List.of(pointId), updatedAfter)
                    .next();
            });
    }

    /**
     * Validates that all specified points exist.
     *
     * @param pointIds Array of point IDs to validate
     * @return A Mono containing true if all points exist
     * @throws ApiException with ErrorCode.NON_EXISTENT_POINTS if any point doesn't exist
     */
    public Mono<Boolean> validatePointsExist(Long... pointIds) {
        return pointRepository.existsByIdIn(List.of(pointIds))
            .map(exists -> (Boolean) exists)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NON_EXISTENT_POINTS, Arrays.toString(pointIds)),
                    "foreign", "key", "point"
                )
                .toException())
            .flatMap(exists -> {
                if (Boolean.FALSE.equals(exists)) {
                    return Mono.error(new ApiException(ErrorCode.NON_EXISTENT_POINTS, Arrays.toString(pointIds)));
                }
                return Mono.just(true);
            });
    }
    
    /**
     * Saves a point to the repository.
     *
     * @param point The point to save
     * @return A Mono containing the saved Point
     * @throws ApiException with ErrorCode.POINT_NAME_DUPLICATED if a point with the same title exists
     */
    private Mono<Point> _savePoint(Point point) {
        if (point.getPointTitle() != null) point.setPointTitle(point.getPointTitle().trim());
        if (point.getPointSummary() != null) point.setPointSummary(point.getPointSummary().trim());
        point.setPointUpdatedTimestamp(Instant.now());

        return pointRepository.save(point)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.POINT_NAME_DUPLICATED, point.getPointTitle()),
                    "unique", "key", "point_title"
                )
                .containsAllElseError(
                    new ApiException(ErrorCode.NULL_INPUT, List.of("point_title")),
                    "point_title", "not-null"
                )
                .toException());
    }

    public Mono<Point> createPoint(Point newPoint) {
        newPoint.setPointId(null);
        newPoint.setPointCreatedTimestamp(Instant.now());
        return _savePoint(newPoint);
    }
    
    public Mono<Point> updatePoint(Long pointId, String pointTitle, String pointSummary) {
        if (pointId == null) {
            return Mono.error(new ApiException(ErrorCode.NULL_INPUT, "pointId"));
        }

        return pointRepository.findById(pointId)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.POINT_NOT_FOUND, pointId),
                    "foreign", "key", "point"
                )
                .toException())
            .switchIfEmpty(Mono.error(new ApiException(ErrorCode.POINT_NOT_FOUND, pointId)))
            .flatMap(existingPoint -> {
                if (pointTitle != null) existingPoint.setPointTitle(pointTitle);
                if (pointSummary != null) existingPoint.setPointSummary(pointSummary);
                return _savePoint(existingPoint);
            });
    }

    private Mono<Point> _updatePointTimestamp(Long pointId) {
        return updatePoint(pointId, null, null);
    }

    /**
     * Deletes a point.
     *
     * @param pointId The ID of the point to delete
     * @return A Mono that completes when the point is deleted
     * @throws ApiException with ErrorCode.POINT_NOT_DELETABLE if the point has associated memos
     */
    public Mono<Void> deletePoint(Long pointId) {
        return pointRepository.deleteById(pointId)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.POINT_NOT_DELETABLE, pointId, "point should be empty. move or delete all memos"),
                    "delete", "foreign", "key", "memo"
                )
                .containsAllElseError(
                    new ApiException(ErrorCode.POINT_NOT_DELETABLE, pointId, "point should be empty. move or delete all edges"),
                    "delete", "foreign", "key", "edge"
                )
                .toException());
    }

}