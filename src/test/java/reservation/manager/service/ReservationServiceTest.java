package reservation.manager.service;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reservation.manager.models.Reservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    private ReservationService classUnderTest;

    /**
     * Sets up tests.
     */
    @BeforeEach
    void setUp() {
        classUnderTest = new ReservationService();
        classUnderTest.setPendingTtl(1000);
    }

    /**
     * Since this is a unit test, I'd normally set up and test each method individually, but I'm running out of time.
     */
    @Test
    void testReservationServiceWhenWorkflowIsValid() {

        // Given
        String providerId = "Dr. House";
        LocalDate providerAvailableDate = LocalDate.of(2050, 01, 01);
        LocalTime providerAvailableStartTime = LocalTime.of(7, 30); //  7:30am
        LocalTime providerAvailableEndTime = LocalTime.of(12, 00); //  OOO at noon for lunch

        String patientId = "992-GT3-RS";
        // we'll use this to test the case where Dr. House just left the office to go on lunch.
        LocalTime unacceptableReservationTime = LocalTime.of(12, 00); // noon
        LocalTime acceptableDesiredReservationTime = LocalTime.of(9, 15); // 9:15am


        // When & then are interlaced

        // Starting fresh, no availability
        Assertions.assertTrue(classUnderTest.getAvailability().isEmpty());
        // Add 4 availabilities for 2050-01-01 from 9am to 10am
        classUnderTest.addAvailability(providerId, providerAvailableDate,
                                        providerAvailableStartTime, providerAvailableEndTime);

        // Availabilities exist for this day
        assertNotNull(classUnderTest.getAvailability(providerAvailableDate));

        //btwn 7:30-noon, there are 4.5 hours, resulting in 18 available timeslots
        assertEquals(18, classUnderTest.getAvailability(providerAvailableDate).size());

        // try to book with an unavail provider
        assertFalse(classUnderTest.reserve("I don't even work here", patientId,providerAvailableDate,providerAvailableStartTime));

        // try to book an unavail time
        assertFalse(classUnderTest.reserve(providerId, patientId,providerAvailableDate,unacceptableReservationTime));

        // book valid time
        assertTrue(classUnderTest.reserve(providerId,patientId,providerAvailableDate,acceptableDesiredReservationTime));
        assertEquals(17, classUnderTest.getAvailability(providerAvailableDate).size()); // 1 valid spot was taken

        // No one else should be able to book this reservation that is awaiting confirmation
        assertFalse(classUnderTest.reserve(providerId,"718-GT4",providerAvailableDate,
                acceptableDesiredReservationTime));

        // User should not be able to confirm a reservation with the wrong details
        assertFalse(classUnderTest.confirm(providerId,patientId,providerAvailableDate.toString(),unacceptableReservationTime));

        // let the pending reservation expire
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // pending reservation timed out, reservations for that day should be available again
        assertEquals(18, classUnderTest.getAvailability(providerAvailableDate).size());

        // book a timeslot and confirm the reservation
        assertTrue(classUnderTest.reserve(providerId,patientId,providerAvailableDate,acceptableDesiredReservationTime));
        assertTrue(classUnderTest.confirm(providerId,patientId,providerAvailableDate.toString(),
                acceptableDesiredReservationTime));

        // Check the confirmed reservations for the reserved slot.
        Reservation reservation = classUnderTest.getConfirmedReservations()
                                                .get(providerAvailableDate.toString())
                                                .stream().findFirst().orElse(null);
        assertNotNull(reservation);
        assertEquals(providerId, reservation.getProvider());
        assertEquals(patientId, reservation.getPatient());
        // 9:15am == 37 because 9th hour is the 36th time block, and n:15 is the first quarter of the hour,
        assertEquals(37,reservation.getTimeBlock());
    }
}