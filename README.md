# slack-webhook-appender
Logback appender which posts logs to slack via incoming webhook.


## Usage

```
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
        <appender-ref ref="SLACK" />
    </root>

</configuration>
```

### Appender options

|Key|Required|Detail|
|:----|:----|:----|
|webhookUrl|Y|URL of incoming webhook|
|channel|Y|channel to post logs to|
|username|Y|username which post logs as|
|iconEmoji|N|icon of the user; you probably want colons like `:smiley:`|
|iconUrl|N|icon of the user|
|linkNames|N|(`true` / `false`) If `false`, you will not be notified by posing message which includes `@channel`, `@{username}` and so forth (`true` by default)|
