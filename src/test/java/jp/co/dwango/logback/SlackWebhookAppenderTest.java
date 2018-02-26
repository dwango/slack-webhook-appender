package jp.co.dwango.logback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEvent;

public class SlackWebhookAppenderTest {

    static class AppenderForTest extends SlackWebhookAppender {
        public byte[] body = null;

        @Override
        protected void post(byte[] body) throws IOException {
            this.body = body;
        }
    }

    @Test
    public void testBuildsMessage() {
        AppenderForTest appender = new AppenderForTest();
        appender.setWebhookUrl("https://hooks.slack.com/services/ABCDEF/GHIJK/LMNOPQR");
        appender.setPayload(
            "{ " +
            "  \"channel\": \"#channel\", " +
            "  \"username\": \"username\", " +
            "  \"icon_emoji\": emoji, " +
            "  \"link_names\": 1, " +
            "  \"attachments\": [{ " +
            "    \"title\": \"title - \" + hostname, " +
            "    \"color\": color, " +
            "    \"fields\": [{ " +
            "      \"title\": \"Message\", " +
            "      \"value\": event.getFormattedMessage(), " +
            "      \"short\": false " +
            "    }] " +
            "  }] " +
            "} ");
        appender.start();

        Map<String, String> properties = new HashMap<>();
        properties.put("CONTEXT_NAME", "textContext");
        properties.put("HOSTNAME", "test.local");
        
        LoggingEvent event = new LoggingEvent();
        event.setLevel(Level.INFO);
        event.setMessage("text \"quoted\"");
        event.setLoggerContextRemoteView(new LoggerContextVO("test", properties, 123L));
        
        appender.append(event);
        
        String actual = new String(appender.body, StandardCharsets.UTF_8);
        String expected = "{\"channel\":\"#channel\",\"username\":\"username\",\"icon_emoji\":\":white_circle:\",\"link_names\":1,\"attachments\":[{\"title\":\"title - test.local\",\"color\":\"good\",\"fields\":[{\"title\":\"Message\",\"value\":\"text \\\"quoted\\\"\",\"short\":false}]}]}";
        Assert.assertEquals(expected, actual);
    }

    @Ignore
    public void testPost() {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.error("nyaos");
    }
}
