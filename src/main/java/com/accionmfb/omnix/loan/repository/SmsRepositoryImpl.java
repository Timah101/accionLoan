package com.accionmfb.omnix.loan.repository;

import com.accionmfb.omnix.loan.model.SMS;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
@Transactional
public class SmsRepositoryImpl implements SmsRepository{

    @PersistenceContext
    EntityManager em;

    @Override
    public SMS createSMS(SMS sms) {
        em.persist(sms);
        em.flush();
        return sms;
    }

    @Override
    public SMS updateSMS(SMS sms) {
        em.merge(sms);
        em.flush();
        return sms;
    }
}
