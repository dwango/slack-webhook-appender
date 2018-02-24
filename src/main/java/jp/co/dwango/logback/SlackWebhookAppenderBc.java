package jp.co.dwango.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Logback appender implementation which posts logs to Slack via webhook.
 */
public class SlackWebhookAppenderBc extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private static final int TIMEOUT_MILLIS = 50_000;

    private String webhookUrl;

    private String channel;

    private String username;

    private String iconEmoji;

    private String iconUrl;

    private boolean linkNames = true;

    private Layout<ILoggingEvent> layout;

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getChannel() {
        return this.channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIconEmoji() {
        return this.iconEmoji;
    }

    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    public String getIconUrl() {
        return this.iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean getLinkNames() {
        return this.linkNames;
    }

    public void setLinkNames(boolean linkNames) {
        this.linkNames = linkNames;
    }

    public Layout<ILoggingEvent> getLayout() {
        return this.layout;
    }

    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        List<String> fields = new ArrayList<>();

        try {
            String text = createText(eventObject);

            fields.add("\"text\": \"" + escapeQuotes(text) + '"');
            fields.add("\"channel\": \"" + escapeQuotes(checkNotNull(this.channel, "Channel is not specified.")) + '"');
            fields.add("\"username\": \"" + escapeQuotes(checkNotNull(this.username, "Username is not specified.")) + '"');

            if (this.iconEmoji != null) {
                fields.add("\"icon_emoji\": \"" + escapeQuotes(this.iconEmoji) + '"');
            }
            if (this.iconUrl != null) {
                fields.add("\"icon_url\": \"" + escapeQuotes(this.iconUrl) + '"');
            }
            if (this.linkNames) {
                fields.add("\"link_names\": 1");
            }

            String bodyText = "{ " + String.join(", ", fields) + " }";
            byte[] bodyBytes = bodyText.getBytes(StandardCharsets.UTF_8);

            post(bodyBytes);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            addError("Failed to post a log to slack.", e);
        }
    }

    // Visible for testing
    protected void post(byte[] body) throws IOException {

        checkNotNull(this.webhookUrl, "Webhook URL is not specified.");

        URL url = new URL(this.webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setFixedLengthStreamingMode(body.length);
        connection.setRequestProperty("Content-Type", "application/json");
        OutputStream os = connection.getOutputStream();

        os.write(body);
        os.flush();
        os.close();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(connection.getResponseMessage() + "\n" + new String(body, StandardCharsets.UTF_8));
        }
    }

    private String createText(ILoggingEvent event) {
        Layout<ILoggingEvent> layout = this.layout;
        if (layout == null) {
            return event.getFormattedMessage();
        } else {
            return layout.doLayout(event);
        }
    }

    private String escapeQuotes(String str) {
        return str.replaceAll("\"", Matcher.quoteReplacement("\\\""));
    }

    private <T> T checkNotNull(T arg, String message) {
        if (arg == null) {
            throw new IllegalStateException(message);
        }
        return arg;
    }
}
