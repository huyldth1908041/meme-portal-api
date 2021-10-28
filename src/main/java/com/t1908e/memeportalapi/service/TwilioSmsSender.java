package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.config.TwilioConfiguration;
import com.t1908e.memeportalapi.sms.SmsRequest;
import com.t1908e.memeportalapi.sms.SmsSender;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TwilioSmsSender implements SmsSender {
    private final TwilioConfiguration twilioConfiguration;

    @Override
    public void sendSms(SmsRequest smsRequest){
        try {
            com.twilio.rest.lookups.v1.PhoneNumber phoneNumber = com.twilio.rest.lookups.v1.PhoneNumber.fetcher(
                    new com.twilio.type.PhoneNumber(smsRequest.getPhoneNumber()))
                    .setCountryCode("VN").fetch();
            PhoneNumber to = new PhoneNumber(phoneNumber.getPhoneNumber().toString());
            PhoneNumber from = new PhoneNumber(twilioConfiguration.getTrialNumber());
            Message message = Message.creator(to, from, smsRequest.getMessage()).create();
        } catch (Exception exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }

    }

}
