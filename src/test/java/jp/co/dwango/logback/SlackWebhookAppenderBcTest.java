package jp.co.dwango.logback;

import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SlackWebhookAppenderBcTest {

    static class AppenderForTest extends SlackWebhookAppender {
        public byte[] body = null;

        @Override
        protected void post(byte[] body) throws IOException {
            this.body = body;
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testBuildsMessage() {
        AppenderForTest appender = new AppenderForTest();
        appender.setChannel("channel");
        appender.setUsername("username");
        appender.setIconEmoji("icon-emoji");
        appender.setIconUrl("icon-url");
        appender.setLinkNames(true);

        LoggingEvent event = new LoggingEvent();
        event.setMessage("text \"quoted\"");

        appender.append(event);

        String actual = new String(appender.body, StandardCharsets.UTF_8);
        String expected = "{ \"text\": \"text \\\"quoted\\\"\", \"channel\": \"channel\", \"username\": \"username\", \"icon_emoji\": \"icon-emoji\", \"icon_url\": \"icon-url\", \"link_names\": 1 }";
        Assert.assertEquals(expected, actual);
    }

    @Ignore
    public void testPost() {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.error("nyaos");
    }
}
