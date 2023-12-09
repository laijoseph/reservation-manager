package reservation.manager.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reservation.manager.models.Reservation;
import reservation.manager.models.ReservationResponse;
import reservation.manager.models.ReservationsResponse;
import reservation.manager.service.ReservationService;
import reservation.manager.service.ReservationUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reservations")
@ComponentScan
public class ReservationController {
	private final ReservationService reservationService;

	@Autowired
	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@GetMapping("/{date}")
	public ResponseEntity<ReservationsResponse> getReservationsByDate(@PathVariable LocalDate date) {
		try{
			Set<Reservation> reservations = reservationService.getAvailability(date);
			if(CollectionUtils.isEmpty(reservations)) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No reservations available.");
			} else {
				return new ResponseEntity<>(
						new ReservationsResponse(date,
								reservations.stream().map(e -> ReservationResponse.builder()
						.reservationTime(ReservationUtil.convertBlockToTime(e.getTimeBlock()))
						.provider(e.getProvider())
						.build())
										// since we're sorting here, this method will run in O(nlogn), but that's
										// better than sorting it every time we're manipulating the reservations,
										// where n is the average number of appts per day
										.sorted((a, b) -> a.getReservationTime().compareTo(b.getReservationTime()))
										.collect(Collectors.toList())), HttpStatus.OK);
			}
		} catch (DateTimeParseException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format.");
		}
	}

	@GetMapping()
	public ResponseEntity<List<ReservationsResponse>> getReservations() {
		Map<String, Set<Reservation>> reservations = reservationService.getAvailability();
		List<ReservationsResponse> response =
		reservations.entrySet().stream().map(e ->
				new ReservationsResponse(LocalDate.parse(e.getKey(), ReservationService.DATE_FORMATTER),
				e.getValue().stream()

						.map(reservation -> ReservationResponse.builder()
						.reservationTime(ReservationUtil.convertBlockToTime(reservation.getTimeBlock()))
						.provider(reservation.getProvider())
						.build())
						// same situation as before O(nlogn) where n is the average number of reservations per days
						.sorted((a,b) -> a.getReservationTime().compareTo(b.getReservationTime()))
						.collect(Collectors.toList())
				))
				// O(mlogm) where m is the number of days. I _think_ this makes our time complexity of this method O
				// (mlogm*nlogn).  If we didn't sort, we could get this in O(1) time (sorting isn't a requirement)
				.sorted((a,b) -> a.getReservationDate().compareTo(b.getReservationDate()))
				.collect(Collectors.toList());
		if(CollectionUtils.isEmpty(response)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No Reservations available.");
		} else {
			return new ResponseEntity<>(response, HttpStatus.OK);
		}
	}

	@PutMapping("/addAvailability/{date}")
	public ResponseEntity<String> addAvailability(@RequestParam String provider, @PathVariable LocalDate date,
											@RequestParam LocalTime start,
											@RequestParam LocalTime end) {
		reservationService.addAvailability(provider, date, start, end);
		return ResponseEntity.ok().body("Availability added.");
	}

	//@todo if time permit, add removeAvailability (or I guess a provider can just book their own time to remove it)

	@PutMapping("/reserve/{date}")
	public ResponseEntity<String> reserve(@RequestParam String provider, @PathVariable LocalDate date,
										  @RequestParam LocalTime start, @RequestParam String patient) {
		boolean successful = reservationService.reserve(provider, patient,date, start);

		return successful
				? ResponseEntity.ok().body("Reserved! Remember to confirm within 30 min!")
				: ResponseEntity.badRequest().body("Unable to book appt.");
	}

	@PutMapping("/confirm/{date}")
	public ResponseEntity<String> confirm(@RequestParam String provider, @PathVariable LocalDate date,
								  @RequestParam LocalTime start, @RequestParam String patient) {
		boolean successful = reservationService.confirm(provider, patient, date.toString(), start);

		return successful
				? ResponseEntity.ok().body("Reservation confirmed!")
				: ResponseEntity.badRequest().body("Unable to find reservation to confirm.");
	}

	@GetMapping("/confirmed")
	public Map<String, Set<Reservation>> getConfirmedReservations() {
		return reservationService.getConfirmedReservations();
	}
}
