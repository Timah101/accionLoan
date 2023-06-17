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
@Table(name = "credit_bureau_performance_summary")
public class CreditBureauPerformanceSummary implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "bureau_currency")
    private String bureauCurrency;
    @Column(name = "data_product_id")
    private String dataProductId;
    @Column(name = "provider_source")
    private String providerSource;
    @Column(name = "facility_count")
    private String facilityCount;
    @Column(name = "performing_facility")
    private String performingFacility;
    @Column(name = "non_performing_facility")
    private String nonPerformingFacility;
    @Column(name = "approved_amount")
    private String approvedAmount;
    @Column(name = "account_balance")
    private String accountBalance;
    @Column(name = "overdue_amount")
    private String overdueAmount;
    @Column(name = "dishonored_cheque_count")
    private String dishonoredChequeCount;
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
