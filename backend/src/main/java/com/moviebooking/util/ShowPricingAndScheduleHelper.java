package com.moviebooking.util;

import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Screen;
import com.moviebooking.entity.Show;
import com.moviebooking.entity.Theatre;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ShowPricingAndScheduleHelper {

    private ShowPricingAndScheduleHelper() {}

    public record PricingTier(BigDecimal basePrice, BigDecimal premiumPrice, BigDecimal vipPrice) {}

    /**
     * Calculates realistic, distinct pricing per theatre chain, screen technology, and time slot.
     */
    public static PricingTier calculatePricing(Theatre theatre, Screen screen, int slotIndex) {
        String tName = "";
        Long theatreId = 1L;
        Long screenId = 1L;
        String sName = "";

        try {
            if (theatre != null) {
                theatreId = theatre.getId() != null ? theatre.getId() : 1L;
                tName = theatre.getName() != null ? theatre.getName().toLowerCase() : "";
            }
        } catch (Exception ignored) {}

        try {
            if (screen != null) {
                screenId = screen.getId() != null ? screen.getId() : 1L;
                sName = screen.getName() != null ? screen.getName().toLowerCase() : "";
            }
        } catch (Exception ignored) {}

        // Chain base price by brand prestige
        int chainBase;
        if (tName.contains("director's cut") || tName.contains("insignia") || tName.contains("gold")) {
            chainBase = 440;
        } else if (tName.contains("pvr")) {
            chainBase = 260;
        } else if (tName.contains("cinepolis")) {
            chainBase = 240;
        } else if (tName.contains("inox")) {
            chainBase = 210;
        } else if (tName.contains("miraj") || tName.contains("wave") || tName.contains("carnival")) {
            chainBase = 180;
        } else {
            chainBase = 195;
        }

        // Format technology surcharge
        int formatExtra = 0;
        if (sName.contains("imax")) {
            formatExtra = 90;
        } else if (sName.contains("4dx")) {
            formatExtra = 70;
        } else if (sName.contains("dolby") || sName.contains("atmos")) {
            formatExtra = 40;
        }

        // Unique multiplex variance per theatre: ensures neighbouring theatres in the same city NEVER have identical prices
        int theatreVariance = (((int) ((theatreId * 31L) % 7)) - 3) * 15; // -45, -30, -15, 0, +15, +30, +45
        int screenVariance = ((int) (screenId % 3L)) * 20;               // 0, 20, 40

        int base = chainBase + formatExtra + theatreVariance + screenVariance;

        // Slot adjustments (Morning discount / Prime evening peak surcharge)
        if (slotIndex == 0) {
            base = Math.max(140, base - 30);
        } else if (slotIndex == 3 || slotIndex == 4) {
            base += 35;
        }

        int premium = (int) Math.round(base * 1.45 / 10.0) * 10;
        int vip = (int) Math.round(base * 2.35 / 10.0) * 10;

        return new PricingTier(
                BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(premium).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(vip).setScale(2, RoundingMode.HALF_UP)
        );
    }

    /**
     * Staggers showtimes realistically so every cinema in the city has distinct schedules.
     */
    public static LocalDateTime calculateShowTime(LocalDate showDate, int dayOffset, int slotIndex, Theatre theatre, Screen screen) {
        Long theatreId = 1L;
        Long screenId = 1L;

        try {
            if (theatre != null && theatre.getId() != null) {
                theatreId = theatre.getId();
            }
        } catch (Exception ignored) {}

        try {
            if (screen != null && screen.getId() != null) {
                screenId = screen.getId();
            }
        } catch (Exception ignored) {}

        // Each theatre has a staggered start offset (0, 15, 30, 45, 60, 75 mins)
        int theatreMinutesOffset = (int) ((theatreId * 17L) % 6) * 15;
        // Each screen within the theatre is staggered by 35 minutes so screens never start simultaneously
        int screenMinutesOffset = (int) ((screenId % 3L) * 35);
        int totalOffset = theatreMinutesOffset + screenMinutesOffset;

        // Standard schedule base slots across the day
        int baseHour = switch (slotIndex) {
            case 0 -> 9;
            case 1 -> 12;
            case 2 -> 16;
            case 3 -> 19;
            default -> 22;
        };
        int baseMin = switch (slotIndex) {
            case 0 -> 15;
            case 1 -> 45;
            case 2 -> 15;
            case 3 -> 45;
            default -> 30;
        };

        LocalDateTime standardTime = showDate.atTime(baseHour, baseMin).plusMinutes(totalOffset);

        if (dayOffset == 0) {
            LocalDateTime now = LocalDateTime.now();
            if (standardTime.isBefore(now.plusMinutes(10))) {
                // If this slot has passed today, schedule it in upcoming evening/night hours so today is always bookable
                int futureDelay = 20 + theatreMinutesOffset + (slotIndex * 80) + screenMinutesOffset;
                return now.plusMinutes(futureDelay).withSecond(0).withNano(0);
            }
            return standardTime.withSecond(0).withNano(0);
        } else {
            return standardTime.withSecond(0).withNano(0);
        }
    }

    /**
     * Creates a full set of realistic shows for given screens and live movies across specified days.
     */
    public static List<Show> generateShows(List<Screen> screens, List<Movie> liveMovies, int daysAhead) {
        List<Show> result = new ArrayList<>();
        if (screens == null || screens.isEmpty() || liveMovies == null || liveMovies.isEmpty()) {
            return result;
        }

        LocalDate today = LocalDate.now();
        for (int dayOffset = 0; dayOffset < daysAhead; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);

            for (int i = 0; i < screens.size(); i++) {
                Screen screen = screens.get(i);
                Theatre theatre = screen.getTheatre();

                // 5 staggered show slots per screen per day
                for (int slot = 0; slot < 5; slot++) {
                    int movieIdx = (i * 3 + dayOffset * 2 + slot) % liveMovies.size();
                    Movie movie = liveMovies.get(movieIdx);

                    LocalDateTime showTime = calculateShowTime(date, dayOffset, slot, theatre, screen);
                    PricingTier pricing = calculatePricing(theatre, screen, slot);

                    result.add(Show.builder()
                            .movie(movie)
                            .screen(screen)
                            .showTime(showTime)
                            .basePrice(pricing.basePrice())
                            .premiumPrice(pricing.premiumPrice())
                            .vipPrice(pricing.vipPrice())
                            .status(Show.ShowStatus.SCHEDULED)
                            .build());
                }
            }
        }
        return result;
    }
}

