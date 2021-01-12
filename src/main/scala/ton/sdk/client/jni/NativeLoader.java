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
    private static final String JAVA_IO_FREETONTMPDIR = "java.io.freetontmpdir";


    public static void apply() throws Exception {
        File folder = libsDir();
        createTempLib(folder, jniLibName);
        createTempLib(folder, tonClientLibName);
        addPath(folder);
        System.load(libFile(folder, jniLibName).getAbsolutePath());
    }

    private static void addPath(File path) {
        try {
            String javaPath = System.getProperty(javaProp);
            if (javaPath != null) {
                System.setProperty(javaProp, javaPath + File.pathSeparator + path);
            } else {
                System.setProperty(javaProp, path.getAbsolutePath());
            }
        } catch (Exception ex) {
            log.warn("Failed to set environment, the ton client library might fail to load");
        }
    }

    private static File libsDir() throws IOException {
        String outer = System.getProperty(JAVA_IO_FREETONTMPDIR);
        if (outer == null) {
            return Files.createTempDirectory(tonClientLibName).toFile();
        } else {
            File dir = new File(outer);
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Couldn't create libs directory " + dir.getName());
            dir.deleteOnExit();
            return dir;
        }
    }

    private static File libFile(File path, String name) {
        String fullPath = path.getAbsolutePath() + File.separatorChar + System.mapLibraryName(name);
        return new File(fullPath);
    }

    private static void createTempLib(File dir, String name) throws IOException {
        name = System.mapLibraryName(name);
        File lib = new File(dir, name);
        try (InputStream is = NativeLoader.class.getClassLoader().getResourceAsStream(name)) {
            if (is != null) {
                Files.copy(is, lib.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new FileNotFoundException("Could not find library [" + name + "] in the JAR");
            }
        } catch (IOException e) {
            throw e;
        }
    }

}
