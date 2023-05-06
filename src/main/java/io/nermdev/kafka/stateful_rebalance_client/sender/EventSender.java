package io.nermdev.kafka.stateful_rebalance_client.sender;

import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.Closeable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface EventSender<K,V> extends Closeable {
    Future<RecordMetadata> send(V payload);
    Future<RecordMetadata> send(K key, V payload);

    final class SendException extends Exception {
        private static final long serialVersionUID = 1L;

        SendException(Throwable cause) { super(cause); }
    }

    default RecordMetadata blockingSend(V payload) throws SendException, InterruptedException {
        try {
            return send(payload).get();
        } catch (ExecutionException e) {
            throw new SendException(e.getCause());
        }
    }

    @Override
    public void close();
}
