
package org.alsception.pegasus.features.reservations;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class PGSReservationController {

    private final PGSReservationService reservationService;

    //TODO: add logger

    @GetMapping
    public List<PGSReservationDTO> getAllReservations() {
        return reservationService.getAllReservations();
    }

    @GetMapping("/{id}")
    public PGSReservationDTO getReservation(
            @PathVariable Long id
    ) {
        return reservationService.getReservation(id);
    }

    @PostMapping
    public ResponseEntity<PGSReservationDTO> createReservation(
            @RequestBody PGSReservationDTO dto
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.createReservation(dto));
    }

    @PutMapping("/{id}")
    public PGSReservationDTO updateReservation(
            @PathVariable Long id,
            @RequestBody PGSReservationDTO dto
    ) {
        return reservationService.updateReservation(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservation(
            @PathVariable Long id
    ) {
        reservationService.deleteReservation(id);
    }
}