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
                                .title("Hope")
                                .tmdbId(1058424L)
                                .imdbId("tt27369017")
                                .description("Residents of a remote harbor town discover a mysterious extraterrestrial presence in the misty outskirts, igniting a high-stakes survival struggle.")
                                .genre("Sci-Fi / Mystery / Action")
                                .durationMinutes(125)
                                .language("English / Korean / Hindi")
                                .releaseDate(LocalDate.now().minusDays(17))
                                .posterUrl("https://image.tmdb.org/t/p/w500/6VY1ZKgpsc8CfGsQcCGiYEgHdH0.jpg")
                                .rating(7.8)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Clayface")
                                .tmdbId(1400940L)
                                .description("A former actor turned tragic shapeshifter infiltrates high society to uncover a vast conspiracy, defying heroes and villains alike in a battle of identity.")
                                .genre("Action / Sci-Fi / Thriller")
                                .durationMinutes(132)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(17))
                                .posterUrl("https://image.tmdb.org/t/p/w500/9l1NuFjel2j89hiuiJbsxfblqjy.jpg")
                                .rating(7.6)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Ramayana: Rise of a Legend")
                                .tmdbId(656908L)
                                .description("Nitesh Tiwari's grand visual spectacle bringing India's timeless epic to cinema screens with groundbreaking IMAX 3D technology.")
                                .genre("Mythology / Epic Action")
                                .durationMinutes(185)
                                .language("Hindi / Telugu / Tamil")
                                .releaseDate(LocalDate.now().minusDays(4))
                                .posterUrl("https://image.tmdb.org/t/p/w500/dPrtk4iNf6T5w8fWfg4uxDDbJSW.jpg")
                                .rating(8.5)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Jailer 2")
                                .tmdbId(1234139L)
                                .description("Tiger Muthuvel Pandian is drawn out of peaceful retirement into an international syndicate confrontation spanning Tamil Nadu, Karnataka, and Mumbai.")
                                .genre("Action / Crime / Drama")
                                .durationMinutes(168)
                                .language("Tamil / Telugu / Hindi")
                                .releaseDate(LocalDate.now().minusDays(10))
                                .posterUrl("https://image.tmdb.org/t/p/w500/upwdEyhhYZ7pT2hD535fr2MGfpx.jpg")
                                .rating(8.2)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Drishyam: The Conclusion")
                                .tmdbId(1269325L)
                                .description("Vijay Salgaonkar faces his ultimate cerebral face-off with police investigators as decades-buried secrets resurface in Goa.")
                                .genre("Thriller / Drama")
                                .durationMinutes(152)
                                .language("Hindi")
                                .releaseDate(LocalDate.now().minusDays(22))
                                .posterUrl("https://image.tmdb.org/t/p/w500/67FsF2kpgEZ1T5Qos4adgqm9dLf.jpg")
                                .rating(7.9)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Verity")
                                .tmdbId(1283515L)
                                .description("A struggling writer accepts the job offer of a lifetime to finish the remaining books of a successful series after the author was injured.")
                                .genre("Thriller / Mystery / Romance")
                                .durationMinutes(115)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(15))
                                .posterUrl("https://image.tmdb.org/t/p/w500/rmaNMA1seHqpjtnYKzyynFAr1g8.jpg")
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
                                .posterUrl("https://image.tmdb.org/t/p/w500/8nP4lF51Ea861YfHEEqhZBLYv3e.jpg")
                                .rating(6.8)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Mookuthi Amman 2")
                                .tmdbId(1327835L)
                                .description("A queen sacrifices herself to protect her son from invading enemies. Mookuthi Amman returns to protect the child and confronts a new evil.")
                                .genre("Fantasy / Comedy / Drama")
                                .durationMinutes(145)
                                .language("Tamil / Telugu / Hindi")
                                .releaseDate(LocalDate.now().minusDays(3))
                                .posterUrl("https://image.tmdb.org/t/p/w500/dNNzsstJeQxnESyH0hZcLGblLMz.jpg")
                                .rating(8.1)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Wild Horse Nine")
                                .tmdbId(891621L)
                                .description("Shortly before the 1973 Chilean coup, CIA agents Chris and Lee are dispatched from Santiago to Easter Island amidst iconic statues.")
                                .genre("Comedy / Mystery")
                                .durationMinutes(112)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().minusDays(3))
                                .posterUrl("https://image.tmdb.org/t/p/w500/dNJFRtVg3xLQ7dbNIQ55cmmIniD.jpg")
                                .rating(8.1)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Scene")
                                .tmdbId(1402181L)
                                .description("Inbaraj, a police officer, is forced to come back from retirement when a new case resembles one he closed years ago.")
                                .genre("Comedy / Crime / Thriller")
                                .durationMinutes(135)
                                .language("Tamil / Hindi")
                                .releaseDate(LocalDate.now().minusDays(3))
                                .posterUrl("https://image.tmdb.org/t/p/w500/1vjVJKpdT6gha7cpkmDJODpk8mU.jpg")
                                .rating(8.4)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Ranabaali")
                                .tmdbId(1389379L)
                                .description("Set in 19th-century British India, a fierce leader rises from a drought-stricken land to fight colonial oppression and atrocities.")
                                .genre("Action / Period Drama / Thriller")
                                .durationMinutes(155)
                                .language("Telugu / Hindi")
                                .releaseDate(LocalDate.now().minusDays(8))
                                .posterUrl("https://image.tmdb.org/t/p/w500/cXaCyYYbafVyMrs8rwKmxezBYPJ.jpg")
                                .rating(7.7)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Bhool Bhulaiyaa 3")
                                .tmdbId(1196470L)
                                .description("Rooh Baba ventures into a haunted mansion in West Bengal, confronting two vengeful spirits claiming to be Manjulika.")
                                .genre("Comedy / Horror")
                                .durationMinutes(158)
                                .language("Hindi")
                                .releaseDate(LocalDate.now().minusDays(8))
                                .posterUrl("https://image.tmdb.org/t/p/w500/3AfHD1HoaQpQwKH8kxRdBKVmzeU.jpg")
                                .rating(7.1)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Disco")
                                .tmdbId(1612811L)
                                .description("An ordinary man undergoes a dramatic transformation as he enters the dark and dangerous world of the underworld.")
                                .genre("Action / Crime / Thriller")
                                .durationMinutes(130)
                                .language("Malayalam / Hindi")
                                .releaseDate(LocalDate.now().minusDays(3))
                                .posterUrl("https://image.tmdb.org/t/p/w500/yZ3AeO4bGYkSVT4onkAbLPTOSXe.jpg")
                                .rating(7.2)
                                .isUpcoming(false)
                                .isActive(true)
                                .build(),

                        // UPCOMING RELEASES (COMING SOON)
                        Movie.builder()
                                .title("Godzilla Minus Zero")
                                .tmdbId(1381071L)
                                .description("The iconic monster returns in a titanic prehistoric showdown set in post-war Japan's shattered coastal frontier.")
                                .genre("Action / Sci-Fi / Monster")
                                .durationMinutes(130)
                                .language("Japanese / English / Hindi")
                                .releaseDate(LocalDate.now().plusDays(11))
                                .posterUrl("https://image.tmdb.org/t/p/w500/hSmIJluOJmTWvJs4d7kHRxjc8s8.jpg")
                                .rating(8.2)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Ghost Soldier")
                                .tmdbId(1562642L)
                                .description("An elite cybernetically enhanced commando presumed KIA returns from behind enemy lines with tactical intelligence to avert World War III.")
                                .genre("Action / Sci-Fi")
                                .durationMinutes(124)
                                .language("English / Hindi")
                                .releaseDate(LocalDate.now().plusDays(2))
                                .posterUrl("https://image.tmdb.org/t/p/w500/8Y4oJ5iUs3hgPcDB3SIEmsNYjZk.jpg")
                                .rating(8.0)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("THE FURTHER MIS-ADVENTURES OF CLIFF BOOTH")
                                .tmdbId(1457866L)
                                .description("Cliff Booth drifts across Hollywood's sun-drenched, perilous underbelly taking on stunt jobs and mercenary gigs.")
                                .genre("Action / Comedy / Crime")
                                .durationMinutes(140)
                                .language("English")
                                .releaseDate(LocalDate.now().plusDays(16))
                                .posterUrl("https://image.tmdb.org/t/p/w500/5jO6rhfethzH7xdWl6KCOxCNCzQ.jpg")
                                .rating(8.6)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("Yeh Prem Mol Liya")
                                .tmdbId(1209723L)
                                .description("A heartfelt romantic drama set against the vibrant landscapes of Rajasthan exploring sacrifice, heritage, and modern love.")
                                .genre("Romance / Drama")
                                .durationMinutes(138)
                                .language("Hindi")
                                .releaseDate(LocalDate.now().plusDays(18))
                                .posterUrl("https://image.tmdb.org/t/p/w500/kUxkvh6GMZL4AHNgNDty68yTnwQ.jpg")
                                .rating(7.9)
                                .isUpcoming(true)
                                .isActive(true)
                                .build(),
                        Movie.builder()
                                .title("OM: Chapter 1")
                                .tmdbId(1384528L)
                                .description("A mythic warrior bound by an ancient pledge rises to protect a sacred mountain kingdom from ruthless invaders.")
                                .genre("Action / Mythology / Fantasy")
                                .durationMinutes(152)
                                .language("Telugu / Hindi / Tamil")
                                .releaseDate(LocalDate.now().plusDays(11))
                                .posterUrl("https://image.tmdb.org/t/p/w500/gWWedXJXLrEaKCTrC4nMPPumDSb.jpg")
                                .rating(8.4)
                                .isUpcoming(true)
                                .isActive(true)
                                .build()
                );

                movieRepository.saveAll(freshCatalog);
                log.info("Seeded {} fresh 2025-2026 live and upcoming movies", freshCatalog.size());
            }

            // Database Self-Healing: Repair existing database records with authentic TMDB posters and purge old/synthetic records
            try {
                List<Movie> existingMovies = movieRepository.findAll();
                for (Movie m : existingMovies) {
                    String title = m.getTitle() != null ? m.getTitle().trim() : "";
                    
                    // 1. Fix "Reporting Live" mismatch -> "Bhool Bhulaiyaa 3" (TMDB 1196470)
                    if ("Reporting Live".equalsIgnoreCase(title)) {
                        m.setTitle("Bhool Bhulaiyaa 3");
                        m.setTmdbId(1196470L);
                        m.setGenre("Comedy / Horror");
                        m.setDescription("Rooh Baba ventures into a haunted mansion in West Bengal, confronting two vengeful spirits claiming to be Manjulika.");
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/3AfHD1HoaQpQwKH8kxRdBKVmzeU.jpg");
                        m.setReleaseDate(LocalDate.now().minusDays(8));
                        movieRepository.save(m);
                    }
                    // 2. Fix other synthetic titles from old initializer to genuine TMDB titles
                    else if ("Fall 2: Deadpoint".equalsIgnoreCase(title)) {
                        m.setTitle("Verity");
                        m.setTmdbId(1283515L);
                        m.setGenre("Thriller / Mystery / Romance");
                        m.setDescription("A struggling writer accepts the job offer of a lifetime to finish the remaining books of a successful series.");
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/rmaNMA1seHqpjtnYKzyynFAr1g8.jpg");
                        movieRepository.save(m);
                    }
                    else if ("The Paradise".equalsIgnoreCase(title)) {
                        m.setTitle("Disco");
                        m.setTmdbId(1612811L);
                        m.setGenre("Action / Crime / Thriller");
                        m.setDescription("An ordinary man undergoes a dramatic transformation as he enters the underworld.");
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/yZ3AeO4bGYkSVT4onkAbLPTOSXe.jpg");
                        movieRepository.save(m);
                    }
                    else if ("Runner".equalsIgnoreCase(title)) {
                        m.setTitle("Wild Horse Nine");
                        m.setTmdbId(891621L);
                        m.setGenre("Comedy / Mystery");
                        m.setDescription("Shortly before the 1973 Chilean coup, CIA agents are dispatched to Easter Island amidst iconic statues.");
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/dNJFRtVg3xLQ7dbNIQ55cmmIniD.jpg");
                        movieRepository.save(m);
                    }
                    else if ("Resident Evil".equalsIgnoreCase(title)) {
                        m.setTitle("Scene");
                        m.setTmdbId(1402181L);
                        m.setGenre("Comedy / Crime / Thriller");
                        m.setDescription("Inbaraj, a police officer, is forced to come back from retirement when a new case resembles one he closed years ago.");
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/1vjVJKpdT6gha7cpkmDJODpk8mU.jpg");
                        movieRepository.save(m);
                    }
                    else if ("Dhurandhar".equalsIgnoreCase(title)) {
                        m.setTitle("Ranabaali");
                        m.setTmdbId(1389379L);
                        m.setGenre("Action / Period Drama / Thriller");
                        m.setDescription("Set in 19th-century British India, a fierce leader rises from a drought-stricken land to fight colonial oppression.");
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/cXaCyYYbafVyMrs8rwKmxezBYPJ.jpg");
                        movieRepository.save(m);
                    }
                    else if ("Toxic".equalsIgnoreCase(title)) {
                        m.setTitle("Mookuthi Amman 2");
                        m.setTmdbId(1327835L);
                        m.setGenre("Fantasy / Comedy / Drama");
                        m.setDescription("A queen sacrifices herself to protect her son. Mookuthi Amman returns to protect the child and confronts a new evil.");
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/dNNzsstJeQxnESyH0hZcLGblLMz.jpg");
                        movieRepository.save(m);
                    }
                    else if ("Jailer 2".equalsIgnoreCase(title)) {
                        m.setTmdbId(1234139L);
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/upwdEyhhYZ7pT2hD535fr2MGfpx.jpg");
                        movieRepository.save(m);
                    }
                    else if ("Drishyam: The Conclusion".equalsIgnoreCase(title)) {
                        m.setTmdbId(1269325L);
                        m.setPosterUrl("https://image.tmdb.org/t/p/w500/67FsF2kpgEZ1T5Qos4adgqm9dLf.jpg");
                        movieRepository.save(m);
                    }
                    // 3. Deactivate old movies (e.g. Finding Nemo 2003, Forrest Gump 1994, Judgment Night 1993)
                    else if (m.getReleaseDate() != null && m.getReleaseDate().isBefore(LocalDate.of(2024, 1, 1))) {
                        m.setIsActive(false);
                        movieRepository.save(m);
                        log.info("Deactivated legacy movie record: {} (Release: {})", m.getTitle(), m.getReleaseDate());
                    }
                    // 4. Poster healing for core movies
                    else if (m.getTmdbId() != null) {
                        if (m.getTmdbId().equals(1058424L)) {
                            m.setPosterUrl("https://image.tmdb.org/t/p/w500/6VY1ZKgpsc8CfGsQcCGiYEgHdH0.jpg");
                            m.setImdbId("tt27369017");
                            movieRepository.save(m);
                        } else if (m.getTmdbId().equals(1400940L)) {
                            m.setPosterUrl("https://image.tmdb.org/t/p/w500/9l1NuFjel2j89hiuiJbsxfblqjy.jpg");
                            movieRepository.save(m);
                        } else if (m.getTmdbId().equals(656908L)) {
                            m.setPosterUrl("https://image.tmdb.org/t/p/w500/dPrtk4iNf6T5w8fWfg4uxDDbJSW.jpg");
                            movieRepository.save(m);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Database self-healing warning: {}", e.getMessage());
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
