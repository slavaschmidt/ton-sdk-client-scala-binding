package ton.sdk.client.jni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

    private static final ConcurrentHashMap<Long, Handler> mapping = new ConcurrentHashMap<>();
    private static final AtomicLong counter = new AtomicLong(0);

    public static void loadNativeLibrary() {
//        String path = new File("lib/").getAbsolutePath();
//        System.out.println("loading from " + path);
//        System.setProperty("java.library.path", path);
//        System.loadLibrary("TonSdkClientJniBinding");
        String path = new File("lib/libTonSdkClientJniBinding.dylib").getAbsolutePath();
        System.load(path);
    }
}
