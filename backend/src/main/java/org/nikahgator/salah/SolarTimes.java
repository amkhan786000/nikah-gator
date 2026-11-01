package org.nikahgator.salah;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Sunrise and sunset for a place and date, calculated locally (no outside service) with the standard sunrise
 * equation. Accurate to about a minute at US latitudes, which is what a posted timetable needs.
 */
public final class SolarTimes {
    private SolarTimes() {
    }

    public record Sun(LocalTime sunrise, LocalTime sunset) {
    }

    /** The sun's rise and set on {@code date} at the place, in the place's time zone; empty when it does not rise or set that day. */
    public static Optional<Sun> of(LocalDate date, double latitude, double longitude, ZoneId zone) {
        double phi = Math.toRadians(latitude);
        double n = date.toEpochDay() + 2440588 - 2451545.0 + 0.0008;      // days since J2000 at local solar noon (approx.)
        double jStar = n - longitude / 360.0;                              // mean solar time (west longitudes are negative)
        double m = Math.toRadians((357.5291 + 0.98560028 * jStar) % 360);  // solar mean anomaly
        double c = 1.9148 * Math.sin(m) + 0.0200 * Math.sin(2 * m) + 0.0003 * Math.sin(3 * m);
        double lambda = Math.toRadians((Math.toDegrees(m) + c + 180 + 102.9372) % 360); // ecliptic longitude
        double jTransit = 2451545.0 + jStar + 0.0053 * Math.sin(m) - 0.0069 * Math.sin(2 * lambda);
        double sinDelta = Math.sin(lambda) * Math.sin(Math.toRadians(23.4397));
        double delta = Math.asin(sinDelta);
        double cosOmega = (Math.sin(Math.toRadians(-0.833)) - Math.sin(phi) * sinDelta) / (Math.cos(phi) * Math.cos(delta));
        if (cosOmega < -1 || cosOmega > 1) {
            return Optional.empty(); // polar day or night
        }
        double omega = Math.toDegrees(Math.acos(cosOmega));
        return Optional.of(new Sun(local(jTransit - omega / 360.0, zone), local(jTransit + omega / 360.0, zone)));
    }

    /** A Julian date as a local time, rounded to the nearest minute. */
    private static LocalTime local(double julian, ZoneId zone) {
        long epochSeconds = Math.round((julian - 2440587.5) * 86400.0);
        return Instant.ofEpochSecond(epochSeconds).atZone(zone).toLocalTime().plusSeconds(30).truncatedTo(ChronoUnit.MINUTES);
    }
}
