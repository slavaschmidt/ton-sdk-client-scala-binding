package ton.sdk.client.jni;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * An attempt to make native library loading more user-friendly.
 * Probably will not work in any other Java version because of the way the environment variables are modified.
 */
public class NativeLoader {

    private static final Logger log = LoggerFactory.getLogger(NativeLoader.class);
    private static final String jniLibName = "TonSdkClientJniBinding";
    private static final String tonClientLibName = "ton_client";
    private static final String javaProp = "java.library.path";

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

    private static void addPath(String path) {
        try {
            String javaPath = System.getProperty(javaProp);
            if (javaPath != null) {
                System.setProperty(javaProp, javaPath + File.pathSeparator + path);
            } else {
                System.setProperty(javaProp, path);
            }
        } catch (Exception ex) {
            log.warn("Failed to set environment, the ton client library might fail to load");
        }
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
        String tempDir = System.getProperty("java.io.freetontmpdir");
        if (tempDir == null) {
            tempDir = new File("lib").getAbsolutePath();
        }
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

}
