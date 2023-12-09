package reservation.manager.models;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class ReservationResponse {
    private LocalTime reservationTime;
    private String provider;
}

