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
@Table(name = "credit_bureau_credit_risk")
public class CreditBureauCreditRisk implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "indicator_type")
    private String indicatorType;
    @Column(name = "indicator_description")
    private String indicatorDescription;
    @Column(name = "indicator_value")
    private String indicatorValue;
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
