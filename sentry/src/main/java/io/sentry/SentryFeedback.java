package io.sentry;

import io.sentry.dsn.Dsn;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SentryFeedback {

    public static SentryFeedback build() {
        return new SentryFeedback();
    }

    public static boolean send(String name, String email, String comments, String eventId) {
        return new SentryFeedback(name, email, comments, eventId).send();
    }

    private String comments;
    private String email;
    private String name;
    private String eventId;

    private SentryFeedback() {

    }

    private SentryFeedback(String name, String email, String comments, String eventId) {
        this.comments = comments;
        this.email = email;
        this.name = name;
        this.eventId = eventId;
    }

    /*
     * Ignore IntelliJ Java 10+ recommendations about URLEncoder
     */
    @SuppressWarnings("all")
    private byte[] toFormURLEncoded() throws UnsupportedEncodingException {

        String result = URLEncoder.encode("name", "UTF-8") +
                "=" +
                URLEncoder.encode(name, "UTF-8") +
                "&" +
                URLEncoder.encode("email", "UTF-8") +
                "=" +
                URLEncoder.encode(email, "UTF-8") +
                "&" +
                URLEncoder.encode("comments", "UTF-8") +
                "=" +
                URLEncoder.encode(comments, "UTF-8") +
                "&";

        return result.getBytes(StandardCharsets.UTF_8);
    }

    public SentryFeedback setComments(String comments) {
        this.comments = comments;
        return this;
    }

    public SentryFeedback setEmail(String email) {
        this.email = email;
        return this;
    }

    public SentryFeedback setName(String name) {
        this.name = name;
        return this;
    }

    public SentryFeedback setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public String getEventId() {
        return eventId;
    }

    private boolean validate() {

        if (name == null || comments == null || email == null) {
            return false;
        }

        if (name.trim().isEmpty()) {
            return false;
        }

        if (comments.trim().isEmpty()) {
            return false;
        }

        return !email.trim().isEmpty();
    }

    public boolean send() {

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new NullPointerException("Event Id must be provided");
        }

        if (!validate()) {
            throw new IllegalArgumentException("Invalid feedback information, name, comment and email must be provided");
        }

        String path = "https://sentry.io/api/embed/error-page/?eventId=" + eventId + "&dsn=" + Dsn.dsnLookup();

        try {
            URL url = new URL(path);

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.addRequestProperty("Accept", "*/*");
            connection.addRequestProperty("Accept-Encoding", "gzip, deflate, br");
            connection.addRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");

            connection.connect();

            try (OutputStream os = connection.getOutputStream()) {
                os.write(toFormURLEncoded());
            }

            int response = connection.getResponseCode();

            connection.disconnect();

            return response == 200;

        } catch (IOException ex) {
            Logger.getLogger(SentryFeedback.class.getName()).log(Level.SEVERE, "Error on sending feedback to sentry", ex);
        }

        return false;
    }

}
