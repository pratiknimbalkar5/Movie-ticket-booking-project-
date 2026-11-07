package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CastMemberDto {
    private Long id;
    private String name;
    private String character;
    private String profileUrl;
    private Integer order;
}
