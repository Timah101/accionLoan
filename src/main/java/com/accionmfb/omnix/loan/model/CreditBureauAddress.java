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
@Table(name = "credit_bureau_address")
public class CreditBureauAddress implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "sl_id")
    private String slid;
    @Column(name = "address_type")
    private String addressType;
    @Column(name = "address", length = 1000000)
    private String address;
    @Column(name = "last_reported_date")
    private String lastReportedDate;
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
