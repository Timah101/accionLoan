/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.constant;

/**
 *
 * @author bokon
 */
public class ApiPaths {

    /**
     * This class includes the name and API end points of other microservices
     * that we need to communicate. NOTE: WRITE EVERYTHING IN ALPHABETICAL ORDER
     */
    //A
    public static final String ACCOUNT_BALANCE = "/balance";
    public static final String ARTISAN_LOAN_BOOKING = "/artisan/booking";
    public static final String ARTISAN_LOAN_DISBURSEMENT = "/artisan/disburse";
    public static final String ARTISAN_PENDING_LOAN = "/artisan/pending";
    public static final String ARTISAN_LOAN_AUTHORIZATION = "/artisan/authorize";
    public static final String ARTISAN_LOAN_RENEWAL = "/artisan/renewal";
    //B
    public static final String BASE_API = "/omnix/api";
    public static final String BVN_VALIDATION = "/validation";
    //C
    public static final String CUSTOMER_QUERY = "/customer/query";

    public static final String CUSTOMER_DETAILS = "/details";
    public static final String CUSTOMER_CARD_DETAILS = "/card/details";
    //D
    public static final String DIGITAL_LOAN_BOOKING = "/digital/booking";
    
    public static final String DIGITAL_LOAN_DISBURSEMENT = "/digital/disburse";
    public static final String DIGITAL_LOAN_RENEWAL = "/digital/renewal";
    public static final String DIGITAL_LOAN_ACCEPTANCE = "/digital/accept";
    public static final String DIGITAL_LOAN_ACTIVE = "/digital/active";

    public static final String DIGITAL_LOAN_PENDING = "/digital/pending";

    public static final String DIGITAL_LOAN_EARLY_REPAYMENT = "/digital/repayment";
    public static final String DIGITAL_LOAN_DECLINE = "/digital/decline";
    public static final String DIGITAL_LOAN_HISTORY = "/digital/history";
    //E
    public static final String EMAIL_NOTIFICATION = "/email/notification";
    //G
    public static final String GUARANTOR_UPDATE= "/digital/guarantor"; // just added
    //H
    public static final String HEADER_STRING = "Authorization";
    //I
    public static final String IDENTITY_VALIDATION = "/validation";
    //K
    //L
    public static final String LOAN_APPLICANT_CONTACT= "/digital/contacts";
    public static final String LOAN_TYPE_LISTING = "/type/listing";
    public static final String LOAN_SETUP = "/type/setup";
    public static final String LOAN_DETAILS = "/details";
    public static final String LOAN_TERMINATION = "/terminate";
    public static final String LOAN_OFFER_LETTER = "/offer-letter";
    public static final String LOAN_STATUS = "/status";

    public static final String LOCAL_TRANSFER = "/local";
    public static final String LOCAL_TRANSFER_WITH_PL_INTERNAL = "/local/internal-account";
    public static final String LOCAL_TRANSFER_WITH_CHARGE = "/local/with-charges";
    public static final String LOCAL_TRANSFER_INTERNAL_DEBIT_WITH_CHARGE = "/local/debit-internal-account";
    //M
    public static final String STATISTICS_MEMORY = "/actuator/stats";
    public static final String SMS_NOTIFICATION = "/sms/send";
    //O
    //P
    //R
    public static final String RUBYX_LOAN_RENEWAL = "/rubyx/loan/renewal";
    public static final String RUBYX_LOAN_RENEWAL_UPDATE = "/rubyx/loan/renewal/update";
    public static final String RUBYX_LOAN_RENEWAL_APPLY = "/rubyx/loan/renewal/apply";
    public static final String RUBYX_LOAN_RENEWAL_FETCH = "/rubyx/loan/renewal/fetch";
    public static final String RUBYX_LOAN_RENEWAL_LIST = "/rubyx/loan/renewal/list";
    public static final String RUBYX_LOAN_RENEWAL_DISBURSEMENT = "/rubyx/loan/renewal/disbursement";
    //T
    public static final String TOKEN_PREFIX = "Bearer";

}
