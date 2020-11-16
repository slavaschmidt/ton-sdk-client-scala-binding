package ton.sdk.client.jni;

/**
 * The interface of the handler to be called from the native code.
 * Mirrors corresponding SDK client definition with respect to long <-> uint mapping
 */
public interface Handler {
    void handle(long requestId, String paramsJson, long responseType, boolean finished);
}
