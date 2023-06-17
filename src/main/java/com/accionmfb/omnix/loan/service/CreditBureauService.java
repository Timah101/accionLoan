/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import static com.accionmfb.omnix.loan.constant.ApiPaths.CUSTOMER_QUERY;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-creditbureau", url = "${zuul.routes.creditBureauService.url}")
public interface CreditBureauService {

    @PostMapping(value = CUSTOMER_QUERY, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String creditBureauValidation(@RequestHeader("Authorization") String bearerToken, String requestPayload);
}
