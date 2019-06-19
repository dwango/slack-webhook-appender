package jp.co.dwango.logback;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

/**
 * Logback appender implementation which posts logs to Slack via webhook.
 */
public class SlackWebhookAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    /** default timeout in milliseconds */
    private static final int DEFAULT_TIMEOUT_MILLIS = 50_000;
    
    /** {@link ScriptEngineManager} */
    private static final ScriptEngineManager SCRIPT_MANAGER = new ScriptEngineManager();

    /** string of webhookUrl */
    private String webhookUrlStr;
    
    /** URL of webhookUrl */
    private URL webhookUrl;

    /** payload */
    private String payload;

    /** timeout */
    private int timeout = DEFAULT_TIMEOUT_MILLIS;
    
    /** {@link ScriptEngine} */
    private ScriptEngine engine;
    
    /** for backward compatibility  */
    private final SlackWebhookAppenderBc bc = new SlackWebhookAppenderBc() {
        protected void post(byte[] body) throws IOException {
            SlackWebhookAppender.this.post(body);
        }  
    };

    /**
     * Gets webhookUrl
     * 
     * @return webhookUrl
     */
    public String getWebhookUrl() {
        return this.webhookUrlStr;
    }

    /**
     * Sets webhookUrl
     * 
     * @param webhookUrl WebhookUrl
     */
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrlStr = webhookUrl;
        bc.setWebhookUrl(webhookUrl);
    }

    /**
     * Gets payload
     *
     * @return payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Sets payload
     * 
     * @param payload payload
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    /**
     * Gets timeout
     *
     * @return timeout
     */
    public int getTimeout() {
        return timeout;
    }
    
    /**
     * Sets timeout
     * 
     * @param timeout timeout
     */
    public void setTimeout(String timeout) {
        this.timeout = Integer.parseInt(timeout);
    }

    /**
     * Sets timeout
     * 
     * @param timeout timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @see ch.qos.logback.core.UnsynchronizedAppenderBase#start()
     */
    @Override
    public void start() {
        // backward compatibility mode
        if(this.payload == null) {
            bc.start();
            return;
        }
        
        int errors = 0;
        try {
            initScriptEngine();
        } catch (Exception e) {
            errors++;
            addError(e.getMessage(), e);
        }

        if (webhookUrlStr == null) {
            errors++;
            addError("Webhook URL is not specified.");
        } else {
            try {
                this.webhookUrl = new URL(webhookUrlStr);
            } catch (MalformedURLException e) {
                errors++;
                addError("Webhook URL is not malformed.", e);
            }
        }

        if (errors == 0) {
            super.start();
        }
    }
    
    /**
     * Initializes a {@link ScriptEngine}
     * 
     * @throws ScriptException if an error occurs in payload script.
     * @throws IllegalStateException if {@link #payload} field is null or empty
     */
    private void initScriptEngine() throws ScriptException {
        String payload = this.payload;
        if(payload == null) {
            throw new IllegalStateException("Payload is null");
        }
        payload = payload.trim();
        if(payload.isEmpty()) {
            throw new IllegalStateException("Payload is empty");
        }
        
        // looks up and creates JavaScript engine
        this.engine = SCRIPT_MANAGER.getEngineByName("js");
        
        // define formatTimestamp function
        engine.eval(
            "function formatTimestamp(timestamp) { " +
            "    var odt = java.time.OffsetDateTime.ofInstant( " +
            "        java.time.Instant.ofEpochMilli(timestamp), " +
            "        java.time.ZoneId.systemDefault()); " +
            "    return java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(odt); " +
            "} "
        );
        
        // define payload function
        StringBuilder function = new StringBuilder();
        function.append("function payload(event) {");
        function.append(  "var property = (function() {");
        function.append(      "var lcvo = event.getLoggerContextVO(),");
        function.append(          "properties = lcvo.getPropertyMap();");
        function.append(      "return function(name) {");
        function.append(        "if(!name && name !== 0) {");
        function.append(          "return 'Property_HAS_NO_KEY';");
        function.append(        "}");
        function.append(        "name = '' + name;");
        function.append(        "var value = properties.get(name);");
        function.append(        "return value != null ? value : java.lang.System.getProperty(name);");
        function.append(      "};");
        function.append(    "})(), color, emoji,");
        function.append(    "hostname = property('HOSTNAME'),");
        function.append(    "level = event.getLevel().toString(),");
        function.append(    "timestamp = formatTimestamp(event.getTimeStamp()),");
        function.append(    "message = event.getFormattedMessage();");
        function.append(  "if(level == 'FATAL' || level == 'ERROR') {");
        function.append(    "color = 'danger'; emoji = ':ng_woman:';");
        function.append(  "} else if(level == 'WARN') {");
        function.append(    "color = 'warning'; emoji = ':ok_woman:';");
        function.append(  "} else {");
        function.append(    "color = 'good'; emoji = ':white_circle:';");
        function.append(  "}");
        if(payload.startsWith("{")) {
            function.append("return ");
            function.append(payload);
            function.append(";");
        } else {
            function.append(payload);
        }
        function.append("\n}");
        engine.eval(function.toString());
    }

    /**
     * @see ch.qos.logback.core.UnsynchronizedAppenderBase#append(java.lang.Object)
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        // backward compatibility mode
        if(this.payload == null) {
            bc.append(eventObject);
            return;
        }
        
        try {
            Invocable invocable = (Invocable)engine;
            Object payload = invocable.invokeFunction("payload", eventObject);
            if(payload == null) {
                return;
            }

            Object JSON = engine.get("JSON");
            Object payloadStr = invocable.invokeMethod(JSON, "stringify", payload);
            
            byte[] bodyBytes = payloadStr.toString().getBytes(StandardCharsets.UTF_8);

            post(bodyBytes);
        } catch (Exception e) {
            addError("Failed to post a log to slack.", e);
        }
    }

    /**
     * Posts to Slack
     * 
     * @param body payload body in UTF-8 byte array
     * @throws IOException if an I/O exception occurs.
     */
    protected void post(byte[] body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) webhookUrl.openConnection();
        connection.setReadTimeout(timeout);
        connection.setConnectTimeout(timeout);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setFixedLengthStreamingMode(body.length);
        connection.setRequestProperty("Content-Type", "application/json");
        try(OutputStream os = connection.getOutputStream()) {
            os.write(body);
            os.flush();
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException(connection.getResponseMessage() + "\n" + new String(body, StandardCharsets.UTF_8));
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //   B A C K W A R D   C O M P A T I B I L I T Y
    // 
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    /**
     * Gets slack channel
     * 
     * @return slack channel
     */
    @Deprecated
    public String getChannel() {
        return bc.getChannel();
    }

    /**
     * Sets slack channel
     * 
     * @param channel slack channel
     */
    @Deprecated
    public void setChannel(String channel) {
        bc.setChannel(channel);
    }

    /**
     * Gets username
     * 
     * @return username
     */
    @Deprecated
    public String getUsername() {
        return bc.getUsername();
    }

    /**
     * Sets username
     * 
     * @param username username
     */
    @Deprecated
    public void setUsername(String username) {
        bc.setUsername(username);
    }

    /**
     * Gets iconEmoji 
     * 
     * @return iconEmoji
     */
    @Deprecated
    public String getIconEmoji() {
        return bc.getIconEmoji();
    }

    /**
     * Sets iconEmoji
     * 
     * @param iconEmoji iconEmoji
     */
    @Deprecated
    public void setIconEmoji(String iconEmoji) {
        bc.setIconEmoji(iconEmoji);
    }

    /**
     * Gets iconUrl
     * 
     * @return iconUrl
     */
    @Deprecated
    public String getIconUrl() {
        return bc.getIconUrl();
    }

    /**
     * Sets iconUrl
     * 
     * @param iconUrl iconUrl
     */
    @Deprecated
    public void setIconUrl(String iconUrl) {
        bc.setIconUrl(iconUrl);
    }

    /**
     * Gets linkNames
     * 
     * @return linkNames 
     */
    @Deprecated
    public boolean getLinkNames() {
        return bc.getLinkNames();
    }

    /**
     * Sets linkNames
     * 
     * @param linkNames linkNames
     */
    @Deprecated
    public void setLinkNames(boolean linkNames) {
        bc.setLinkNames(linkNames);
    }

    /**
     * Gets layout
     * 
     * @return layout
     */
    @Deprecated
    public Layout<ILoggingEvent> getLayout() {
        return bc.getLayout();
    }

    /**
     * Sets layout
     * 
     * @param layout
     */
    @Deprecated
    public void setLayout(Layout<ILoggingEvent> layout) {
        bc.setLayout(layout);
    }
}
