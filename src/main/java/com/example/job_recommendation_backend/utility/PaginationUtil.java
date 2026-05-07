package com.example.job_recommendation_backend.utility;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PaginationUtil {

    public Pageable getPageable(int page, int size) {
        size = Math.min(size, 50);
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
}