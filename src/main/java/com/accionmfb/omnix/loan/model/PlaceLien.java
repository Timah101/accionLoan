package com.accionmfb.omnix.loan.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "place_lien")
public class PlaceLien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "lock_id")
    private String lockId;
    @Column(name = "loan_id")
    private String loanId;
    private String amount;
    private String status;
    @Column(name = "failure_reason")
    private String failureReason;
}
