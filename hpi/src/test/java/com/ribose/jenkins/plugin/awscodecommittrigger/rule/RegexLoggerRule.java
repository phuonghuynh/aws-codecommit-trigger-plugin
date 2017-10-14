package com.ribose.jenkins.plugin.awscodecommittrigger.rule;

import hudson.util.RingBufferLogHandler;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import java.util.regex.Pattern;

public class RegexLoggerRule extends ExternalResource {

    protected final Pattern regex;
    protected final Logger logger;

    protected RingBufferLogHandler ringHandler;
    protected List<String> messages = new ArrayList<>();

    public RegexLoggerRule(Pattern regex, Logger logger, Level level) {
        this.regex = regex;
        this.logger = logger;
        this.logger.setLevel(level);
    }

    public RegexLoggerRule capture(int maximum) {
        messages.clear();

        ringHandler = new RingBufferLogHandler(maximum) {
            final Formatter f = new SimpleFormatter(); // placeholder instance for what should have been a static method perhaps

            @Override
            public synchronized void publish(LogRecord record) {
                String message = f.formatMessage(record);
                if (RegexLoggerRule.this.regex.matcher(message).find()) {
                    Throwable x = record.getThrown();
                    messages.add(message == null && x != null ? x.toString() : message);
                    super.publish(record);
                }
            }
        };
        ringHandler.setLevel(Level.ALL);
        logger.addHandler(ringHandler);
        return this;
    }

    public List<String> getMessages() {
        return messages;
    }

    public Pattern getRegex() {
        return regex;
    }
}
