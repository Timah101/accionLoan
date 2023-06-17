/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.loan.service;

import static com.accionmfb.omnix.loan.constant.ApiPaths.ACCOUNT_BALANCE;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 *
 * @author bokon
 */
@FeignClient(name = "omnix-account", url = "${zuul.routes.accountService.url}")
public interface AccountService {

    @PostMapping(value = ACCOUNT_BALANCE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String accountBalance(@RequestHeader("Authorization") String bearerToken, String requestPayload);
}
