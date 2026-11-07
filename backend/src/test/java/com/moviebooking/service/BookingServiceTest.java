package com.moviebooking.service;

import com.moviebooking.dto.request.BookingRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.entity.*;
import com.moviebooking.exception.CancellationNotAllowedException;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.exception.SeatAlreadyBookedException;
import com.moviebooking.exception.UnauthorizedAccessException;
import com.moviebooking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private ShowRepository showRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Show testShow;
    private Screen testScreen;
    private Theatre testTheatre;
    private Movie testMovie;
    private Seat testSeat1;
    private Seat testSeat2;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .role(User.Role.USER)
                .build();

        testMovie = Movie.builder()
                .id(10L)
                .title("Interstellar")
                .durationMinutes(169)
                .build();

        testTheatre = Theatre.builder()
                .id(20L)
                .name("PVR Cinemas")
                .city("Bengaluru")
                .build();

        testScreen = Screen.builder()
                .id(30L)
                .name("Screen 1")
                .theatre(testTheatre)
                .build();

        testShow = Show.builder()
                .id(100L)
                .movie(testMovie)
                .screen(testScreen)
                .showTime(LocalDateTime.now().plusDays(1))
                .basePrice(new BigDecimal("200.00"))
                .premiumPrice(new BigDecimal("350.00"))
                .vipPrice(new BigDecimal("500.00"))
                .status(Show.ShowStatus.SCHEDULED)
                .build();

        testSeat1 = Seat.builder()
                .id(101L)
                .screen(testScreen)
                .seatNumber("A1")
                .seatType(Seat.SeatType.REGULAR)
                .build();

        testSeat2 = Seat.builder()
                .id(102L)
                .screen(testScreen)
                .seatNumber("B1")
                .seatType(Seat.SeatType.PREMIUM)
                .build();
    }

    @Test
    @DisplayName("Should successfully book available seats and calculate correct total amount")
    void testBookSeats_Success() {
        BookingRequest request = BookingRequest.builder()
                .showId(100L)
                .seatIds(List.of(101L, 102L))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(showRepository.findById(100L)).thenReturn(Optional.of(testShow));
        when(seatRepository.findByIdInWithPessimisticLock(List.of(101L, 102L)))
                .thenReturn(List.of(testSeat1, testSeat2));
        when(bookingSeatRepository.existsByShowIdAndSeatIdIn(100L, List.of(101L, 102L))).thenReturn(false);

        Booking savedBooking = Booking.builder()
                .id(500L)
                .user(testUser)
                .show(testShow)
                .status(Booking.BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("550.00"))
                .bookingSeats(new ArrayList<>())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.bookSeats(1L, request);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(new BigDecimal("550.00"), response.getTotalAmount());
        assertEquals(Booking.BookingStatus.CONFIRMED, response.getStatus());

        verify(seatRepository).findByIdInWithPessimisticLock(List.of(101L, 102L));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw SeatAlreadyBookedException when seats are already occupied")
    void testBookSeats_AlreadyBooked_ThrowsException() {
        BookingRequest request = BookingRequest.builder()
                .showId(100L)
                .seatIds(List.of(101L))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(showRepository.findById(100L)).thenReturn(Optional.of(testShow));
        when(seatRepository.findByIdInWithPessimisticLock(List.of(101L))).thenReturn(List.of(testSeat1));
        when(bookingSeatRepository.existsByShowIdAndSeatIdIn(100L, List.of(101L))).thenReturn(true);

        assertThrows(SeatAlreadyBookedException.class, () -> bookingService.bookSeats(1L, request));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw CancellationNotAllowedException when show has already started")
    void testCancelBooking_ShowAlreadyStarted_ThrowsException() {
        testShow.setShowTime(LocalDateTime.now().minusMinutes(10)); // 10 minutes in the past

        Booking booking = Booking.builder()
                .id(600L)
                .user(testUser)
                .show(testShow)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByIdWithDetails(600L)).thenReturn(Optional.of(booking));

        assertThrows(CancellationNotAllowedException.class, () ->
                bookingService.cancelBooking(600L, 1L, false)
        );
    }

    @Test
    @DisplayName("Should throw CancellationNotAllowedException when booking is already cancelled (duplicate cancellation)")
    void testCancelBooking_AlreadyCancelled_ThrowsException() {
        Booking booking = Booking.builder()
                .id(600L)
                .user(testUser)
                .show(testShow)
                .status(Booking.BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findByIdWithDetails(600L)).thenReturn(Optional.of(booking));

        assertThrows(CancellationNotAllowedException.class, () ->
                bookingService.cancelBooking(600L, 1L, false)
        );
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when user attempts to cancel someone else's booking")
    void testCancelBooking_UnauthorizedUser_ThrowsException() {
        Booking booking = Booking.builder()
                .id(600L)
                .user(testUser) // owned by userId = 1L
                .show(testShow)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByIdWithDetails(600L)).thenReturn(Optional.of(booking));

        // Attempting to cancel with userId = 2L and isAdmin = false
        assertThrows(UnauthorizedAccessException.class, () ->
                bookingService.cancelBooking(600L, 2L, false)
        );
    }

    @Test
    @DisplayName("Should successfully cancel booking if eligible and free up seats via orphan removal")
    void testCancelBooking_Success() {
        testShow.setShowTime(LocalDateTime.now().plusHours(5)); // 5 hours away

        Booking booking = Booking.builder()
                .id(600L)
                .user(testUser)
                .show(testShow)
                .status(Booking.BookingStatus.CONFIRMED)
                .bookingSeats(new ArrayList<>())
                .build();

        when(bookingRepository.findByIdWithDetails(600L)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancelBooking(600L, 1L, false);

        assertNotNull(response);
        assertEquals(Booking.BookingStatus.CANCELLED, booking.getStatus());
        assertNotNull(booking.getCancellationTime());
        verify(bookingRepository).flush();
    }
}
