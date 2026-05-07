package unillm.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public final class HttpJson {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpJson() {
    }

    public static JsonNode postJson(HttpClient client, String url, Map<String, String> headers, JsonNode body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));

        headers.forEach(builder::header);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " from " + sanitizeUrl(url) + ": " + response.body());
        }
        return MAPPER.readTree(response.body());
    }

    public static JsonNode getJson(HttpClient client, String url, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .GET();

        headers.forEach(builder::header);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " from " + sanitizeUrl(url) + ": " + response.body());
        }
        return MAPPER.readTree(response.body());
    }

    private static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        return url
                .replaceAll("([?&](?:key|api_key|token)=)[^&]*", "$1***")
                .replaceAll("([?&](?:access_token)=)[^&]*", "$1***");
    }
}
