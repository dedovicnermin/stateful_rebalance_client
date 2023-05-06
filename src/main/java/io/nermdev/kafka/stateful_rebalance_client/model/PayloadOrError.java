package io.nermdev.kafka.stateful_rebalance_client.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayloadOrError<T>{
    private final T payload;
    private final Throwable error;
    private final String encodedValue;
}
