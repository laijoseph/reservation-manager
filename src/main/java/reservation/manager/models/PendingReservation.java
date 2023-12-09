package reservation.manager.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PendingReservation {
	private long expiry;
	private Reservation reservation;
}
