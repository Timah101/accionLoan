package com.accionmfb.omnix.loan.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "paystack_details")
public class PaystackDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    private Long id;
    private String email;
    @Column(name = "authorization_code")
    private String authorizationCode;
    @Column(name = "loan_id")
    private String loanId;
    @Column(name = "card_type")
    private String cardType;
    @Column(name = "card_resuable")
    boolean cardResuable;
    @Column(name = "card_last_4_digit")
    private String cardLast4Digit;

}
