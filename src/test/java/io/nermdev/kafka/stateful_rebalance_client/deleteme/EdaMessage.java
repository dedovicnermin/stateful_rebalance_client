package io.nermdev.kafka.stateful_rebalance_client.deleteme;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.builder.ToStringBuilder;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdaMessage {
//    private Metadata metadata;
//    private List<Context> context;
    private String message;

    public EdaMessage() {
    }

//    public String findValueInContext(String key) {
//        return (String)((Stream)Optional.ofNullable(this.context).map(Collection::stream).orElseGet(Stream::empty)).filter((item) -> {
//            return key.equalsIgnoreCase(item.getKey());
//        }).findFirst().map(Context::getValue).orElse((Object)null);
//    }

//    public Metadata getMetadata() {
//        return this.metadata;
//    }

//    public void setMetadata(Metadata metadata) {
//        this.metadata = metadata;
//    }
//
//    public List<Context> getContext() {
//        return this.context;
//    }
//
//    public void setContext(List<Context> context) {
//        this.context = context;
//    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
