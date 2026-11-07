package com.moviebooking.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moviebooking.entity.Seat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Long id;
    private String seatNumber;
    private Seat.SeatType seatType;
    
    @JsonProperty("isAvailable")
    private boolean isAvailable;
    
    private BigDecimal price;

    @JsonProperty("available")
    public boolean getAvailable() {
        return isAvailable;
    }
}
