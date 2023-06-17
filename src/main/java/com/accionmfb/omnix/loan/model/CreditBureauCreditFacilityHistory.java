/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.model;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.Basic;
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
@Table(name = "credit_bureau_credit_facility_histor")
public class CreditBureauCreditFacilityHistory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "institution_name")
    private String institutionName;
    @Column(name = "asset_classification")
    private String assetClassification;
    @Column(name = "credit_facility_type")
    private String creditFacilityType;
    @Column(name = "credit_facility_status")
    private String creditFacilityStatus;
    @Column(name = "currency")
    private String currency;
    @Column(name = "approved_amount")
    private String approvedAmount;
    @Column(name = "current_balance")
    private String currentBalance;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "amount_overdue")
    private String amountOverdue;
    @Column(name = "amount_written_off")
    private String amountWrittenOff;
    @Column(name = "credit_facility_opened_date")
    private String creditFacilityOpenedDate;
    @Column(name = "credit_facility_maturity_date")
    private String creditFacilityMaturityDate;
    @Column(name = "repayment")
    private String repayment;
    @Column(name = "last_repayment_date")
    private String lastRepaymentDate;
    @Column(name = "ndia")
    private String ndia;
    @Column(name = "date_restructured")
    private String dateRestructured;
    @Column(name = "reported_date")
    private String reportedDate;
    @Column(name = "security_coverage")
    private String securityCoverage;
    @Column(name = "month1_date")
    private String month1Date;
    @Column(name = "month2_date")
    private String month2Date;
    @Column(name = "month3_date")
    private String month3Date;
    @Column(name = "month4_date")
    private String month4Date;
    @Column(name = "month5_date")
    private String month5Date;
    @Column(name = "month6_date")
    private String month6Date;
    @Column(name = "month7_date")
    private String month7Date;
    @Column(name = "month8_date")
    private String month8Date;
    @Column(name = "month9_date")
    private String month9Date;
    @Column(name = "month10_date")
    private String month10Date;
    @Column(name = "month11_date")
    private String month11Date;
    @Column(name = "month12_date")
    private String month12Date;
    @Column(name = "month13_date")
    private String month13Date;
    @Column(name = "month14_date")
    private String month14Date;
    @Column(name = "month15_date")
    private String month15Date;
    @Column(name = "month16_date")
    private String month16Date;
    @Column(name = "month17_date")
    private String month17Date;
    @Column(name = "month18_date")
    private String month18Date;
    @Column(name = "month19_date")
    private String month19Date;
    @Column(name = "month20_date")
    private String month20Date;
    @Column(name = "month21_date")
    private String month21Date;
    @Column(name = "month22_date")
    private String month22Date;
    @Column(name = "month23_date")
    private String month23Date;
    @Column(name = "month24_date")
    private String month24Date;
    @Column(name = "month1_value")
    private String month1Value;
    @Column(name = "month2_value")
    private String month2Value;
    @Column(name = "month3_value")
    private String month3Value;
    @Column(name = "month4_value")
    private String month4Value;
    @Column(name = "month5_value")
    private String month5Value;
    @Column(name = "month6_value")
    private String month6Value;
    @Column(name = "month7_value")
    private String month7Value;
    @Column(name = "month8_value")
    private String month8Value;
    @Column(name = "month9_value")
    private String month9Value;
    @Column(name = "month10_value")
    private String month10Value;
    @Column(name = "month11_value")
    private String month11Value;
    @Column(name = "month12_value")
    private String month12Value;
    @Column(name = "month13_value")
    private String month13Value;
    @Column(name = "month14_value")
    private String month14Value;
    @Column(name = "month15_value")
    private String month15Value;
    @Column(name = "month16_value")
    private String month16Value;
    @Column(name = "month17_value")
    private String month17Value;
    @Column(name = "month18_value")
    private String month18Value;
    @Column(name = "month19_value")
    private String month19Value;
    @Column(name = "month20_value")
    private String month20Value;
    @Column(name = "month21_value")
    private String month21Value;
    @Column(name = "month22_value")
    private String month22Value;
    @Column(name = "month23_value")
    private String month23Value;
    @Column(name = "month24_value")
    private String month24Value;
    @Column(name = "created_at")
    private LocalDate createdAt;
    @ManyToOne()
    private Customer customer;
    @ManyToOne
    private AppUser appUser;
    @Column(name = "request_id")
    private String requestId;
    @Column(name = "status")
    private String status;
    @Column(name = "validation_source")
    private String validationSource;
}
