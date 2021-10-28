package com.t1908e.memeportalapi.config;

import com.twilio.Twilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioInitialize {
    private final TwilioConfiguration twilioConfiguration;
    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioInitialize.class);
    @Autowired
    public TwilioInitialize(TwilioConfiguration twilioConfiguration) {
        this.twilioConfiguration = twilioConfiguration;
        Twilio.init(twilioConfiguration.getAccountSid(), twilioConfiguration.getAuthToken());
        LOGGER.info("Twilio initialized... with sid {}", twilioConfiguration.getAccountSid());
    }
}
