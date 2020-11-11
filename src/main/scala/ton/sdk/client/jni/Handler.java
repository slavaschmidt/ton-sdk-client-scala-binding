package ton.sdk.client.jni;

public interface Handler {
    void handle(long requestId, String paramsJson, long responseType, boolean finished);
}
