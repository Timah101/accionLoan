package com.accionmfb.omnix.loan.repository;

import com.accionmfb.omnix.loan.model.SMS;

public interface SmsRepository {

    SMS createSMS(SMS sms);

    SMS updateSMS(SMS sms);
}
