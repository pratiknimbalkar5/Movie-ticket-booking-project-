package com.moviebooking.service;

import com.moviebooking.dto.request.BookingRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.dto.response.BookingSeatResponse;
import com.moviebooking.entity.Booking;
import com.moviebooking.entity.BookingSeat;
import com.moviebooking.entity.Seat;
import com.moviebooking.entity.Show;
import com.moviebooking.entity.User;
import com.moviebooking.exception.CancellationNotAllowedException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.exception.SeatAlreadyBookedException;
import com.moviebooking.exception.UnauthorizedAccessException;
import com.moviebooking.repository.BookingRepository;
import com.moviebooking.repository.BookingSeatRepository;
import com.moviebooking.repository.SeatRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    /**
     * Concurrency-safe ticket booking flow.
     * Uses pessimistic write locking and DB-level unique constraint.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse bookSeats(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + request.getShowId()));

        if (show.getStatus() != Show.ShowStatus.SCHEDULED) {
            throw new IllegalArgumentException("Cannot book tickets for a show that is not SCHEDULED");
        }

        if (show.getShowTime().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new IllegalArgumentException("Cannot book tickets for a show that has already concluded");
        }

        List<Long> seatIds = request.getSeatIds();
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }

        // Layer 1: Acquire Pessimistic Write Lock on the requested seats
        List<Seat> lockedSeats = seatRepository.findByIdInWithPessimisticLock(seatIds);
        if (lockedSeats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more selected seats do not exist");
        }

        // Verify seats belong to the screen showing this movie
        Long screenId = show.getScreen().getId();
        for (Seat seat : lockedSeats) {
            if (!Objects.equals(seat.getScreen().getId(), screenId)) {
                throw new IllegalArgumentException("Seat " + seat.getSeatNumber() + " does not belong to the screen for this show");
            }
        }

        // Check whether any of these seats are already booked for this show
        boolean anySeatBooked = bookingSeatRepository.existsByShowIdAndSeatIdIn(show.getId(), seatIds);
        if (anySeatBooked) {
            throw new SeatAlreadyBookedException("One or more selected seats have already been booked for this show");
        }

        // Calculate total amount based on seat tiers
        BigDecimal totalAmount = BigDecimal.ZERO;
        String seatSummary = lockedSeats.stream().map(Seat::getSeatNumber).collect(java.util.stream.Collectors.joining(", "));

        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .status(Booking.BookingStatus.CONFIRMED)
                .seatSummary(seatSummary)
                .bookingSeats(new ArrayList<>())
                .build();

        for (Seat seat : lockedSeats) {
            BigDecimal price = switch (seat.getSeatType()) {
                case REGULAR -> show.getBasePrice();
                case PREMIUM -> show.getPremiumPrice();
                case VIP -> show.getVipPrice();
            };
            totalAmount = totalAmount.add(price);

            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .show(show)
                    .price(price)
                    .build();
            booking.getBookingSeats().add(bookingSeat);
        }
        booking.setTotalAmount(totalAmount);

        // Layer 2: Save booking. CascadeType.ALL persists the booking and all its BookingSeats atomically.
        // The UNIQUE constraint (seat_id, show_id) in DB enforces strict concurrency safety.
        Booking savedBooking = bookingRepository.save(booking);

        return mapToBookingResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable).map(this::mapToBookingResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, Long userId, boolean isAdmin) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!isAdmin && !booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to view this booking");
        }

        return mapToBookingResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId, boolean isAdmin) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!isAdmin && !booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new CancellationNotAllowedException("This booking is already cancelled");
        }

        // Show must not have already started
        if (booking.getShow().getShowTime().isBefore(LocalDateTime.now())) {
            throw new CancellationNotAllowedException("Cannot cancel tickets for a show that has already started");
        }

        // Capture seat responses snapshot before clearing collection so history is preserved in response
        List<BookingSeatResponse> seatResponses = (booking.getBookingSeats() == null || booking.getBookingSeats().isEmpty())
                ? java.util.Collections.<BookingSeatResponse>emptyList()
                : booking.getBookingSeats().stream()
                        .map(bs -> BookingSeatResponse.builder()
                                .seatId(bs.getSeat() != null ? bs.getSeat().getId() : null)
                                .seatNumber(bs.getSeat() != null ? bs.getSeat().getSeatNumber() : null)
                                .seatType(bs.getSeat() != null ? bs.getSeat().getSeatType() : null)
                                .price(bs.getPrice())
                                .build())
                        .toList();

        // Release the seats so they become immediately bookable again.
        // Orphan removal cleanly deletes the booking_seats records, freeing the uk_seat_show unique constraint.
        if (booking.getBookingSeats() != null && !booking.getBookingSeats().isEmpty()) {
            booking.getBookingSeats().clear();
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancellationTime(LocalDateTime.now());
        bookingRepository.flush();

        BookingResponse response = mapToBookingResponse(booking);
        if (response.getBookedSeats() == null || response.getBookedSeats().isEmpty()) {
            response.setBookedSeats(seatResponses);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(this::mapToBookingResponse);
    }

    public BookingResponse mapToBookingResponse(Booking booking) {
        List<BookingSeatResponse> seatResponses = new ArrayList<>();
        if (booking.getBookingSeats() != null && !booking.getBookingSeats().isEmpty()) {
            for (BookingSeat bs : booking.getBookingSeats()) {
                if (bs.getSeat() != null) {
                    seatResponses.add(BookingSeatResponse.builder()
                            .seatId(bs.getSeat().getId())
                            .seatNumber(bs.getSeat().getSeatNumber())
                            .seatType(bs.getSeat().getSeatType())
                            .price(bs.getPrice())
                            .build());
                }
            }
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .username(booking.getUser().getUsername())
                .showId(booking.getShow().getId())
                .movieTitle(booking.getShow().getMovie().getTitle())
                .theatreName(booking.getShow().getScreen().getTheatre().getName())
                .screenName(booking.getShow().getScreen().getName())
                .showTime(booking.getShow().getShowTime())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .bookingTime(booking.getBookingTime())
                .cancellationTime(booking.getCancellationTime())
                .bookedSeats(seatResponses)
                .seatSummary(booking.getSeatSummary())
                .build();
    }
}
