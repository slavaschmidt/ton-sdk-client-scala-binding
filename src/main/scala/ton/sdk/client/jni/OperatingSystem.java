package ton.sdk.client.jni;

import java.util.Locale;

/**
 * Simple utility class to detect an operating system the client is running on.
 */
public class OperatingSystem {
    private static final String OS = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    private static final String ARCH = System.getProperty("os.arch", "unknown").toLowerCase(Locale.ROOT);

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac") || OS.contains("darwin");
    }

    public static boolean isUnix() {
        return OS.contains("nux");
    }

    public static boolean is64() { return ARCH.contains("64"); }
}
