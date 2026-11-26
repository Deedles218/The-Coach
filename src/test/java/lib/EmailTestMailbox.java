package lib;

import org.openqa.selenium.json.Json;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** An independent test mailbox. Tokens, message bodies and codes stay in memory. */
public final class EmailTestMailbox {
    private static final String API = "https://api.mail.tm";
    private static final Pattern CODE = Pattern.compile(
            "(?iu)\\b(?:code|código|code de sécurité|codice|sicherheitscode)\\b[^\\d]{0,55}(\\d{4})(?!\\d)");
    private final String address;
    private final String token;

    private EmailTestMailbox(String address, String token) {
        this.address = address;
        this.token = token;
    }

    public static EmailTestMailbox create() throws Exception {
        List<Map<String, Object>> domains = rows(requestAfterThrottle("/domains", null, null));
        String domain = domains.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("isActive"))
                        && !Boolean.TRUE.equals(item.get("isPrivate")))
                .map(item -> (String) item.get("domain"))
                .findFirst().orElseThrow(() -> new IllegalStateException("No active test mailbox domain"));
        Map<String, Object> credentials = new HashMap<>();
        String address = "qatest" + (10000000 + new SecureRandom().nextInt(90000000)) + "@" + domain;
        credentials.put("address", address);
        credentials.put("password", UUID.randomUUID().toString());
        requestAfterThrottle("/accounts", credentials, null);
        Object token = requestAfterThrottle("/token", credentials, null).get("token");
        if (!(token instanceof String) || ((String) token).isEmpty()) {
            throw new IllegalStateException("Test mailbox provider did not return a token");
        }
        return new EmailTestMailbox(address, (String) token);
    }

    public String address() { return address; }

    public Set<String> messageIds() throws Exception {
        Set<String> ids = new HashSet<>();
        for (Map<String, Object> row : rows(requestAfterThrottle("/messages", null, token))) {
            ids.add(messageId(row));
        }
        return ids;
    }

    public String awaitCodeAfter(Set<String> previousIds) throws Exception {
        return awaitCodeAfter(previousIds, 0);
    }

    public String awaitCodeAfter(Set<String> previousIds, long notBeforeMillis) throws Exception {
        if (notBeforeMillis < 0) throw new IllegalArgumentException("Invalid mailbox action boundary");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
        Set<String> inspected = new HashSet<>(previousIds);
        do {
            try {
                for (Map<String, Object> row : messages()) {
                    String id = messageId(row);
                    if (inspected.contains(id)) continue;
                    if (!receivedAfterAction(row, notBeforeMillis)) {
                        inspected.add(id);
                        continue;
                    }
                    String code = findCode(value(row, "subject") + " " + value(row, "intro"));
                    if (code == null) {
                        Map<String, Object> full = request("/messages/" + id, null, token);
                        code = findCode(value(full, "subject") + " " + value(full, "text"));
                    }
                    if (code != null) return code;
                    inspected.add(id);
                }
            } catch (MailboxRateLimit rateLimit) {
                if (!waitForRetry(rateLimit, deadline)) throw rateLimit;
                continue;
            }
            Thread.sleep(3000);
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("No new email containing an OTP reached the test mailbox");
    }

    private List<Map<String, Object>> messages() throws Exception {
        return rows(request("/messages", null, token));
    }

    private static boolean receivedAfterAction(Map<String, Object> row, long notBeforeMillis) {
        if (notBeforeMillis == 0) return true;
        try {
            return Instant.parse(value(row, "createdAt")).toEpochMilli() >= notBeforeMillis;
        } catch (DateTimeParseException invalidTimestamp) {
            return false;
        }
    }

    private static String messageId(Map<String, Object> row) {
        String id = value(row, "id");
        if (!id.matches("[A-Za-z0-9_-]+")) throw new IllegalStateException("Invalid mailbox message ID");
        return id;
    }

    private static String findCode(String body) {
        Matcher match = CODE.matcher(body.replaceAll("\\s+", " "));
        return match.find() ? match.group(1) : null;
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof String ? (String) value : "";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Map<String, Object> response) {
        Object list = response.get("hydra:member");
        if (!(list instanceof List)) throw new IllegalStateException("Invalid test mailbox collection");
        return (List<Map<String, Object>>) list;
    }

    private static Map<String, Object> requestAfterThrottle(String path, Map<String, Object> data, String token)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (true) {
            try {
                return request(path, data, token);
            } catch (MailboxRateLimit rateLimit) {
                if (!waitForRetry(rateLimit, deadline)) throw rateLimit;
            }
        }
    }

    private static boolean waitForRetry(MailboxRateLimit rateLimit, long deadline) throws InterruptedException {
        long remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
        if (rateLimit.delayMillis >= remaining) return false;
        Thread.sleep(rateLimit.delayMillis);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> request(String path, Map<String, Object> data, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API + path).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/ld+json");
        if (token != null) connection.setRequestProperty("Authorization", "Bearer " + token);
        try {
            if (data != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] payload = new Json().toJson(data).getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(payload);
                }
            }
            int status = connection.getResponseCode();
            if (status == 429) throw new MailboxRateLimit(connection.getHeaderField("Retry-After"));
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Test mailbox API returned HTTP " + status);
            }
            try (InputStream stream = connection.getInputStream();
                 ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int length;
                while ((length = stream.read(buffer)) != -1) result.write(buffer, 0, length);
                return new Json().toType(result.toString(StandardCharsets.UTF_8.name()), Map.class);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static final class MailboxRateLimit extends Exception {
        private final long delayMillis;

        MailboxRateLimit(String retryAfter) {
            super("Test mailbox provider returned HTTP 429");
            long seconds = 3;
            if (retryAfter != null && retryAfter.trim().matches("[0-9]{1,9}")) {
                seconds = Math.max(1, Long.parseLong(retryAfter.trim()));
            }
            delayMillis = TimeUnit.SECONDS.toMillis(seconds);
        }
    }
}
