/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "loan_options")
public class LoanOptions implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "loan_id")
    private String loanId;
    @Column(name = "loan_option_id")
    private String loanOptionId;
    @Column(name = "interest_rate")
    private String interestRate;
    @Column(name = "tenor")
    private String tenor;
    @Column(name = "total_repayment")
    private String totalRepayment;
    @Column(name = "monthly_repayment")
    private String monthlyRepayment;
    @Column(name = "approved_amount")
    private String approvedAmount;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
     @Column(name = "repayment")
    private String repayment;
   
}
