package com.accionmfb.omnix.loan.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static com.accionmfb.omnix.loan.constant.ApiPaths.*;

@FeignClient(name = "omnix-fundstransfer", url = "${zuul.routes.fundstransferService.url}")
public interface FundsTransferService {

    @PostMapping(value = LOCAL_TRANSFER_WITH_PL_INTERNAL, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransfer(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_WITH_CHARGE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransferWithCharges(@RequestHeader("Authorization") String bearerToken, String requestPayload);

    @PostMapping(value = LOCAL_TRANSFER_INTERNAL_DEBIT_WITH_CHARGE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    String localTransferWithInternalDebitCharges(@RequestHeader("Authorization") String bearerToken, String requestPayload);

}
