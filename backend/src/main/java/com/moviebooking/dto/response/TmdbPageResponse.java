package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbPageResponse<T> {
    private int page;
    private int totalPages;
    private int totalResults;
    private List<T> results;
    private boolean cached;
}
