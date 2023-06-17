/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "loan")
public class Loan implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "loan_amount_requested")
    private BigDecimal loanAmountRequested;
    @Column(name = "loan_amount_approved")
    private BigDecimal loanAmountApproved;
    @Column(name = "monthly_repayment")
    private BigDecimal monthlyRepayment;
    @Column(name = "total_repayment")
    private BigDecimal totalRepayment;
    @Column(name = "interest_rate")
    private BigDecimal interestRate;
    @Column(name = "loan_tenor")
    private String loanTenor;
    @Column(name = "loan_purpose")
    private String loanPurpose;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "disbursement_account")
    private String disbursementAccount;
    @Column(name = "disbursed_at")
    private LocalDate disbursedAt;
    @Column(name = "loan_id")
    private String loanId;
    @Column(name = "loan_disbursement_id")
    private String loanDisbursementId;
    @Column(name = "liquidated_at")
    private LocalDate liquidatedAt;
    @Column(name = "matured_at")
    private LocalDate maturedAt;
    @Column(name = "first_repayment_date")
    private LocalDate firstRepaymentDate;
    @ManyToOne
    private LoanSetup loanSetup;
    @Column(name = "request_id")
    private String requestId;
    @ManyToOne
    private Customer customer;
    @ManyToOne
    private AppUser appUser;
    @ManyToOne
    private Branch branch;
    @Column(name = "status")
    private String status;
    @Column(name = "customer_business")
    private String customerBusiness;
    @Column(name = "selection_score")
    private String selectionScore;
    @Column(name = "selection_score_rating")
    private String selectionScoreRating;
    @Column(name = "msme_score")
    private String msmeScore;
    @Column(name = "limit_range")
    private String limitRange;
    @Column(name = "time_period")
    private char timePeriod;
    @Column(name = "product_code")
    private String productCode;
    @Column(name = "admin_fee")
    private String adminFee;
    @Column(name = "insurance_fee")
    private String insuranceFee;
    
    @Column(name = "marital_status")
    private String maritalStatus;
    @Column(name = "name_of_spouse")
    private String nameOfSpouse;
    @Column(name = "no_of_dependent")
    private String noOfDependent;
    @Column(name = "business_name")
    private String businessName;
     @Column(name = "business_type")
    private String businessType;
    @Column(name = "business_address")
    private String businessAddress;
    @Column(name = "type_of_residence")
    private String typeOfResidence;
    @Column(name = "rent_per_year")
    private String rentPerYear;
    @Column(name = "year_of_residence")
    private String yearOfResidence;
    @Column(name = "paystack_Ref")
    private String paystackRef;
    @Column(name = "failure_reason")
    private String failureReason;
}
