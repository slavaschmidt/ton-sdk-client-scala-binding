package ton.sdk.client.jni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class is a proxy binding for the sdk client.
 * The JNI calls are done in a specific manner using Java types so it is not possible to call SDK client directly.
 * Instead, there is a thin C implementation that performs call forwarding and type conversion.
 * All the heavy lifting for context and callback management is done by the scala counterpart.
 *
 * This class represents the TON SDK client definition one-to-one with the exception of the
 * signed long JDK type being used to represent uint in SDK client definition.
 */
public class Binding {
    private static final Logger logger = LoggerFactory.getLogger(Binding.class);

    public static native String tcCreateContext(String config);

    public static native void tcDestroyContext(long context);

    public static native String tcRequestSync(long context, String functionName, String functionParamsJson);

    private static native void tcRequest(long context, String functionName, String functionParamsJson, long requestId);

    public static void request(long context, String functionName, String functionParamsJson, Handler responseHandler) {
        long id = counter.incrementAndGet();
        mapping.put(id, responseHandler);
        try {
            tcRequest(context, functionName, functionParamsJson, id);
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    private static void handle(long requestId, String paramsJson, long responseType, boolean finished) {
        Handler handler = mapping.get(requestId);
        try {
            handler.handle(requestId, paramsJson, responseType, finished);
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            if (finished) {
                mapping.remove(requestId);
            }
        }
    }

    // request to callback mapping and request id generator
    private static final ConcurrentHashMap<Long, Handler> mapping = new ConcurrentHashMap<>();
    private static final AtomicLong counter = new AtomicLong(0);

}
