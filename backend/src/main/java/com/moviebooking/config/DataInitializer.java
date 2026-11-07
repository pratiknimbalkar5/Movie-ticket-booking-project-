package com.moviebooking.config;

import com.moviebooking.entity.*;
import com.moviebooking.repository.*;
import com.moviebooking.service.TmdbMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final PasswordEncoder passwordEncoder;
    private final TmdbMovieService tmdbMovieService;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            // 1. Seed Users (Admin & Regular User)
            if (userRepository.count() == 0) {
                log.info("Seeding initial authentication users...");
                User admin = User.builder()
                        .username("admin")
                        .email("admin@moviebooking.com")
                        .passwordHash(passwordEncoder.encode("Admin@123"))
                        .role(User.Role.ADMIN)
                        .build();
                userRepository.save(admin);

                User testUser = User.builder()
                        .username("john_doe")
                        .email("john@example.com")
                        .passwordHash(passwordEncoder.encode("User@123"))
                        .role(User.Role.USER)
                        .build();
                userRepository.save(testUser);

                log.info("Users seeded: 'admin' (Admin@123) and 'john_doe' (User@123)");
            }

            // 2. Mark any outdated movies from 2024 or earlier as inactive without deleting bookings
            List<Movie> staleMovies = movieRepository.findAll().stream()
                    .filter(m -> m.getReleaseDate() != null && m.getReleaseDate().isBefore(LocalDate.of(2025, 1, 1)) && Boolean.TRUE.equals(m.getIsActive()))
                    .toList();
            if (!staleMovies.isEmpty()) {
                log.info("Setting {} outdated movies from 2024 or earlier to inactive...", staleMovies.size());
                staleMovies.forEach(m -> m.setIsActive(false));
                movieRepository.saveAll(staleMovies);
            }

            // 3. Seed Fresh 2025-2026 Live Now-Showing and Upcoming Movies
            if (movieRepository.count() < 18) {
                log.info("Seeding fresh 2025-2026 live in-cinemas and upcoming Indian blockbusters catalog...");

                List<Movie> freshCatalog = List.of(
                        // LIVE IN CINEMAS (NOW PLAYING)
                        Movie.builder()
                                .title("Clayface")
                                .tmdbId(1400940L)
                                .description("A former actor turned tragic shapeshifter infiltrates high society to uncover a vast conspiracy, defying heroes and villains alike in a battle of identity.")
                                .genre("Action / Sci-Fi / Thriller")
                                .durationMinutes(132)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(5))
                                .posterUrl("https://image.tmdb.org/t/p/w500/3Yk8rZz6K29D5T5OQ3b8F9s7a6z.jpg")
                                .rating(7.6)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Hope")
                                .tmdbId(1058424L)
                                .description("Residents of a remote harbor town discover a mysterious extraterrestrial presence in the misty outskirts, igniting a high-stakes survival struggle.")
                                .genre("Sci-Fi / Mystery / Action")
                                .durationMinutes(125)
                                .language("English / Korean / Hindi")
                                .releaseDate(LocalDate.now().minusDays(3))
                                .posterUrl("https://image.tmdb.org/t/p/w500/qqHFLg90IUSoTmSMpKOOW5RyqN3.jpg")
                                .rating(7.8)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Jailer 2")
                                .description("Tiger Muthuvel Pandian is drawn out of peaceful retirement into an international syndicate confrontation spanning Tamil Nadu, Karnataka, and Mumbai.")
                                .genre("Action / Crime / Drama")
                                .durationMinutes(168)
                                .language("Tamil / Telugu / Hindi")
                                .releaseDate(LocalDate.now().minusDays(10))
                                .posterUrl("https://image.tmdb.org/t/p/w500/eSpGBv7gDk3P4Xm5gU8X490v5Yn.jpg")
                                .rating(8.2)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Drishyam: The Conclusion")
                                .description("Vijay Salgaonkar faces his ultimate cerebral face-off with police investigators as decades-buried secrets resurface in Goa.")
                                .genre("Thriller / Drama")
                                .durationMinutes(152)
                                .language("Hindi")
                                .releaseDate(LocalDate.now().minusDays(22))
                                .posterUrl("https://image.tmdb.org/t/p/w500/gO9FhR37f0UdfT7P5H67uM3R2wK.jpg")
                                .rating(7.9)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Fall 2: Deadpoint")
                                .description("Thrill-seekers scale the most lethal mountain face in the southern hemisphere, facing sheer vertical terror when their descent equipment fails.")
                                .genre("Thriller / Adventure")
                                .durationMinutes(108)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(15))
                                .posterUrl("https://image.tmdb.org/t/p/w500/aK7pL5rQ8YdE3B2w8C6vJ3r9xYl.jpg")
                                .rating(7.3)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Street Fighter")
                                .tmdbId(1153576L)
                                .description("World-class martial artists clash in an elite clandestine global tournament organized by the enigmatic Shadaloo empire.")
                                .genre("Action / Martial Arts")
                                .durationMinutes(120)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(9))
                                .posterUrl("https://image.tmdb.org/t/p/w500/vGXptEdgZIhPg3cGlc7e8sNPC2e.jpg")
                                .rating(6.8)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Toxic")
                                .description("A fairy tale for grown-ups: An international cartel kingpin weaves through the underbelly of drug empires and vengeance across coastal India.")
                                .genre("Action / Crime Thriller")
                                .durationMinutes(160)
                                .language("Kannada / Hindi / Telugu / Tamil")
                                .releaseDate(LocalDate.now().minusDays(18))
                                .posterUrl("https://image.tmdb.org/t/p/w500/6izwz7rsy95ARzTR3poZ8H6c5pp.jpg")
                                .rating(8.1)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Runner")
                                .description("An underground courier in Mumbai's neon labyrinth has ninety minutes to deliver an encrypted drive across the city before hitmen intercept.")
                                .genre("Action / Thriller")
                                .durationMinutes(114)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(7))
                                .posterUrl("https://image.tmdb.org/t/p/w500/nfnhwfUEFuSOxxf4jDdBlY6Lccw.jpg")
                                .rating(8.4)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Resident Evil")
                                .description("In the depths of an abandoned bio-research metropolis, a specialized tactical team encounters the origin strain of Umbrella's catastrophic viral mutations.")
                                .genre("Sci-Fi / Horror / Action")
                                .durationMinutes(118)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(14))
                                .posterUrl("https://image.tmdb.org/t/p/w500/2uSWRTtCG336nuBiG8jOTEUKSy8.jpg")
                                .rating(7.4)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Dhurandhar")
                                .description("An undercover RAW intelligence team infiltrates hostile territory on a lethal counter-terrorism mission involving international espionage.")
                                .genre("Action / Espionage Thriller")
                                .durationMinutes(155)
                                .language("Hindi")
                                .releaseDate(LocalDate.now().minusDays(4))
                                .posterUrl("https://image.tmdb.org/t/p/w500/1T21FblunT0y8fz7YaW8JMYgUKm.jpg")
                                .rating(7.7)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Reporting Live")
                                .description("An investigative broadcast journalist breaks a sensational corporate conspiracy on prime time television, only to find herself relentlessly pursued.")
                                .genre("Thriller / Mystery")
                                .durationMinutes(126)
                                .language("Hindi")
                                .releaseDate(LocalDate.now().minusDays(16))
                                .posterUrl("https://image.tmdb.org/t/p/w500/3AfHD1HoaQpQwKH8kxRdBKVmzeU.jpg")
                                .rating(7.1)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("The Paradise")
                                .description("A wealthy resort island in the Indian Ocean becomes an arena of life and death when an armed syndicate takes holidaymakers hostage.")
                                .genre("Action / Suspense")
                                .durationMinutes(130)
                                .language("Telugu / Hindi")
                                .releaseDate(LocalDate.now().minusDays(20))
                                .posterUrl("https://image.tmdb.org/t/p/w500/2cxhvwyEwRlysAmRH4iodkvo0z5.jpg")
                                .rating(7.2)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),

                        // UPCOMING RELEASES (COMING SOON)
                        Movie.builder()
                                .title("Ramayana: Rise of a Legend")
                                .tmdbId(656908L)
                                .description("Nitesh Tiwari's grand visual spectacle bringing India's timeless epic to cinema screens with groundbreaking IMAX 3D technology.")
                                .genre("Mythology / Epic Action")
                                .durationMinutes(185)
                                .language("Hindi / Telugu / Tamil")
                                .releaseDate(LocalDate.now().plusDays(12))
                                .posterUrl("https://image.tmdb.org/t/p/w500/rstcAnBeCkxNQjNp3YXrF6IP1tW.jpg")
                                .rating(8.9)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Wild Horse Nine")
                                .tmdbId(891621L)
                                .description("A desert frontier legend of nine renegade cavalrymen embarking on a perilous rescue crossing unexplored canyon territory.")
                                .genre("Adventure / Drama")
                                .durationMinutes(138)
                                .language("English")
                                .releaseDate(LocalDate.now().plusDays(14))
                                .posterUrl("https://image.tmdb.org/t/p/w500/wTnV3PCVW5O92JMrFvvrRcV39RU.jpg")
                                .rating(8.6)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("War 2")
                                .description("Major Kabir Dhaliwal and Agent Vikram engage in a pulse-pounding clash across Spain, Tokyo, and Ladakh in Yash Raj Films' Spy Universe.")
                                .genre("Action / Spy Thriller")
                                .durationMinutes(165)
                                .language("Hindi / Telugu / Tamil")
                                .releaseDate(LocalDate.now().plusDays(25))
                                .posterUrl("https://image.tmdb.org/t/p/w500/2JbNkHg8m7LaBy61LyrnnlenaxY.jpg")
                                .rating(8.5)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Superman")
                                .description("Clark Kent reconciles his Kryptonian heritage with his small-town Kansas human upbringing as the champion of truth and justice.")
                                .genre("Action / Sci-Fi")
                                .durationMinutes(145)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().plusDays(40))
                                .posterUrl("https://image.tmdb.org/t/p/w500/ldyfo0BKmz5rWtJJKCvwaNS4cJT.jpg")
                                .rating(8.7)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Avatar: Fire and Ash")
                                .description("Jake Sully and Neytiri encounter the fierce and volcanic Ash People clan of Pandora, challenging everything they fought to preserve.")
                                .genre("Sci-Fi / Adventure")
                                .durationMinutes(192)
                                .language("English / Hindi / Telugu / Tamil")
                                .releaseDate(LocalDate.now().plusDays(55))
                                .posterUrl("https://image.tmdb.org/t/p/w500/bRBeSHfGHwkEpImlhxPmOcUsaeg.jpg")
                                .rating(9.0)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Spider-Man: Beyond the Spider-Verse")
                                .description("Miles Morales hurtles across dimensions to confront the Spot and reshape his destiny alongside Gwen Stacy and the Spider-Society.")
                                .genre("Animation / Action / Adventure")
                                .durationMinutes(150)
                                .language("English / Hindi / Tamil / Telugu")
                                .releaseDate(LocalDate.now().plusDays(70))
                                .posterUrl("https://image.tmdb.org/t/p/w500/9KAe39xqyZnv9J4W3DRGdQqX82h.jpg")
                                .rating(9.2)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Mookuthi Amman 2")
                                .description("Goddess Mookuthi Amman descends once more to combat fake godmen, digital scammers, and protect simple devotees in a comedic adventure.")
                                .genre("Comedy / Fantasy")
                                .durationMinutes(136)
                                .language("Tamil / Telugu")
                                .releaseDate(LocalDate.now().plusDays(15))
                                .posterUrl("https://image.tmdb.org/t/p/w500/xDGbZ0JJ3mYaGKy4Nzd9Kph6M9L.jpg")
                                .rating(7.8)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Ghost Soldier")
                                .description("An elite cybernetically enhanced commando presumed KIA returns from behind enemy lines with tactical intelligence to avert World War III.")
                                .genre("Action / Sci-Fi")
                                .durationMinutes(124)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().plusDays(18))
                                .posterUrl("https://image.tmdb.org/t/p/w500/chpWmskl3aKm1aTZqUHRCtviwPy.jpg")
                                .rating(8.0)
                                .isUpcoming(true)
                                .isActive(true)
                                .build()
                );

                movieRepository.saveAll(freshCatalog);
                log.info("Seeded {} fresh 2025-2026 live and upcoming movies", freshCatalog.size());
            }

            // 4. Seed Premier Residential Cinema Theatres across 36+ Major Indian Cities
            if (theatreRepository.count() < 30) {
                log.info("Seeding premier residential multiplexes across 36 major Indian cities with GPS coordinates...");
                showRepository.deleteAll();
                seatRepository.deleteAll();
                screenRepository.deleteAll();
                theatreRepository.deleteAll();

                List<Theatre> indianTheatres = List.of(
                        // 1. Mumbai
                        Theatre.builder().name("PVR ICON Infinity Mall").city("Mumbai").address("Link Road, Andheri West, Mumbai 400053").latitude(19.1415).longitude(72.8315).build(),
                        Theatre.builder().name("INOX Megaplex Inorbit Mall").city("Mumbai").address("New Link Road, Malad West, Mumbai 400064").latitude(19.1764).longitude(72.8358).build(),
                        Theatre.builder().name("Cinepolis Viviana Mall").city("Mumbai").address("Eastern Express Highway, Thane West, Mumbai 400606").latitude(19.2088).longitude(72.9715).build(),

                        // 2. Delhi NCR
                        Theatre.builder().name("PVR Director's Cut Ambience Mall").city("Delhi NCR").address("Nelson Mandela Road, Vasant Kunj, New Delhi 110070").latitude(28.5402).longitude(77.1554).build(),
                        Theatre.builder().name("INOX Insignia Epicuria").city("Delhi NCR").address("Nehru Place Metro Station, New Delhi 110019").latitude(28.5501).longitude(77.2515).build(),
                        Theatre.builder().name("PVR Superplex Mall of India").city("Delhi NCR").address("Sector 18, Noida, Uttar Pradesh 201301").latitude(28.5672).longitude(77.3209).build(),
                        Theatre.builder().name("PVR Ambience Mall").city("Delhi NCR").address("NH 8, Cyber City, Gurugram, Haryana 122002").latitude(28.5042).longitude(77.0970).build(),

                        // 3. Bengaluru
                        Theatre.builder().name("PVR IMAX Forum Mall").city("Bengaluru").address("Hosur Road, Koramangala, Bengaluru 560095").latitude(12.9352).longitude(77.6111).build(),
                        Theatre.builder().name("INOX Garuda Mall").city("Bengaluru").address("Magrath Road, Ashok Nagar, Bengaluru 560025").latitude(12.9704).longitude(77.6094).build(),
                        Theatre.builder().name("Cinepolis Nexus Shantiniketan").city("Bengaluru").address("Whitefield Main Road, Bengaluru 560067").latitude(12.9892).longitude(77.7289).build(),

                        // 4. Hyderabad
                        Theatre.builder().name("AMB Cinemas").city("Hyderabad").address("Gachibowli - Miyapur Road, Kothaguda, Hyderabad 500084").latitude(17.4577).longitude(78.3644).build(),
                        Theatre.builder().name("PVR Next Galleria Mall").city("Hyderabad").address("Panjagutta Circle, Hyderabad 500082").latitude(17.4262).longitude(78.4522).build(),
                        Theatre.builder().name("Prasads Multiplex").city("Hyderabad").address("NTR Gardens, Necklace Road, Hyderabad 500063").latitude(17.4125).longitude(78.4682).build(),

                        // 5. Ahmedabad
                        Theatre.builder().name("Cinepolis Alpha One Mall").city("Ahmedabad").address("Vastrapur Lake, Ahmedabad 380015").latitude(23.0373).longitude(72.5312).build(),
                        Theatre.builder().name("PVR Acropolis Mall").city("Ahmedabad").address("Thaltej Cross Road, SG Highway, Ahmedabad 380054").latitude(23.0514).longitude(72.5186).build(),

                        // 6. Chennai
                        Theatre.builder().name("SPI Cinemas Sathyam").city("Chennai").address("Thiru Vi Ka Road, Royapettah, Chennai 600014").latitude(13.0569).longitude(80.2586).build(),
                        Theatre.builder().name("PVR VR Mall").city("Chennai").address("Jawaharlal Nehru Road, Anna Nagar, Chennai 600040").latitude(13.0844).longitude(80.1947).build(),

                        // 7. Kolkata
                        Theatre.builder().name("INOX Quest Mall").city("Kolkata").address("Syed Amir Ali Avenue, Park Circus, Kolkata 700017").latitude(22.5392).longitude(88.3653).build(),
                        Theatre.builder().name("PVR Mani Square").city("Kolkata").address("EM Bypass, Phoolbagan, Kolkata 700054").latitude(22.5765).longitude(88.4014).build(),

                        // 8. Pune
                        Theatre.builder().name("PVR Phoenix Marketcity").city("Pune").address("Viman Nagar, Pune 411014").latitude(18.5621).longitude(73.9167).build(),
                        Theatre.builder().name("Cinepolis Seasons Mall").city("Pune").address("Magarpatta City, Hadapsar, Pune 411028").latitude(18.5197).longitude(73.9312).build(),

                        // 9. Jaipur
                        Theatre.builder().name("INOX Crystal Palm").city("Jaipur").address("Sahakar Marg, C-Scheme, Jaipur 302001").latitude(26.9038).longitude(75.7954).build(),
                        Theatre.builder().name("Cinepolis World Trade Park").city("Jaipur").address("Jawahar Lal Nehru Marg, Malviya Nagar, Jaipur 302017").latitude(26.8533).longitude(75.8052).build(),
                        Theatre.builder().name("Raj Mandir Cinema").city("Jaipur").address("Bhagwan Das Road, Panch Batti, Jaipur 302001").latitude(26.9171).longitude(75.8152).build(),

                        // 10. Lucknow
                        Theatre.builder().name("PVR Phoenix Palassio").city("Lucknow").address("Sector 7, Gomti Nagar Extension, Amar Shaheed Path, Lucknow 226010").latitude(26.8091).longitude(81.0211).build(),
                        Theatre.builder().name("INOX Riverside Mall").city("Lucknow").address("Vipin Khand, Gomti Nagar, Lucknow 226010").latitude(26.8521).longitude(80.9782).build(),
                        Theatre.builder().name("Cinepolis One Awadh Center").city("Lucknow").address("Vibhuti Khand, Gomti Nagar, Lucknow 226010").latitude(26.8672).longitude(81.0028).build(),

                        // 11. Chandigarh
                        Theatre.builder().name("PVR Elante Mall").city("Chandigarh").address("Industrial Area Phase 1, Chandigarh 160002").latitude(30.7055).longitude(76.8013).build(),
                        Theatre.builder().name("Cinepolis Jagat Mall").city("Chandigarh").address("Sector 17, Chandigarh 160017").latitude(30.7410).longitude(76.7850).build(),

                        // 12. Kochi
                        Theatre.builder().name("PVR Lulu Mall").city("Kochi").address("NH 544, Edappally, Kochi, Kerala 682024").latitude(10.0271).longitude(76.3082).build(),
                        Theatre.builder().name("Cinepolis Centre Square Mall").city("Kochi").address("MG Road, Shenoys, Kochi 682035").latitude(9.9723).longitude(76.2825).build(),

                        // 13. Indore
                        Theatre.builder().name("INOX C21 Mall").city("Indore").address("AB Road, Scheme 54 PU4, Vijay Nagar, Indore 452010").latitude(22.7533).longitude(75.8937).build(),
                        Theatre.builder().name("PVR Treasure Island").city("Indore").address("MG Road, South Tukoganj, Indore 452001").latitude(22.7214).longitude(75.8778).build(),
                        Theatre.builder().name("Fundore Cinema").city("Indore").address("Rau Circle, AB Road, Rau, Indore, Madhya Pradesh 453331").latitude(22.6774).longitude(75.8295).build(),
                        Theatre.builder().name("Fortune Cinemas").city("Indore").address("Vikram Mall, Trinity Mall, Umariya, Mhow-Indore Road, Indore 453441").latitude(22.6480).longitude(75.7850).build(),

                        // 14. Surat
                        Theatre.builder().name("Cinepolis Imperial Square").city("Surat").address("Pal-Adajan Road, Surat 395009").latitude(21.1925).longitude(72.7844).build(),
                        Theatre.builder().name("INOX VR Surat").city("Surat").address("Dumas Road, Magdalla, Surat 395007").latitude(21.1492).longitude(72.7571).build(),

                        // 15. Bhopal
                        Theatre.builder().name("Cinepolis DB City Mall").city("Bhopal").address("Arera Hills, MP Nagar, Bhopal 462011").latitude(23.2332).longitude(77.4326).build(),
                        Theatre.builder().name("PVR Aura Mall").city("Bhopal").address("Trilanga Main Road, Shahpura, Bhopal 462039").latitude(23.1895).longitude(77.4335).build(),

                        // 16. Patna
                        Theatre.builder().name("Cinepolis Patna One Mall").city("Patna").address("Dak Bunglow Road, Fraser Road, Patna 800001").latitude(25.6094).longitude(85.1376).build(),
                        Theatre.builder().name("INOX City Centre").city("Patna").address("Lodipur, Patna 800001").latitude(25.6025).longitude(85.1432).build(),

                        // 17. Nagpur
                        Theatre.builder().name("Cinepolis VR Mall").city("Nagpur").address("Medical Square, Rambagh, Nagpur 440003").latitude(21.1278).longitude(79.0965).build(),
                        Theatre.builder().name("INOX Jaswant Tuli Mall").city("Nagpur").address("Indora Chowk, Kamptee Road, Nagpur 440017").latitude(21.1732).longitude(79.0889).build(),

                        // 18. Visakhapatnam
                        Theatre.builder().name("INOX Varun Beach").city("Visakhapatnam").address("Beach Road, Pandurangapuram, Visakhapatnam 530002").latitude(17.7135).longitude(83.3188).build(),
                        Theatre.builder().name("Cinepolis CMR Central").city("Visakhapatnam").address("Maddilapalem, Visakhapatnam 530013").latitude(17.7289).longitude(83.3195).build(),

                        // 19. Vadodara
                        Theatre.builder().name("INOX Race Course").city("Vadodara").address("Race Course Road, Alkapuri, Vadodara 390007").latitude(22.3114).longitude(73.1752).build(),
                        Theatre.builder().name("Cinepolis Inorbit Mall").city("Vadodara").address("Alembic Road, Gorwa, Vadodara 390003").latitude(22.3245).longitude(73.1678).build(),

                        // 20. Coimbatore
                        Theatre.builder().name("INOX Prozone Mall").city("Coimbatore").address("Sathy Road, Saravanampatti, Coimbatore 641035").latitude(11.0558).longitude(76.9942).build(),
                        Theatre.builder().name("The Cinema Brookefields").city("Coimbatore").address("Krishnaswamy Road, Coimbatore 641001").latitude(11.0089).longitude(76.9587).build(),

                        // 21. Dehradun
                        Theatre.builder().name("PVR Pacific Mall").city("Dehradun").address("Rajpur Road, Jakhan, Dehradun 248001").latitude(30.3648).longitude(78.0702).build(),
                        Theatre.builder().name("Silvercity Multiplex").city("Dehradun").address("Rajpur Road, Hathibarkala, Dehradun 248001").latitude(30.3341).longitude(78.0556).build(),

                        // 22. Guwahati
                        Theatre.builder().name("Cinepolis Central Mall").city("Guwahati").address("GS Road, Christian Basti, Guwahati 781005").latitude(26.1558).longitude(91.7765).build(),
                        Theatre.builder().name("PVR City Centre").city("Guwahati").address("GS Road, Rukmini Gaon, Guwahati 781022").latitude(26.1389).longitude(91.7924).build(),

                        // 23. Mysuru
                        Theatre.builder().name("INOX Mall of Mysore").city("Mysuru").address("MG Road, Nazarbad, Mysuru 570010").latitude(12.3021).longitude(76.6665).build(),
                        Theatre.builder().name("DRC Cinemas").city("Mysuru").address("BM Habitat Mall, Jayalakshmipuram, Mysuru 570012").latitude(12.3245).longitude(76.6234).build(),

                        // 24. Amritsar
                        Theatre.builder().name("Cinepolis Mall of Amritsar").city("Amritsar").address("GT Road, Amritsar 143001").latitude(31.6245).longitude(74.9012).build(),
                        Theatre.builder().name("INOX Trillium Mall").city("Amritsar").address("Circular Road, Amritsar 143001").latitude(31.6512).longitude(74.8765).build(),

                        // 25. Varanasi
                        Theatre.builder().name("PVR JHV Mall").city("Varanasi").address("The Mall, Cantonment, Varanasi 221002").latitude(25.3345).longitude(82.9812).build(),
                        Theatre.builder().name("IP Sigra Mall").city("Varanasi").address("Sigra Road, Varanasi 221010").latitude(25.3168).longitude(82.9912).build(),

                        // 26. Agra
                        Theatre.builder().name("Meher Talkies & Cinema").city("Agra").address("Fatehabad Road, Tajganj, Agra 282001").latitude(27.1623).longitude(78.0345).build(),
                        Theatre.builder().name("Wave Cinemas TDI Mall").city("Agra").address("Taj Nagari Phase 1, Agra 282001").latitude(27.1589).longitude(78.0512).build(),

                        // 27. Goa (Panaji)
                        Theatre.builder().name("INOX Panjim").city("Goa (Panaji)").address("Old GMC Heritage Complex, Panaji 403001").latitude(15.4989).longitude(73.8234).build(),
                        Theatre.builder().name("Cinepolis Caculo Mall").city("Goa (Panaji)").address("St. Inez, Panaji 403001").latitude(15.4912).longitude(73.8189).build(),

                        // 28. Thiruvananthapuram
                        Theatre.builder().name("PVR Lulu Mall").city("Thiruvananthapuram").address("Aakkulam, Kazhakkoottam, Thiruvananthapuram 695029").latitude(8.5345).longitude(76.9012).build(),
                        Theatre.builder().name("Aries Plex SL Cinemas").city("Thiruvananthapuram").address("Thampanoor, Thiruvananthapuram 695001").latitude(8.4876).longitude(76.9512).build(),

                        // 29. Bhubaneswar
                        Theatre.builder().name("Cinepolis Esplanade One").city("Bhubaneswar").address("Rasulgarh, Bhubaneswar 751010").latitude(20.3012).longitude(85.8612).build(),
                        Theatre.builder().name("INOX Forum Mart").city("Bhubaneswar").address("Kharvel Nagar, Bhubaneswar 751001").latitude(20.2789).longitude(85.8412).build(),

                        // 30. Raipur
                        Theatre.builder().name("PVR Magneto The Mall").city("Raipur").address("Labhandi, GE Road, Raipur 492006").latitude(21.2389).longitude(81.6812).build(),
                        Theatre.builder().name("Cinepolis City Centre Mall").city("Raipur").address("Pandri, Raipur 492004").latitude(21.2589).longitude(81.6512).build(),

                        // 31. Mangalore
                        Theatre.builder().name("PVR Forum Fiza Mall").city("Mangalore").address("Pandeshwar, Mangalore 575001").latitude(12.8612).longitude(74.8412).build(),
                        Theatre.builder().name("Cinepolis City Centre Mall").city("Mangalore").address("KS Rao Road, Hampankatta, Mangalore 575001").latitude(12.8712).longitude(74.8456).build(),

                        // 32. Ranchi
                        Theatre.builder().name("PVR Nucleus Mall").city("Ranchi").address("Circular Road, Lalpur, Ranchi 834001").latitude(23.3712).longitude(85.3345).build(),
                        Theatre.builder().name("Fun Cinemas").city("Ranchi").address("Hinoo, Ranchi 834002").latitude(23.3212).longitude(85.3145).build(),

                        // 33. Jodhpur
                        Theatre.builder().name("Bioscope Cinemas").city("Jodhpur").address("Shastri Nagar, Jodhpur 342003").latitude(26.2712).longitude(73.0112).build(),
                        Theatre.builder().name("INOX Indiabulls Mega Mall").city("Jodhpur").address("Pal Road, Jodhpur 342008").latitude(26.2512).longitude(72.9812).build(),

                        // 34. Madurai
                        Theatre.builder().name("INOX Vishaal De Mal").city("Madurai").address("Gokhale Road, Tallakulam, Madurai 625002").latitude(9.9345).longitude(78.1312).build(),
                        Theatre.builder().name("Jazz Cinemas").city("Madurai").address("KK Nagar, Madurai 625020").latitude(9.9212).longitude(78.1456).build(),

                        // 35. Nashik
                        Theatre.builder().name("Cinepolis City Centre Mall").city("Nashik").address("Untwadi Road, Sambhaji Chowk, Nashik 422002").latitude(19.9812).longitude(73.7612).build(),
                        Theatre.builder().name("INOX Pinnacle Mall").city("Nashik").address("Trimbak Road, Sharanpur, Nashik 422002").latitude(19.9945).longitude(73.7789).build(),

                        // 36. Kanpur
                        Theatre.builder().name("INOX Z Square Mall").city("Kanpur").address("Bada Chauraha, Civil Lines, Kanpur 208001").latitude(26.4678).longitude(80.3456).build(),
                        Theatre.builder().name("Rave 3 Multiplex").city("Kanpur").address("Parbati Bagla Road, Tilak Nagar, Kanpur 208002").latitude(26.4812).longitude(80.3245).build()
                );

                theatreRepository.saveAll(indianTheatres);
                log.info("Seeded 75 premier theatres across 36 major Indian cities");
            }

            // 5a. Ensure Rau/Indore specific cinemas always exist (targeted check — survives re-runs without full wipe)
            boolean fundoreMissing = !theatreRepository.existsByNameAndCity("Fundore Cinema", "Indore");
            boolean fortuneMissing = !theatreRepository.existsByNameAndCity("Fortune Cinemas", "Indore");
            if (fundoreMissing || fortuneMissing) {
                log.info("Adding missing Rau-area cinemas (Fundore / Fortune) to Indore theatre list...");
                List<Theatre> rauCinemas = new ArrayList<>();
                if (fundoreMissing) {
                    rauCinemas.add(Theatre.builder()
                            .name("Fundore Cinema")
                            .city("Indore")
                            .address("Rau Circle, AB Road, Rau, Indore, Madhya Pradesh 453331")
                            .latitude(22.6774)
                            .longitude(75.8295)
                            .build());
                }
                if (fortuneMissing) {
                    rauCinemas.add(Theatre.builder()
                            .name("Fortune Cinemas")
                            .city("Indore")
                            .address("Vikram Mall, Trinity Mall, Umariya, Mhow-Indore Road, Indore 453441")
                            .latitude(22.6480)
                            .longitude(75.7850)
                            .build());
                }
                List<Theatre> savedRauCinemas = theatreRepository.saveAll(rauCinemas);

                // Seed screens and seats for newly added Rau cinemas
                List<Screen> rauScreens = new ArrayList<>();
                for (Theatre t : savedRauCinemas) {
                    rauScreens.add(Screen.builder().name("Audi 1 (IMAX Laser)").theatre(t).totalSeats(86).build());
                    rauScreens.add(Screen.builder().name("Audi 2 (Dolby Atmos 7.1)").theatre(t).totalSeats(86).build());
                }
                screenRepository.saveAll(rauScreens);

                List<Seat> rauSeats = new ArrayList<>();
                for (Screen sc : rauScreens) {
                    for (String row : List.of("A", "B", "C", "D")) {
                        for (int n = 1; n <= 10; n++) rauSeats.add(Seat.builder().seatNumber(row + n).seatType(Seat.SeatType.REGULAR).screen(sc).build());
                    }
                    for (String row : List.of("E", "F", "G")) {
                        for (int n = 1; n <= 10; n++) rauSeats.add(Seat.builder().seatNumber(row + n).seatType(Seat.SeatType.PREMIUM).screen(sc).build());
                    }
                    for (String row : List.of("H", "J")) {
                        for (int n = 1; n <= 8; n++) rauSeats.add(Seat.builder().seatNumber(row + n).seatType(Seat.SeatType.VIP).screen(sc).build());
                    }
                }
                seatRepository.saveAll(rauSeats);

                // Create shows for Rau cinemas for now-playing movies
                List<Movie> liveMoviesForRau = movieRepository.findByIsActiveTrueAndIsUpcomingFalseOrderByRatingDesc();
                if (!liveMoviesForRau.isEmpty()) {
                    List<Show> rauShows = com.moviebooking.util.ShowPricingAndScheduleHelper.generateShows(rauScreens, liveMoviesForRau, 4);
                    showRepository.saveAll(rauShows);
                }
                log.info("Rau-area cinemas seeded successfully: {} theatres, {} screens, {} seats",
                        savedRauCinemas.size(), rauScreens.size(), rauSeats.size());
            }

            // 5. Seed Screens & Seats (86 seats per screen)
            if (screenRepository.count() == 0 || seatRepository.count() < 2000) {
                log.info("Seeding grand multiplex screens (86 seats per auditorium) and tiered seat maps...");
                showRepository.deleteAll();
                seatRepository.deleteAll();
                screenRepository.deleteAll();

                List<Theatre> allTheatres = theatreRepository.findAll();
                List<Screen> allScreens = new ArrayList<>();

                for (Theatre theatre : allTheatres) {
                    Screen screen1 = Screen.builder()
                            .name("Audi 1 (IMAX Laser)")
                            .theatre(theatre)
                            .totalSeats(86)
                            .build();

                    Screen screen2 = Screen.builder()
                            .name("Audi 2 (Dolby Atmos 7.1)")
                            .theatre(theatre)
                            .totalSeats(86)
                            .build();

                    allScreens.add(screen1);
                    allScreens.add(screen2);
                }

                screenRepository.saveAll(allScreens);

                // Generate 86 seats for each screen across 3 realistic multiplex tiers:
                // Rows A, B, C, D: REGULAR (10 seats each: 1 to 10) = 40 seats
                // Rows E, F, G: PREMIUM (10 seats each: 1 to 10) = 30 seats
                // Rows H, J: VIP RECLINER (8 seats each: 1 to 8) = 16 seats
                List<Seat> allSeats = new ArrayList<>();
                for (Screen screen : allScreens) {
                    // Regular tier (Rows A-D, 10 seats each)
                    for (String r : List.of("A", "B", "C", "D")) {
                        for (int num = 1; num <= 10; num++) {
                            allSeats.add(Seat.builder()
                                    .seatNumber(r + num)
                                    .seatType(Seat.SeatType.REGULAR)
                                    .screen(screen)
                                    .build());
                        }
                    }
                    // Premium tier (Rows E-G, 10 seats each)
                    for (String r : List.of("E", "F", "G")) {
                        for (int num = 1; num <= 10; num++) {
                            allSeats.add(Seat.builder()
                                    .seatNumber(r + num)
                                    .seatType(Seat.SeatType.PREMIUM)
                                    .screen(screen)
                                    .build());
                        }
                    }
                    // VIP Recliner tier (Rows H, J, 8 luxury recliners each)
                    for (String r : List.of("H", "J")) {
                        for (int num = 1; num <= 8; num++) {
                            allSeats.add(Seat.builder()
                                    .seatNumber(r + num)
                                    .seatType(Seat.SeatType.VIP)
                                    .screen(screen)
                                    .build());
                        }
                    }
                }

                seatRepository.saveAll(allSeats);
                log.info("Seeded screens and seats across all theatres");
            }

            // 6. Seed Real-time Shows for Now-Showing Live Movies
            if (showRepository.count() == 0) {
                log.info("Scheduling real-time live shows across upcoming days with cinema-specific showtimes and realistic tiered pricing...");

                List<Movie> liveMovies = movieRepository.findByIsActiveTrueAndIsUpcomingFalseOrderByRatingDesc();
                List<Screen> allScreens = screenRepository.findAllWithTheatre();

                if (!liveMovies.isEmpty() && !allScreens.isEmpty()) {
                    List<Show> allShows = com.moviebooking.util.ShowPricingAndScheduleHelper.generateShows(allScreens, liveMovies, 4);
                    showRepository.saveAll(allShows);
                    log.info("Successfully scheduled {} live shows with realistic varied showtimes and pricing", allShows.size());
                }
            }

            // 7. Live Auto-Sync from TMDb API for India region (runs in background so server starts in 2 seconds)
            if (tmdbMovieService.isConfigured()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        log.info("Performing live background auto-sync from TMDb Indian cinemas feed...");
                        tmdbMovieService.syncLiveMoviesFromImdb();
                    } catch (Exception e) {
                        log.warn("Background auto-sync from TMDB notice: {}", e.getMessage());
                    }
                }, "tmdb-sync-thread").start();
            }
        };
    }
}
