/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import com.accionmfb.omnix.loan.payload.CRCDataUploadCreditPayload;
import com.accionmfb.omnix.loan.payload.CRCDataUploadGuarantorPayload;
import com.accionmfb.omnix.loan.payload.CRCDataUploadIndividualPayload;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 *
 * @author bokon
 */
@Service
@Scope("prototype")
public class SendToCreditBureau implements Runnable {

    @Autowired
    GenericService genericService;
    Gson gson;
    @Value("${omnix.creditBureau.data.dump.path}")
    private String crcDataDumpPath;
    @Value("${omnix.creditBureau.data.upload.path}")
    private String crcDataUploadPath;
    @Value("${omnix.creditBureau.data.upload.crc.individual}")
    private String individualDataUploadUrl;
    @Value("${omnix.creditBureau.data.upload.crc.credit}")
    private String creditDataUploadUrl;
    @Value("${omnix.creditBureau.data.upload.crc.guarantor}")
    private String guarantorDataUploadUrl;
    @Value("${omnix.creditBureau.data.upload.crc.apikey}")
    private String dataUploadApiKey;
    private static final String CRC_USERNAME = "10852215accion";
    private static final String CRC_PASSWORD = "AcC10nmFb2i2o";
    private String threadName;
    private String transType = "";
    private String token;
    private List<CRCDataUploadIndividualPayload> individualRecord;
    private List<CRCDataUploadCreditPayload> creditRecord;
    private List<CRCDataUploadGuarantorPayload> guarantorRecord;

    public void individualRecord(List<CRCDataUploadIndividualPayload> loanRecord, String transType, String token) {
        this.individualRecord = loanRecord;
        this.threadName = "Individual Record Upload To Credit Bureau";
        this.transType = transType;
        this.token = token;
    }

    public void creditRecord(List<CRCDataUploadCreditPayload> loanRecord, String transType, String token) {
        this.creditRecord = loanRecord;
        this.threadName = "Credit Record Upload To Credit Bureau";
        this.transType = transType;
        this.token = token;
    }

    public void guarantorRecord(List<CRCDataUploadGuarantorPayload> loanRecord, String transType, String token) {
        this.guarantorRecord = loanRecord;
        this.threadName = "Guarantor Record Upload To Credit Bureau";
        this.transType = transType;
        this.token = token;
    }

    @Override
    public void run() {
        System.out.println("Running " + threadName);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.IDENTITY);
        gson = gsonBuilder.create();

        if (this.transType.equalsIgnoreCase("Individual")) {
            processIndividualRecord();
        } else if (this.transType.equalsIgnoreCase("Credit")) {
            processCreditRecord();
        } else {
            processGuarantorRecord();
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dayMonthFormat = "^([0-2][0-9]|(3)[0-1])(\\/)(((0)[0-9])|((1)[0-2]))(\\/)\\d{4}$";
        String monthDayFormat = "^(0[1-9]|1[0-2])\\/(0[1-9]|1\\d|2\\d|3[01])\\/(19|20)\\d{2}$";
        List<String> accountStatus = Arrays.asList("OPEN", "CLOSED", "FREEZED", "INACTIVE", "WRITTEN OFF");
        List<String> loanClassification = Arrays.asList("PERFORMING", "PASS AND WATCH", "SUB STANDARD", "DOUBTFUL", "LOST");
        List<String> interestType = Arrays.asList("FIXED", "FLOATING");
        List<String> repaymentFrequency = Arrays.asList("BALLOON", "BULLET", "DAILY", "WEEKLY", "FORNIGHTLY", "MONTHLY", "BI MONTHLY", "QUARTERLY", "SEMI ANNUAL", "ANNUAL", "DEMAND", "OTHERS");

    }

    public void processIndividualRecord() {
        try {
            String requestJson = gson.toJson(individualRecord);
            requestJson = requestJson.replace("Primarycity", "Primarycity/LGA");
            requestJson = requestJson.replace("SecondaryAddressCity", "SecondaryAddressCity/LGA");
            requestJson = requestJson.replace("emailAddress", "E-mailaddress");
            genericService.generateLog("Individual Record Upload To Credit Bureau", this.token, requestJson, "API Request", "INFO", "");
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> responseJson = Unirest.post(individualDataUploadUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("payload", requestJson)
                    .field("userid", "automations").asString();
            genericService.generateLog("Individual Record Upload To Credit Bureau", this.token, responseJson.getBody(), "API Response", "INFO", "");

        } catch (UnirestException ex) {
            genericService.generateLog("Individual Record Upload To Credit Bureau", this.token, ex.getMessage(), "API Request", "INFO", "");
        }
    }

    public void processCreditRecord() {
        try {
            String requestJson = gson.toJson(creditRecord);
            //Update key mappings for Credit Information
            requestJson = requestJson.replace("DateOfLoan", "DateOfLoan(Facility)Disbursement/LoanEffectiveDate");
            requestJson = requestJson.replace("LoanLimit", "CreditLimit(Facility)Amount/GlobalLimit");
            requestJson = requestJson.replace("LoanAmount", "Loan(Facility)Amount/AvailedLimit");
            requestJson = requestJson.replace("LoanTenor", "Loan(Facility)Tenor");
            requestJson = requestJson.replace("CreditLimit", "CreditLimit(Facility)Amount/GlobalLimit");
            requestJson = requestJson.replace("LoanType", "Loan(Facility)Type");
            genericService.generateLog("Credit Record Upload To Credit Bureau", token, requestJson, "API Request", "INFO", "");
            HttpResponse<String> responseJson = Unirest.post(creditDataUploadUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("payload", requestJson)
                    .field("userid", "automations").asString();
            genericService.generateLog("Credit Record Upload To Credit Bureau", this.token, responseJson.getBody(), "API Response", "INFO", "");
        } catch (Exception ex) {
            genericService.generateLog("Credit Record Upload To Credit Bureau", this.token, ex.getMessage(), "API Request", "INFO", "");
        }
    }

    public void processGuarantorRecord() {
        try {
            String requestJson = gson.toJson(guarantorRecord);
            //Update the key mappings for Guarantor Information
            requestJson = requestJson.replace("GuarantorsDateOfBirth", "GuarantorsDateOfBirth/Incorporation");
            requestJson = requestJson.replace("GuarantorsPrimaryAddressCity", "GuarantorsPrimaryAddressCity/LGA");
            genericService.generateLog("Guarantor Record Upload To Credit Bureau", "", requestJson, "API Request", "INFO", "");
            HttpResponse<String> responseJson = Unirest.post(guarantorDataUploadUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("payload", requestJson)
                    .field("userid", "automations").asString();
            genericService.generateLog("Guarantor Record Upload To Credit Bureau", this.token, responseJson.getBody(), "API Response", "INFO", "");
        } catch (Exception ex) {
            genericService.generateLog("Guarantor Record Upload To Credit Bureau", this.token, ex.getMessage(), "API Request", "INFO", "");
        }
    }

}
