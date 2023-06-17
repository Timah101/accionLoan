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
@Table(name = "loan_setup")
public class LoanSetup implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "loan_name")
    private String loanName;
    @Column(name = "loan_description")
    private String loanDescription;
    @Column(name = "loan_category")
    private String loanCategory;
    @Column(name = "min_tenor")
    private int minTenor;
    @Column(name = "max_tenor")
    private int maxTenor;
    @Column(name = "min_amount")
    private BigDecimal minAmount;
    @Column(name = "max_amount")
    private BigDecimal maxAmount;
    @Column(name = "interest_rate")
    private double interestRate;
    @Column(name = "admin_fee")
    private double adminFee;
    @Column(name = "processing_fee")
    private double processingFee;
    @Column(name = "insurance_fee")
    private double insuranceFee;
    @Column(name = "status")
    private String status;
    @ManyToOne
    private AppUser appUser;
}
