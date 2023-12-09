package reservation.manager.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class ReservationsResponse {
    private LocalDate reservationDate;
    private List<ReservationResponse> availableReservations;

}
