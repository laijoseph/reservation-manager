package reservation.manager.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Reservation {
	private int timeBlock;
	private String provider;
	private String patient;

	/**
	 * Clears patient data so reservation can be reused.
	 */
	public void clearPatient() {
		patient = null;
	}
}
