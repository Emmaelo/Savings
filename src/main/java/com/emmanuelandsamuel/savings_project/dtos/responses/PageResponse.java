package com.emmanuelandsamuel.savings_project.dtos.responses;

import java.util.List;

import com.emmanuelandsamuel.savings_project.entities.Group;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PageResponse<T> {
   private List<Group> content;

    private int page;

    private int size;

    private int totalPages;

    private long totalElements;
}
