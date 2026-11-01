package org.nikahgator.salah;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Finds the coordinates of the masjid's own address, only when the imam asks (one lookup on OpenStreetMap's public
 * search). Nothing else is sent: just the address the masjid already publishes.
 */
@Component
public class Geocoder {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static final Pattern LAT = Pattern.compile("\"lat\"\\s*:\\s*\"(-?[0-9.]+)\"");
    private static final Pattern LON = Pattern.compile("\"lon\"\\s*:\\s*\"(-?[0-9.]+)\"");
    private static final Pattern NAME = Pattern.compile("\"display_name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    @Value("${app.site-url:}")
    private String siteUrl;

    public record Found(double latitude, double longitude, String place) {
    }

    public Optional<Found> find(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&countrycodes=us&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Masjidly/1.0 " + (siteUrl == null || siteUrl.isBlank() ? "" : "(" + siteUrl + ")"))
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                return Optional.empty();
            }
            String body = res.body();
            Matcher lat = LAT.matcher(body), lon = LON.matcher(body), name = NAME.matcher(body);
            if (!lat.find() || !lon.find()) {
                return Optional.empty();
            }
            return Optional.of(new Found(Double.parseDouble(lat.group(1)), Double.parseDouble(lon.group(1)),
                    name.find() ? name.group(1).replace("\\\"", "\"") : ""));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
