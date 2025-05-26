package com.wizlit.path.model.response;

import com.wizlit.path.model.domain.*;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FinalResponse {
    private Long currentId;
    private List<ProjectDto> projects;
    private List<PointDto> points;
    private List<UserDto> users;

    public FinalResponse forGetProject(Long currentId, ProjectDto project, List<PointDto> points) {
        this.currentId = currentId;
        this.projects = List.of(project);
        this.points = points;
        return this;
    }

    public FinalResponse forGetPoint(Long currentId, PointDto point, List<UserDto> users) {
        this.currentId = currentId;
        this.points = List.of(point);
        this.users = users;
        return this;
    }

    public FinalResponse forOnlyPoint(Long currentId, PointDto point) {
        this.currentId = currentId;
        this.points = List.of(point);
        return this;
    }
}


