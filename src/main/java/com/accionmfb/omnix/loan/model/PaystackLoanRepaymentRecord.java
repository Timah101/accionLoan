package com.accionmfb.omnix.loan.model;

import lombok.Data;
import org.apache.tomcat.jni.Local;

import javax.persistence.*;
import java.time.LocalDate;

@Table(name = "paystack_loan_repayment_record")
@Data
@Entity
public class PaystackLoanRepaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "credit_account")
    private String creditAccount;
    @Column(name = "debit_account")
    private String debitAccount;
    @Column(name = "created_at")
    private LocalDate createdAt;
    @Column(name = "repayment_date")
    private String repaymentDate;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "t24_trans_ref")
    private String t24TransRef;
    @Column(name = "loan_id")
    private String loanId;
    private String status;
    private String amount;
}
