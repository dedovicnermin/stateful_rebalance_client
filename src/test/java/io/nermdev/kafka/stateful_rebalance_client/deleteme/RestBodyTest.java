package io.nermdev.kafka.stateful_rebalance_client.deleteme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RestBodyTest {
    final ObjectMapper mapper = new ObjectMapper();

    @Test
    void test() throws JsonProcessingException {
        final EdaMessage edaMessage = new EdaMessage();
        edaMessage.setMessage("Hello, world");
        final Record record = new Record();
        record.setKey(null);
        record.setValue(edaMessage);

        final EdaRestRequest edaRestRequest = new EdaRestRequest().withValue(edaMessage);
        final String json = mapper.writeValueAsString(edaRestRequest);
        System.out.println(json);
        Assertions.assertThat(json).isNotNull();
    }

    @Test
    void testWithKey() throws JsonProcessingException {
        final EdaMessage edaMessage = new EdaMessage();
        edaMessage.setMessage("Hello, world");
        final Record record = new Record();
        record.setKey("hotelCode_001");
        record.setValue(edaMessage);

        final EdaRestRequest edaRestRequest = new EdaRestRequest().withKey(record.getKey()).withValue(edaMessage);
        final String json = mapper.writeValueAsString(edaRestRequest);
        System.out.println(mapper.writeValueAsString(edaRestRequest.build()));
        System.out.println(json);
        Assertions.assertThat(json).isNotNull();
    }
}


