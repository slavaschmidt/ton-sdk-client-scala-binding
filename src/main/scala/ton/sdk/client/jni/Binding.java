package ton.sdk.client.jni;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Binding {
    public static native String tcCreateContext(String config);

    public static native void tcDestroyContext(long context);

    public static native String tcRequestSync(long context, String functionName, String functionParamsJson);

    private static native void tcRequest(long context, String functionName, String functionParamsJson, long requestId);

    public static void request(long context, String functionName, String functionParamsJson, Handler responseHandler) {
        long id = counter.incrementAndGet();
        mapping.put(id, responseHandler);
        tcRequest(context, functionName, functionParamsJson, id);
    }

    private static void handle(long requestId, String paramsJson, long responseType, boolean finished) {
        Handler handler = mapping.get(requestId);
        try {
            handler.handle(requestId, paramsJson, responseType, finished);
        } finally {
            if (finished) mapping.remove(requestId);
        }
    }

    private static final ConcurrentHashMap<Long, Handler> mapping = new ConcurrentHashMap<>();
    private static final AtomicLong counter = new AtomicLong(0);

    static {
        System.loadLibrary("TonSdkClientJniBinding");
    }
}
