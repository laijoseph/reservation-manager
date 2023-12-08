package reservation.manager.service;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reservation.manager.models.PendingReservation;
import reservation.manager.models.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Reservation service to manage reservations.
 */
@Service
@PropertySource("classpath:application.properties")
public class ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationService.class);

    private static final long THIRTY_MIN_IN_MS = DateUtils.MILLIS_PER_HOUR / 2;

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    /**
     * Use set for fast read/write, tradeoff will be unsorted, but we can get all avail reservations for any given day
     */
    private final Map<String, Set<Reservation>> openReservations;
    private final Map<String, PendingReservation> pendingReservations;

    /**
     * Requirements don't specify a need for confirmed reservations, but we should still keep track
     */
    private final Map<String, Set<Reservation>> confirmedReservations;

    private long pendingTtl;

    /**
     * Constructor for the class.
     */
    public ReservationService() {
        openReservations = new HashMap<>();
        pendingReservations = new HashMap<>();
        confirmedReservations = new HashMap<>();
        pendingTtl = THIRTY_MIN_IN_MS;
    }

    /**
     * Constructor for testing.
     */
    public ReservationService(long overrideTtl) {
        openReservations = new HashMap<>();
        pendingReservations = new HashMap<>();
        confirmedReservations = new HashMap<>();
        pendingTtl = overrideTtl;
    }



    /**
     * Adds to availability map.
     *
     * @param provider the provider
     * @param date     the day (key)
     * @param start    start time
     * @param end      end time must be on the same day
     */
    public void addAvailability(String provider, LocalDate date, LocalTime start, LocalTime end) {

        // Prevent adding within 24hrs
        LocalDate today = LocalDate.now();

        // Only perform add operations if date is greater than today
        if (date.isAfter(today)) {
            int startBlock = ReservationUtil.convertTimeToBlock(start);
            int endBlock = ReservationUtil.convertTimeToBlock(end);

            /* Check if date is tomorrow. If it is, we need to filter out timeblocks before now
            (because of the 24hr in advance rule */
            if (date.isEqual(today.plusDays(1))) {
                startBlock = Math.max(startBlock, timeBlockOfNow());
            }

            Set<Reservation> reservations = openReservations.get(date.toString());
            if (reservations == null) {
                openReservations.put(date.toString(), new HashSet<>());
                reservations = openReservations.get(date.toString()); // update reference to new list
            }

            /* End block is not included because the last appt should be end-1.
               (e.g. end time is 15:00, we want the last appt to be at 14:45 */
            for (int i = startBlock; i < endBlock; i++) {
                reservations.add(Reservation.builder().provider(provider).timeBlock(i).build());
                LOGGER.debug("Availability added for provider {}, day {}, time block {}.", provider, date, i);
            }
        } else {
            LOGGER.debug("Availability for provider {}, day {} was not added: Date is not outside of 24hr window.",
                    provider, date);
        }
    }

    /**
     * Returns the entire availability map.
     *
     * @return  the entire availability map
     */
    public Map<String, Set<Reservation>> getAvailability() {
        refreshOpenReservations();
        refreshPendingReservations();
        return Collections.unmodifiableMap(openReservations);
    }

    /**
     * Returns the open appts for that day.
     *
     * @param date string in yyyy-MM-dd format
     * @return the open appts for that day
     */
    public Set<Reservation> getAvailability(LocalDate date) {
        refreshOpenReservations();
        refreshPendingReservations();
        Set<Reservation> reservations = openReservations.get(date.toString());
        if(CollectionUtils.isEmpty(reservations)) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(openReservations.get(date.toString()));
    }

    /**
     * Attempts to reserve timeslot with a provider.
     *
     * @param provider the provider
     * @param patient  the patient
     * @param date     of desired reservation
     * @param time     of desired reservation
     * @return {@code true} if slot is available, {@code false} otherwise
     */
    public boolean reserve(String provider, String patient, LocalDate date, LocalTime time) {
        // Update reservation map
        refreshPendingReservations();
        refreshOpenReservations();

        // Check if date is avail
        Set<Reservation> reservations = openReservations.get(date.toString());
        if (CollectionUtils.isEmpty(reservations)) {
            LOGGER.warn("No reservations available on {}", date);
            return false;
        }
        Reservation targetReservation = Reservation.builder()
                .provider(provider)
                .timeBlock(ReservationUtil.convertTimeToBlock(time))
                .build();

        if (reservations.contains(targetReservation)) { // Check if target reservation is available
            reservations.remove(targetReservation);
            targetReservation.setPatient(patient);
            pendingReservations.put(ReservationUtil.generatePendingReservationKey(date.toString(), provider,
                            targetReservation.getTimeBlock()),
                    new PendingReservation(System.currentTimeMillis() + pendingTtl, targetReservation));
            return true;
        }
        return false;
    }

    /**
     * Confirms an active pending reservation.
     *
     * @param provider the provider
     * @param patient the patient
     * @param date the reservation date
     * @param time the reservation time
     * @return  {@code true} if active reservation was found, {@code false} otherwise
     */
    public boolean confirm(String provider, String patient, String date, LocalTime time) {
        String key = ReservationUtil.generatePendingReservationKey(date, provider,
                ReservationUtil.convertTimeToBlock(time));
        PendingReservation pending = pendingReservations.get(key);

        // We don't need a null check for getPatient() because it can't be null if it's in this map
        if (pending == null || !pending.getReservation().getPatient().equals(patient)) {

            /* Reservation for this person at this time with this provider doesn't exist */
            return false;
        } else if (pending.getExpiry() < System.currentTimeMillis()) { // Check if reservation is invalid
            Reservation expiredReservation = pending.getReservation();
            expiredReservation.clearPatient();
            openReservations.get(date).add(expiredReservation);
            pendingReservations.remove(key);
            return false;
        } else { // reservation is valid!
            Reservation validReservation = pending.getReservation();
            Set<Reservation> reservationsForDate = confirmedReservations.getOrDefault(date, new HashSet<>());
            reservationsForDate.add(validReservation);
            confirmedReservations.put(date, reservationsForDate);
            return true;
        }
    }

    /**
     * Updates pendingReservations by removing timed out reservations from pending, and adding them back to
     * openReservations.
     */
    private void refreshPendingReservations() {
        LocalDateTime in24Hrs = LocalDateTime.now().plusDays(1);
        for (Map.Entry<String, PendingReservation> pending : pendingReservations.entrySet()) {
            if (pending.getValue().getExpiry() < System.currentTimeMillis()) {
                //check if it is within 24hrs
                String date = pending.getKey().split(";")[0];
                LocalDateTime reservationDateTime = ReservationUtil.convertDateAndTimeBlockToLocalDateTime(date,
                        pending.getValue().getReservation().getTimeBlock());
                if (reservationDateTime.isAfter(in24Hrs)) {
                    pendingReservations.remove(pending.getKey());
                    Reservation resettingRes = pending.getValue().getReservation();
                    resettingRes.clearPatient();
                    openReservations.get(date).add(resettingRes); // put the reservation back into open
                }
            }
        }
    }

    /**
     * Removes all appt blocks within 24 hrs.
     */
    private void refreshOpenReservations() {
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FORMATTER); // "2023-12-08"
        String tomorrow = today.plusDays(1).toString(); // "2023-12-09"

        // Step1: Remove all of today
        openReservations.remove(todayStr);

        // Step2 Remove all of tomorrow occurs within 24hrs
        Set<Reservation> tomorrowReservations = openReservations.get(tomorrow);
        if (!CollectionUtils.isEmpty(tomorrowReservations)) {
            // remove the timeblock of less than now (now rounds up to the next quarter hour)
            tomorrowReservations.removeIf(reservation -> reservation.getTimeBlock() < timeBlockOfNow());
        }
    }

    /**
     * Returns the confirmed reservation map.
     *
     * @return the confirmed reservation map.
     */
    public Map<String, Set<Reservation>> getConfirmedReservations() {
        return confirmedReservations;
    }

    /**
     * Returns the time block of current time.
     *
     * @return  the time block of current time
     */
    private int timeBlockOfNow() {
        return ReservationUtil.convertTimeToBlock(LocalTime.now());
    }
}
