package ton.sdk.client.jni;

import java.util.Locale;

public class OperatingSystem {
    private static String OS = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac") || OS.contains("darwin");
    }

    public static boolean isUnix() {
        return OS.contains("nux");
    }
}
