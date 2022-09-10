package ton.sdk.client.jni;

import io.circe.Json;

/**
 * The interface of the handler to be called from the native code.
 * Mirrors corresponding SDK client definition with respect to long <-> uint mapping
 */
public interface Callback {
    Json call(Json json);
}
