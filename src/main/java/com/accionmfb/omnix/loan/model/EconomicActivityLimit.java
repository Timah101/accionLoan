/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.model;

import java.io.Serializable;
import javax.persistence.Basic;
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
@Table(name = "economic_activity_limit")
public class EconomicActivityLimit implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Long id;
    @Column(name = "activity")
    private String activity;
    @Column(name = "sales_rank")
    private String salesRank;
    @Column(name = "gross_margin_1")
    private String grossMargin1;
    @Column(name = "gross_margin_2")
    private String grossMargin2;
    @Column(name = "gross_margin_3")
    private String grossMargin3;
    @Column(name = "gross_margin_4")
    private String grossMargin4;
    @Column(name = "net_margin_1")
    private String netMargin1;
    @Column(name = "net_margin_2")
    private String netMargin2;
    @Column(name = "net_margin_3")
    private String netMargin3;
    @Column(name = "net_margin_4")
    private String netMargin4;
    @Column(name = "household_expense_1")
    private String householdExpense1;
    @Column(name = "household_expense_2")
    private String householdExpense2;
    @Column(name = "household_expense_3")
    private String householdExpense3;
    @Column(name = "household_expense_4")
    private String householdExpense4;
}
