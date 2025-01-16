package com.damai.pay;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayResult {

    private final boolean success;

    private final String body;
}
