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
@Table(name = "credit_bureau_classification_type")
public class CreditBureauClassificationType implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "institution_type")
    private String institutionType;
    @Column(name = "product_type")
    private String productType;
    @Column(name = "currency")
    private String currency;
    @Column(name = "number_of_account")
    private String numberOfAccount;
    @Column(name = "number_of_account_6_month")
    private String numberOfAccount6Month;
    @Column(name = "sanctioned_amount")
    private String sanctionedAmount;
    @Column(name = "approved_sanctioned_amount")
    private String approvedSanctionedAmount;
    @Column(name = "outstanding_balance")
    private String outstandingBalance;
    @Column(name = "total_outstanding_balance")
    private String totalOutstandingBalance;
    @Column(name = "amount_overdue")
    private String amountOverdue;
    @Column(name = "legal_flag")
    private String legalFlag;
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
