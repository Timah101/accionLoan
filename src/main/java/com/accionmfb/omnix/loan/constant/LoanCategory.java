/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.accionmfb.omnix.loan.constant;

/**
 *
 * @author dakinkuolie
 */
public enum LoanCategory {
    
      BRIGHTA_LOAN("21068");
  
  private String categoryCode;
  
  public String getCategoryCode() {
    return categoryCode;
  }
  
  LoanCategory(String categoryCode) {
    this.categoryCode = categoryCode;
  }
}
