
package com.accionmfb.omnix.loan.security;

import com.accionmfb.omnix.loan.payload.GenericPayload;
import com.accionmfb.omnix.loan.payload.ValidationPayload;



/**
 *
 * @author dofoleta
 */
public interface AesService {
    
    public String encryptString(String textToEncrypt, String encryptionKey);
    public String decryptString(String textToDecrypt, String encryptionKey);
    
     public String encryptFlutterString(String strToEncrypt, String secret) ;
     public String decryptFlutterString(final String textToDecrypt, final String encryptionKey);
     
     public ValidationPayload validateRequest(GenericPayload genericRequestPayload);
}
