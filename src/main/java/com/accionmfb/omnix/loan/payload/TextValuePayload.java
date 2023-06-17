package com.accionmfb.omnix.loan.payload;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author dofoleta
 */
@Setter
@Getter
public class TextValuePayload {

    private String value;
    private String text;

    public TextValuePayload(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
