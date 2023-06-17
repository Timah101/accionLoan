package com.accionmfb.omnix.loan.model;


import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "schedule")
@Data
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "repayment_amount")
    private String repaymentAmount;
    @Column(name = "repayment_date")
    private LocalDate repaymentDate;
    @Column(name = "past_due_date")
    private String pastDueDate;
    @Column(name = "past_due_amount")
    private String pastDueAmount = "0";
    @Column(name = "early_repayment_amount")
    private String earlyRepaymentAmount = "0";
    @Column(name = "loan_id")
    private String loanId;
    @Column(name = "loan_options_id")
    private String loanOptionsId;
    @Column(name = "disbursed_date")
    private LocalDate disbursedDate;
    private String months;
    @Column(name = "loan_balance")
    private String loanBalance;
    @Column(name = "disbursement_account")
    private String disbursementAccount;
    private String status = "Active";
    @Column(name = "paystack_ref")
    private String paystackRef;
    @Column(name = "outstanding_balance")
    private String outstandingBalance;
}
