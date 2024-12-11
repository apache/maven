/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.api;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalQueries;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonotonicTimeTest {

    @Test
    void startShouldBeInitialized() {
        assertNotNull(MonotonicTime.START);
        assertNotNull(MonotonicTime.START.getWallTime());
    }

    @Nested
    class DurationMeasurements {
        @Test
        void shouldMeasurePositiveDurations() throws InterruptedException {
            MonotonicTime start = MonotonicTime.now();
            Thread.sleep(100); // Sleep for 100ms
            MonotonicTime end = MonotonicTime.now();

            Duration duration = end.durationSince(start);
            assertTrue(duration.toMillis() >= 100);
            assertTrue(duration.toMillis() < 150); // Allow some overhead
        }

        @Test
        void durationShouldBeConsistentWithWallTime() {
            MonotonicTime start = MonotonicTime.now();
            MonotonicTime end = MonotonicTime.now();

            Duration durationDirect = end.durationSince(start);
            Duration durationViaWall = Duration.between(start.getWallTime(), end.getWallTime());

            // Should be exactly equal since wall time is computed from the monotonic duration
            assertEquals(durationDirect, durationViaWall);
        }

        @Test
        void durationSinceStartShouldBeConsistent() {
            MonotonicTime time = MonotonicTime.now();
            assertEquals(time.durationSince(MonotonicTime.START), time.durationSinceStart());
        }
    }

    @Nested
    class TemporalAccessor {
        private MonotonicTime time;
        private Instant expectedInstant;

        @BeforeEach
        void setup() {
            time = MonotonicTime.now();
            expectedInstant = time.getWallTime();
        }

        @Test
        void shouldSupportInstantFields() {
            assertTrue(time.isSupported(ChronoField.INSTANT_SECONDS));
            assertTrue(time.isSupported(ChronoField.NANO_OF_SECOND));
        }

        @Test
        void shouldNotSupportLocalFields() {
            assertFalse(time.isSupported(ChronoField.HOUR_OF_DAY));
            assertFalse(time.isSupported(ChronoField.MINUTE_OF_HOUR));
        }

        @Test
        void shouldProvideCorrectValues() {
            assertEquals(
                    expectedInstant.getLong(ChronoField.INSTANT_SECONDS), time.getLong(ChronoField.INSTANT_SECONDS));
            assertEquals(expectedInstant.getLong(ChronoField.NANO_OF_SECOND), time.getLong(ChronoField.NANO_OF_SECOND));
        }

        @Test
        void shouldWorkWithFormatters() {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
            assertEquals(formatter.format(expectedInstant), formatter.format(time));
        }

        @Test
        void shouldSupportQueries() {
            assertEquals(expectedInstant, time.query(Instant::from));

            ZoneId zoneId = time.query(TemporalQueries.zoneId());
            assertEquals(ZoneId.of("UTC"), zoneId); // Changed expectation to UTC

            ZonedDateTime zdt = ZonedDateTime.from(time);
            assertEquals(expectedInstant.atZone(ZoneId.of("UTC")), zdt);
        }
    }

    @Nested
    class WallTimeComputation {
        @Test
        void wallTimeShouldBeLazilyComputed() {
            // This test is a bit implementation-dependent but helps verify the lazy loading
            MonotonicTime time = MonotonicTime.now();

            // First call should compute the wall time
            Instant first = time.getWallTime();

            // Second call should return the same instance
            Instant second = time.getWallTime();

            assertSame(first, second);
        }

        @Test
        void wallTimeShouldBeConsistentWithMonotonicTime() throws InterruptedException {
            MonotonicTime start = MonotonicTime.now();
            Thread.sleep(100);
            MonotonicTime end = MonotonicTime.now();

            Duration monotonic = end.durationSince(start);
            Duration wall = Duration.between(start.getWallTime(), end.getWallTime());

            // Should be exactly equal since wall time is derived from monotonic
            assertEquals(monotonic, wall);
        }
    }

    @Test
    void equalsShouldOnlyConsiderMonotonicTime() {
        MonotonicTime time1 = MonotonicTime.now();
        MonotonicTime time2 = new MonotonicTime(time1.getNanoTime(), Instant.EPOCH);

        // Should be equal despite different wall times since they have the same monotonic time
        assertEquals(time1, time2);
        assertEquals(time1.hashCode(), time2.hashCode());
    }

    @Test
    void toStringShouldIncludeBothTimes() {
        MonotonicTime time = MonotonicTime.now();
        String str = time.toString();

        assertTrue(str.contains("wall="));
        assertTrue(str.contains("duration="));
        assertTrue(str.contains(time.getWallTime().toString()));
    }

    @Nested
    class NanoTimeAccess {
        @Test
        void getNanoTimeShouldBeMonotonic() {
            MonotonicTime time1 = MonotonicTime.now();
            MonotonicTime time2 = MonotonicTime.now();

            // Later time should have greater nanoTime
            assertTrue(time2.getNanoTime() > time1.getNanoTime());
        }

        @Test
        void nanoTimeShouldMatchDuration() {
            MonotonicTime time1 = MonotonicTime.now();
            MonotonicTime time2 = MonotonicTime.now();

            // Duration in nanos should equal difference of nanoTime values
            assertEquals(
                    time2.getNanoTime() - time1.getNanoTime(),
                    time2.durationSince(time1).toNanos());
        }

        @Test
        void startNanoTimeShouldBeAccessible() {
            // Verify we can access START's nanoTime
            long startNanos = MonotonicTime.START.getNanoTime();
            MonotonicTime now = MonotonicTime.now();

            // Current time should be after START
            assertTrue(now.getNanoTime() > startNanos);

            // Duration since START should match nanoTime difference
            assertEquals(
                    now.getNanoTime() - startNanos, now.durationSinceStart().toNanos());
        }
    }

    @Nested
    class TemporalSupport {
        @Test
        void shouldSupportTimeBasedUnits() {
            MonotonicTime time = MonotonicTime.now();

            // Should support only time-based units
            assertTrue(time.isSupported(ChronoUnit.NANOS));
            assertTrue(time.isSupported(ChronoUnit.MICROS));
            assertTrue(time.isSupported(ChronoUnit.MILLIS));
            assertTrue(time.isSupported(ChronoUnit.SECONDS));
            assertTrue(time.isSupported(ChronoUnit.MINUTES));
            assertTrue(time.isSupported(ChronoUnit.HOURS));

            // DAYS and above are estimated due to daylight savings etc
            assertFalse(time.isSupported(ChronoUnit.DAYS));
            assertFalse(time.isSupported(ChronoUnit.WEEKS));
            assertFalse(time.isSupported(ChronoUnit.MONTHS));
            assertFalse(time.isSupported(ChronoUnit.YEARS));
        }

        @Test
        void shouldNotSupportEstimatedUnits() {
            MonotonicTime time = MonotonicTime.now();

            // Should not support estimated units
            assertFalse(time.isSupported(ChronoUnit.WEEKS));
            assertFalse(time.isSupported(ChronoUnit.MONTHS));
            assertFalse(time.isSupported(ChronoUnit.YEARS));
        }

        @Test
        void shouldHandleUntilWithMonotonicTime() {
            MonotonicTime start = MonotonicTime.now();
            MonotonicTime end = new MonotonicTime(start.getNanoTime() + TimeUnit.SECONDS.toNanos(5), null);

            assertEquals(5_000_000_000L, start.until(end, ChronoUnit.NANOS));
            assertEquals(5_000_000, start.until(end, ChronoUnit.MICROS));
            assertEquals(5_000, start.until(end, ChronoUnit.MILLIS));
            assertEquals(5, start.until(end, ChronoUnit.SECONDS));
        }

        @Test
        void shouldHandleUntilWithInstant() {
            MonotonicTime start = MonotonicTime.now();
            Instant end = start.getWallTime().plusSeconds(5);

            assertEquals(5, start.until(end, ChronoUnit.SECONDS));
        }

        @Test
        void shouldRejectPlusOperation() {
            MonotonicTime time = MonotonicTime.now();
            assertThrows(UnsupportedTemporalTypeException.class, () -> time.plus(1, ChronoUnit.SECONDS));
        }
    }

    @Nested
    class CalendarFields {
        @Test
        void shouldSupportCalendarFields() {
            MonotonicTime time = MonotonicTime.now();

            // These fields are not supported by Instant but should work through ZonedDateTime conversion
            assertDoesNotThrow(() -> time.getLong(ChronoField.YEAR_OF_ERA));
            assertDoesNotThrow(() -> time.getLong(ChronoField.MONTH_OF_YEAR));
            assertDoesNotThrow(() -> time.getLong(ChronoField.DAY_OF_MONTH));

            // Verify actual values
            ZonedDateTime expected = time.getWallTime().atZone(ZoneId.of("UTC"));
            assertEquals(expected.getLong(ChronoField.YEAR_OF_ERA), time.getLong(ChronoField.YEAR_OF_ERA));
            assertEquals(expected.getLong(ChronoField.MONTH_OF_YEAR), time.getLong(ChronoField.MONTH_OF_YEAR));
            assertEquals(expected.getLong(ChronoField.DAY_OF_MONTH), time.getLong(ChronoField.DAY_OF_MONTH));
        }

        @Test
        void shouldWorkWithDateTimeFormatter() {
            MonotonicTime time = MonotonicTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

            // Should not throw
            assertDoesNotThrow(() -> formatter.format(time));

            // Should match UTC ZonedDateTime formatting
            String expected = formatter.format(time.getWallTime().atZone(ZoneId.of("UTC")));
            String actual = formatter.format(time);
            assertEquals(expected, actual);
        }
    }
}
