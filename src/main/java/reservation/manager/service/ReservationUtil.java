package reservation.manager.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ReservationUtil {
    private ReservationUtil () {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Extract these as constants so we can switch to 5, 10, 20, 30 minute blocks if necessary (must be a factor of 60),
     * will make refactoring easier if we need to turn these into app settings.
     */
    private static final int LENGTH_IN_MINUTES = 15;

    /**
     * Fraction of the hour is the number of appts in an hour.
     */
    private static final int FRACTION_OF_HOUR = 60/LENGTH_IN_MINUTES;

    /**
     * Converts an appt block into the start time.
     *
     * @param block the index of the block in a day
     * @return LocalTime the time representation of the block
     */
    public static LocalTime convertBlockToTime(int block) {
        return LocalTime.of(block/FRACTION_OF_HOUR, block%FRACTION_OF_HOUR*LENGTH_IN_MINUTES);
    }

    /**
     * Returns the appt block index of the day from a given time.
     *
     * @param time the appointment time, must start at a factor of the hour
     * @return the appt block index
     */
    public static int convertTimeToBlock(LocalTime time) {
        Objects.requireNonNull(time, "Time cannot be null");
        return time.getHour()*FRACTION_OF_HOUR + (int) Math.ceil((double) time.getMinute() /LENGTH_IN_MINUTES);
    }

    /**
     * Generates a reservation key for pending map.
     *
     * @param date the date
     * @param provider the provider
     * @param timeBlock the timeblock
     * @return the generated reservation key for pending map
     */
    public static String generatePendingReservationKey(String date, String provider, int timeBlock) {
        return String.format("%s;%s;%d", date, provider, timeBlock);
    }

    /**
     * Converts a date and time block to {@code LocalDateTime}.
     *
     * @param date the date
     * @param timeBlock the time block
     * @return the LocalDateTime object
     */
    public static LocalDateTime convertDateAndTimeBlockToLocalDateTime(String date, int timeBlock) {
        String localDateTimeAsText = date + " " + convertBlockToTime(timeBlock).format(DateTimeFormatter.ofPattern("HH:mm"));
        return LocalDateTime.parse(localDateTimeAsText, ReservationService.DATE_TIME_FORMATTER);
    }
}
