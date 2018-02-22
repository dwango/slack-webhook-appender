# slack-webhook-appender
Logback appender which posts logs to slack via incoming webhook.

[![Build Status](https://travis-ci.org/dwango/slack-webhook-appender.svg?branch=master)](https://travis-ci.org/dwango/slack-webhook-appender)

## Usage

### pom.xml

```xml
    <repositories>
        <repository>
            <id>slack-webhook-appender</id>
            <url>https://raw.github.com/dwango/slack-webhook-appender/mvn-repo/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>jp.co.dwango</groupId>
            <artifactId>slack-webhook-appender</artifactId>
            <version>1.0</version> <!-- replace with the latest version -->
        </dependency>
    </dependencies>
```

### logback.xml

```xml
<configuration>

    <appender name="SLACK" class="jp.co.dwango.logback.SlackWebhookAppender">
        <webhookUrl>...</webhookUrl>
        <timeout>50000</timeout>
        <payload>
            {
              "channel": "#_channel",
              "username": "username",
              "icon_emoji": emoji,
              "link_names": 1,
              "attachments": [{
                "title": level + " (" + hostname + ")",
                "fallback": level + " (" + hostname + ")",
                "color": color,
                "fields": [{
                  "title": "Hostname",
                  "value": hostname,
                  "short": true
                }, {
                  "title": "Time",
                  "value": timestamp,
                  "short": true
                }, {
                  "title": "Level",
                  "value": level,
                  "short": true
                }, {
                  "title": "Trigger",
                  "value": message,
                  "short": false
                }]
              }]
            }
        </payload>
    </appender>

    <appender name="ASYNC_SLACK" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="SLACK" />
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
    </appender>

    <root level="INFO">
        <appender-ref ref="ASYNC_SLACK" />
    </root>

</configuration>
```

#### Appender options

|Key|Required|Detail|
|:----|:----|:----|
|webhookUrl|Y|URL of incoming webhook.|
|timeout|N|Timeout of posting to Slack in milliseconds. (Default 50,000 milliseconds)|
|payload|Y|Payload written in JavaScript to send to Slack.|

#### Payload specification

- Written in JavaScript
- Available Buildin variables

|Variable Name|Type|Description|
|:----|:----|:----|
|hostname|string|Hostname where the event occurred|
|event|ILoggingEvent|The representation of logging events. see [API](https://logback.qos.ch/apidocs/ch/qos/logback/classic/spi/ILoggingEvent.html)|
|level|string|Log level. eg. FATAL, ERROR, WARN, INFO, DEBUG, TRACE.|
|timestamp|string|Time when the event occurred. eg. 2018-02-21T19:00:25.827+09:00|
|message|string|The message of logging events.|
|color|string|Color according to log level.|
|emoji|string|Emoji according to log level.|

##### Payload sample - Simple

```javascript
{
  "channel": "#_channel",
  "username": "username",
  "icon_emoji": emoji,
  "link_names": 1,
  "attachments": [{
    "title": level + " (" + hostname + ")",
    "fallback": level + " (" + hostname + ")",
    "color": color,
    "fields": [{
      "title": "Hostname",
      "value": hostname,
      "short": true
    }, {
      "title": "Time",
      "value": timestamp,
      "short": true
    }, {
      "title": "Level",
      "value": level,
      "short": true
    }, {
      "title": "Trigger",
      "value": message,
      "short": false
    }]
  }]
}
```

##### Payload sample - Complex

```javascript
var env = 'dev', mention = '';
if(hostname.indexOf('dev') == -1) {
  env = 'production'; mention = '@channel';
}

return {
  "channel": "#_channel",
  "username": "username",
  "icon_emoji": emoji,
  "link_names": 1,
  "attachments": [{
    "title": level + " ( " + env + " ) " + mention,
    "fallback": level + " ( " + hostname + " )",
    "color": color,
    "fields": [{
      "title": "Hostname",
      "value": hostname,
      "short": true
    }, {
      "title": "Time",
      "value": timestamp,
      "short": true
    }, {
      "title": "Level",
      "value": level,
      "short": true
    }, {
      "title": "Trigger",
      "value": message,
      "short": false
    }]
  }]
};
```

### logback.xml (Legacy Style)

```xml
<configuration>

    <appender name="SLACK" class="jp.co.dwango.logback.SlackWebhookAppender">
        <webhookUrl>...</webhookUrl>
        <channel>...</channel>
        <username>...</username>
        <layout>
            <pattern>%date [%level] from %logger in %thread - %message%n%xException</pattern>
        </layout>
    </appender>

    <appender name="ASYNC_SLACK" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="SLACK" />
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
    </appender>

    <root level="INFO">
        <appender-ref ref="ASYNC_SLACK" />
    </root>

</configuration>
```

#### Appender options

|Key|Required|Detail|
|:----|:----|:----|
|webhookUrl|Y|URL of incoming webhook|
|channel|Y|channel to post logs to|
|username|Y|username which post logs as|
|iconEmoji|N|icon of the user; you probably want colons like `:smiley:`|
|iconUrl|N|icon of the user|
|linkNames|N|(`true` / `false`) If `false`, you will not be notified by posing message which includes `@channel`, `@{username}` and so forth (`true` by default)|
