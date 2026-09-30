
package org.alsception.pegasus.features.reservations;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class PGSReservationController {

    private final PGSReservationService reservationService;

    private static final Logger logger = LoggerFactory.getLogger(PGSReservationController.class);

    @GetMapping
    public List<PGSReservationDTO> getAllReservations() {
        return reservationService.getAllReservations();
    }

    @GetMapping("/search")
    public List<PGSReservationDTO> searchReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) BigDecimal priceFrom,
            @RequestParam(required = false) BigDecimal priceTo,
            @RequestParam(required = false) Integer persons) {

        return reservationService.searchReservations(dateFrom, dateTo, priceFrom, priceTo, persons);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PGSReservationDTO> getReservation(
            @PathVariable Long id
    ) {
        try 
        {
            return ResponseEntity.ok(reservationService.getReservation(id));
        } 
        catch (EntityNotFoundException e) 
        {
            return ResponseEntity.notFound().build();
        }
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
    public ResponseEntity<?> updateReservation(
            @PathVariable Long id,
            @RequestBody PGSReservationDTO dto
    ) {

        if(id==0l)
        {
            try 
            {   //If 0 we will create new
                logger.debug("Received new reservation: ");
                logger.debug(dto.toString());
                return ResponseEntity.ok(reservationService.createReservation(dto));
            } 
            catch (Exception e) 
            {
                logger.error("Greska prilikom kreiranja nove rezervacije", e);
                
                return ResponseEntity.badRequest()
                    .body("ERROR: " + e.getMessage());
            }
        }
        else
        {
            return ResponseEntity.ok(reservationService.updateReservation(id, dto));
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservation(
            @PathVariable Long id
    ) {
        reservationService.deleteReservation(id);
    }
}