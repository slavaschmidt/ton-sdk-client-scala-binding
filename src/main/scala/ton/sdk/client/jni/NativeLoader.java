package ton.sdk.client.jni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * An attempt to make native library loading more user-friendly.
 * Probably will not work in any other Java version because of the way the environment variables are modified.
 */
public class NativeLoader {

    private static final Logger log = LoggerFactory.getLogger(NativeLoader.class);
    private static final String jniLibName = "TonSdkClientJniBinding";
    private static final String tonClientLibName = "ton_client";
    private static final String javaProp = "java.library.path";
    private static final String linuxEnv = "LD_LIBRARY_PATH";
    private static final String winEnv = "PATH";
    private static final String libDir = "lib" + File.separator;

    public static void apply() throws Exception {
        String path = new File(".").getAbsolutePath();
        if (!libsAreThere(path)) {
            log.debug("Could not find libs, creating temporary folder");
            File folder = createTempFolder();
            createTempLib(folder, jniLibName);
            createTempLib(folder, tonClientLibName);
            path = folder.getAbsolutePath();
            addPath(path);
        } else {
            log.debug("Found native libraries in path " + path);
        }
        System.load(libFile(path, jniLibName).getAbsolutePath());
    }

    private static void addPath(String path) throws Exception {
        try {
            String javaPath = System.getProperty(javaProp);
            if (javaPath != null) {
                System.setProperty(javaProp, javaPath + File.pathSeparator + path);
            } else {
                System.setProperty(javaProp, path);
            }
            Map<String, String> env = getModifiableEnvironment();
            extendSinglePath(path, linuxEnv, env);
            extendSinglePath(path, winEnv, env);
        } catch (Exception ex) {
            log.warn("Failed to set environment, the ton client library might fail to load");
        }
    }

    private static void extendSinglePath(String path, String name, Map<String, String> env) {
        env.merge(name, path, (a, b) -> a + File.pathSeparator + b);
    }

    private static boolean libsAreThere(String path) {
        File dir = new File(path);
        boolean dirExists = dir.exists() && dir.isDirectory() && dir.canRead();
        return dirExists && libFile(path, jniLibName).canRead() && libFile(path, tonClientLibName).canRead();
    }

    private static File libFile(String path, String name) {
        String fullPath = path + File.separatorChar + System.mapLibraryName(name);
        return new File(fullPath);
    }

    private static File createTempFolder() throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        File dir = new File(tempDir);
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Couldn't create temp directory " + dir.getName());
        dir.deleteOnExit();
        return dir;
    }

    private static void createTempLib(File dir, String name) throws IOException {
        name = System.mapLibraryName(name);
        File lib = new File(dir, name);
        try (InputStream is = NativeLoader.class.getClassLoader().getResourceAsStream(name)) {
            if (is != null) {
                Files.copy(is, lib.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                dir.delete();
                throw new FileNotFoundException("Could not find lib " + name + " in the JAR");
            }
        } catch (IOException e) {
            dir.delete();
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getModifiableEnvironment() throws Exception {
        Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv", String.class);
        getenv.setAccessible(true);
        Field props = pe.getDeclaredField("theEnvironment");
        props.setAccessible(true);
        return (Map<String, String>) props.get(null);
    }
}
