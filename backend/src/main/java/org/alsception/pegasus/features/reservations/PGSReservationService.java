package org.alsception.pegasus.features.reservations;

import lombok.RequiredArgsConstructor;

import org.alsception.pegasus.features.rooms.PGSRoom;
import org.alsception.pegasus.features.rooms.PGSRoomRepository;
import org.alsception.pegasus.features.users.PGSUser;
import org.alsception.pegasus.features.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PGSReservationService {

    private final PGSReservationRepository reservationRepository;
    private final PGSRoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

    private static final Logger logger = LoggerFactory.getLogger(PGSReservationService.class);

    @Transactional(readOnly = true)
    public List<PGSReservationDTO> getAllReservations() {
        return reservationRepository.findAllByOrderByCheckInAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<PGSReservationDTO> searchReservations(LocalDate dateFrom, LocalDate dateTo,
                                                  BigDecimal priceFrom, BigDecimal priceTo,
                                                  Integer persons) {
        return reservationRepository
                        .search(dateFrom, dateTo, priceFrom, priceTo, persons)
                        .stream()
                        .map(this::toDTO)   // tvoj postojeći mapper
                        .toList();
    }

    @Transactional(readOnly = true)
    public PGSReservationDTO getReservation(Long id) {

        PGSReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Reservation not found: " + id)
                );

        return toDTO(reservation);
    }

    @Transactional
    public PGSReservationDTO createReservation(PGSReservationDTO dto) 
    {

        PGSRoom room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() ->
                        new EntityNotFoundException("Room not found: " + dto.getRoomId())
                );

        validateDates(dto);

        checkRoomAvailability(dto);

        PGSReservation reservation = new PGSReservation();

        reservation.setRoom(room);
        reservation.setCheckIn(dto.getCheckIn());
        reservation.setCheckOut(dto.getCheckOut());
        reservation.setExpectedArrivalTime(dto.getExpectedArrivalTime());
        reservation.setExpectedDepartureTime(dto.getExpectedDepartureTime());
        reservation.setStatus(
                dto.getStatus() != null
                        ? dto.getStatus()
                        : PGSReservationStatus.PENDING
        );
        reservation.setTotalPrice(dto.getTotalPrice());
        reservation.setNotes(dto.getNotes());

        if (dto.getGuests() != null) {
                reservation.setGuests(dto.getGuests());
        }

        if (dto.getBookerId() != null) {

            PGSUser user = userRepository.findById(dto.getBookerId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "User not found: " + dto.getBookerId()
                            )
                    );

            reservation.setBooker(user);
        }

        reservation = reservationRepository.save(reservation);

        //Here we publish event that will be captured by ReservationNotificationListener, after reservation is saved

        logger.debug("Publishing new ReservationCreatedEvent, refId["+reservation.getId()+"]");

        publisher.publishEvent( new ReservationCreatedEvent( reservation.getId(),"pgsadmin" ));//for now we hardcode main admin user.

        return toDTO(reservation);
    }

    @Transactional
    public PGSReservationDTO updateReservation(
            Long id,
            PGSReservationDTO dto
    ) {

        PGSReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Reservation not found: " + id)
                );

        validateDates(dto);

        reservation.setCheckIn(dto.getCheckIn());
        reservation.setCheckOut(dto.getCheckOut());
        reservation.setExpectedArrivalTime(dto.getExpectedArrivalTime());
        reservation.setExpectedDepartureTime(dto.getExpectedDepartureTime());
        reservation.setTotalPrice(dto.getTotalPrice());
        reservation.setNotes(dto.getNotes());
        if (dto.getGuests() != null) {
                reservation.setGuests(dto.getGuests());
        }

        if (dto.getStatus() != null) {
            reservation.setStatus(dto.getStatus());
        }

        if (dto.getRoomId() != null &&
                !dto.getRoomId().equals(reservation.getRoom().getId())) {

            PGSRoom room = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Room not found: " + dto.getRoomId()
                            )
                    );

            reservation.setRoom(room);
        }

        if (dto.getBookerId() != null) {

            PGSUser user = userRepository.findById(dto.getBookerId())
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "User not found: " + dto.getBookerId()
                            )
                    );

            reservation.setBooker(user);
        }

        return toDTO(reservationRepository.save(reservation));
    }

    @Transactional
    public void deleteReservation(Long id) {

        if (!reservationRepository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Reservation not found: " + id
            );
        }

        /*TODO: umesto brisanja trebalo bi ovako nesto:
        reservation.setStatus(PGSReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        */

        reservationRepository.deleteById(id);
    }

    private void validateDates(PGSReservationDTO dto) {

        if (dto.getCheckIn() == null || dto.getCheckOut() == null) {
            throw new IllegalArgumentException(
                    "Check-in and check-out are required"
            );
        }

        if (!dto.getCheckOut().isAfter(dto.getCheckIn())) {
            throw new IllegalArgumentException(
                    "Check-out must be after check-in"
            );
        }
    }

    private void checkRoomAvailability(PGSReservationDTO dto) {

        boolean occupied =
                reservationRepository
                        .existsByRoomIdAndCheckInLessThanAndCheckOutGreaterThanAndStatusNot(
                                dto.getRoomId(),
                                dto.getCheckOut(),
                                dto.getCheckIn(),
                                PGSReservationStatus.CANCELLED
                        );

        if (occupied) {
            throw new IllegalStateException(
                    "Room is already reserved for the selected period"
            );
        }
    }

    private PGSReservationDTO toDTO(PGSReservation reservation) {

        PGSReservationDTO dto = new PGSReservationDTO();

        dto.setId(reservation.getId());

        if (reservation.getBooker() != null) {
                dto.setBookerId(reservation.getBooker().getId());
        }

        dto.setRoomId(reservation.getRoom().getId());
        dto.setRoomNumber(reservation.getRoom().getRoomNumber());
        dto.setCheckIn(reservation.getCheckIn());
        dto.setCheckOut(reservation.getCheckOut());
        dto.setExpectedArrivalTime(reservation.getExpectedArrivalTime());
        dto.setExpectedDepartureTime(reservation.getExpectedDepartureTime());
        dto.setGuests(reservation.getGuests());
        dto.setStatus(reservation.getStatus());
        dto.setTotalPrice(reservation.getTotalPrice());
        dto.setNotes(reservation.getNotes());
        dto.setCreated(reservation.getCreated());
        dto.setModified(reservation.getModified());

        return dto;
    }

    @Transactional
    public List<PGSReservation> createSampleReservations() 
    {
        logger.info("Creating sample reservations...");
        List<PGSRoom> rooms = roomRepository.findAll();

        if (rooms.isEmpty()) 
                {
            throw new IllegalStateException("Nema soba u bazi, prvo kreiraj bar jednu sobu.");
        }
        PGSUser booker = userRepository.findAll().stream().findFirst().orElse(null);

        LocalDate today = LocalDate.now();

        List<PGSReservation> reservations = new ArrayList<>();

        Object[][] reservationData = 
        {
        {0, 0, 1, "90.00", 14, 0, 10, 0, "Sample reservation 1"},
        {1, 2, 5, "250.00", 16, 30, 11, 0, "Sample reservation 2"},
        {2, 7, 14, "480.00", 18, 0, 12, 0, "Sample reservation 3"}
        };

        int i = 1;
        for (Object[] data : reservationData) 
        {
                PGSReservation reservation = buildReservation(
                        rooms.get((int) data[0] % rooms.size()),
                        booker,
                        today.plusDays((int) data[1]),
                        today.plusDays((int) data[2]),
                        i++, // guests
                        new BigDecimal((String) data[3]),
                        LocalTime.of((int) data[4], (int) data[5]),
                        LocalTime.of((int) data[6], (int) data[7]),
                        (String) data[8]
                );

                reservationRepository.save(reservation);
                reservations.add(reservation);
        }

        logger.info("Sample reservations created: "+reservations.size());

        return  reservations;
    }

    private PGSReservation buildReservation(PGSRoom room, PGSUser booker,
                                            LocalDate checkIn, LocalDate checkOut,
                                            int guests, BigDecimal totalPrice,
                                            LocalTime arrival, LocalTime departure,
                                            String notes) {
        PGSReservation r = new PGSReservation();
        r.setRoom(room);
        r.setBooker(booker);
        r.setCheckIn(checkIn);
        r.setCheckOut(checkOut);
        r.setGuests(guests);
        r.setTotalPrice(totalPrice);
        r.setExpectedArrivalTime(arrival);
        r.setExpectedDepartureTime(departure);
        r.setNotes(notes);
        r.setStatus(PGSReservationStatus.PENDING);
        return r;
    }
}