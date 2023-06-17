/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "rubyx_loan_renewal")
public class RubyxLoanRenewal implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "customer_number")
    private String customerNumber;
    @Column(name = "disbursement_account")
    private String disbursementAccount;
    @Column(name = "brighta_commitment_account")
    private String brightaCommitmentAccount;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "bvn")
    private String bvn;
    @Column(name = "account_officer_code")
    private String accountOfficerCode;
    @Column(name = "branch")
    private String branch;
    @Column(name = "renewal_score")
    private String renewalScore;
    @Column(name = "renewal_rating")
    private String renewalRating;
    @Column(name = "renewal_amount")
    private String renewalAmount;
    @Column(name = "loan_amount")
    private String loanAmount;
    @Column(name = "interest_rate")
    private String interestRate;
    @Column(name = "product_code")
    private String productCode;
    @Column(name = "tenor")
    private String tenor;
    @Column(name = "status")
    private String status;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "customer_sms_sent")
    private boolean customerSmsSent = false;
    @Column(name = "contact_center_email_sent")
    private boolean contactCenterEmailSent = false;
    @Column(name = "customer_sign_offer_letter")
    private boolean customerSignOfferLetter = false;
    @Column(name = "guarantor_sign_offer_letter")
    private boolean guarantorSignOfferLetter = false;
    @Column(name = "guarantor_id_verified")
    private boolean guarantorIdVerified = false;
    @Column(name = "total_performing_loan")
    private BigDecimal totalPerformingBalance = BigDecimal.ZERO;
    @Column(name = "total_loan_balance")
    private BigDecimal totalLoanBalance = BigDecimal.ZERO;
    @Column(name = "no_of_performing_loan")
    private int noOfPerformingLoans = 0;
    @Column(name = "no_of_loan")
    private int noOfTotalLoans = 0;
    @Column(name = "credit_bureau_no_hit")
    private boolean creditBureauNoHit = false;
    @ManyToOne
    private Customer customer;
    @Column(name = "request_id")
    private String requestId;
    @Column(name = "customer_apply")
    private boolean customerApply = false;
    @Column(name = "credit_bureau_search_done")
    private boolean creditBureauSearchDone = false;
    @Column(name = "current_loan_cycle")
    private int currentLoanCycle = 0;
}
