package reservation.manager.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reservation.manager.models.Reservation;

import java.time.LocalDateTime;
import java.time.LocalTime;

class ReservationUtilTest {

    /**
     * Tests {@link ReservationUtil#convertBlockToTime(int)}.
     */
    @Test
    void testConvertBlockToTimeWhenBlockIsValidExpectValueToBeAccurate() {

        // given
        int block = 71; // 5:45 pm = 17th hour, 3rd quarter of the hour, so we should be getting 17*4+3

        // when
        LocalTime test = ReservationUtil.convertBlockToTime(block);

        // then
        Assertions.assertEquals(LocalTime.of(17,45), test);
    }

    /**
     * Tests {@link ReservationUtil#convertTimeToBlock(LocalTime)}
     */
    @Test
    void testConvertTimeToBlockWhenTimeIs330pmExpect62() {
        // given
        LocalTime time = LocalTime.of(15,30);
        // when
        int test = ReservationUtil.convertTimeToBlock(time);
        // then
        Assertions.assertEquals(62, test);
    }

    /**
     * Tests {@link ReservationUtil#generatePendingReservationKey(String, String, int)}.
     */
    @Test
    void generatePendingReservationKeyWhenProvidedValidInputsExpectCorrectKey() {
        // given
        String expected = "2023-12-08;someProviderId;87";

        // when
        String test = ReservationUtil.generatePendingReservationKey("2023-12-08", "someProviderId", 87);

        // then
        Assertions.assertEquals(expected, test);

    }

    /**
     * Tests {@link ReservationUtil#convertDateAndTimeBlockToLocalDateTime(String, int)}.
     */
    @Test
    void testConvertDateAndTimeBlockToLocalDateTimeWhenDateIs20231208AndBlockIs48Expect1215Pm() {
        // given
        String date = "2023-12-08";
        int block = 49;

        // when
        LocalDateTime test = ReservationUtil.convertDateAndTimeBlockToLocalDateTime(date,block);

        // then
        Assertions.assertEquals(LocalDateTime.of(2023,12,8,12,15), test);
    }
}