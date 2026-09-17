package org.alsception.pegasus.features.reservations;

import lombok.RequiredArgsConstructor;
import org.alsception.pegasus.features.rooms.PGSRoom;
import org.alsception.pegasus.features.rooms.PGSRoomRepository;
import org.alsception.pegasus.features.users.PGSUser;
import org.alsception.pegasus.features.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PGSReservationService {

    private final PGSReservationRepository reservationRepository;
    private final PGSRoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PGSReservationDTO> getAllReservations() {
        return reservationRepository.findAllByOrderByCheckInAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PGSReservationDTO getReservation(Long id) {

        PGSReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found: " + id)
                );

        return toDTO(reservation);
    }

    @Transactional
    public PGSReservationDTO createReservation(PGSReservationDTO dto) {

        PGSRoom room = roomRepository.findById(dto.getRoomId())
                .orElseThrow(() ->
                        new RuntimeException("Room not found: " + dto.getRoomId())
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

        if (dto.getBookerId() != null) {

            PGSUser user = userRepository.findById(dto.getBookerId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "User not found: " + dto.getBookerId()
                            )
                    );

            reservation.setBooker(user);
        }

        return toDTO(reservationRepository.save(reservation));
    }

    @Transactional
    public PGSReservationDTO updateReservation(
            Long id,
            PGSReservationDTO dto
    ) {

        PGSReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Reservation not found: " + id)
                );

        validateDates(dto);

        reservation.setCheckIn(dto.getCheckIn());
        reservation.setCheckOut(dto.getCheckOut());
        reservation.setExpectedArrivalTime(dto.getExpectedArrivalTime());
        reservation.setExpectedDepartureTime(dto.getExpectedDepartureTime());
        reservation.setTotalPrice(dto.getTotalPrice());
        reservation.setNotes(dto.getNotes());

        if (dto.getStatus() != null) {
            reservation.setStatus(dto.getStatus());
        }

        if (dto.getRoomId() != null &&
                !dto.getRoomId().equals(reservation.getRoom().getId())) {

            PGSRoom room = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Room not found: " + dto.getRoomId()
                            )
                    );

            reservation.setRoom(room);
        }

        if (dto.getBookerId() != null) {

            PGSUser user = userRepository.findById(dto.getBookerId())
                    .orElseThrow(() ->
                            new RuntimeException(
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
            throw new RuntimeException(
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
            dto.setBookerId(
                    reservation.getBooker().getId()
            );
        }

        dto.setRoomId(
                reservation.getRoom().getId()
        );

        dto.setCheckIn(reservation.getCheckIn());
        dto.setCheckOut(reservation.getCheckOut());
        dto.setExpectedArrivalTime(
                reservation.getExpectedArrivalTime()
        );
        dto.setExpectedDepartureTime(
                reservation.getExpectedDepartureTime()
        );
        dto.setStatus(reservation.getStatus());
        dto.setTotalPrice(reservation.getTotalPrice());
        dto.setNotes(reservation.getNotes());

        return dto;
    }
}