package ton.sdk.client.jni;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class NativeLoader {

    private static final String jniLibName = "TonSdkClientJniBinding";
    private static final String tonClientLibName = "ton_client";

    private static void load() throws IOException {
        String linuxPath = System.getenv("LD_LIBRARY_PATH");
        String winPath = System.getenv("PATH");
        String javaPath = System.getProperty("java.library.path");
        String libPath = new File("lib").getAbsolutePath();
        if (javaPath == null) {
            javaPath = "";
        }
        if (winPath == null) {
            winPath = "";
        }
        if (linuxPath == null) {
            linuxPath = "";
        }
        String fullPath = javaPath + File.pathSeparator + linuxPath + File.pathSeparator + winPath + File.pathSeparator + libPath;
        String[] folders = fullPath.split(File.pathSeparator);
        String path = null;
        for (String s : folders) {
            if (!s.equals("") && libsAreThere(s)) {
                path = s;
                break;
            }
        }
        if (path == null) {
            File folder = createTempFolder();
            createTempLib(folder, jniLibName);
            createTempLib(folder, tonClientLibName);
            path = folder.getAbsolutePath();
            addPath(path);
        }
        System.load(libFile(path, jniLibName).getAbsolutePath());
    }

    private static void addPath(String path) throws Exception {
        Map<String, String> env = getModifiableEnvironment();
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
        if (!dir.mkdirs()) throw new IOException("Couldn't create temp directory " + dir.getName());
        dir.deleteOnExit();
        return dir;
    }

    private static void createTempLib(File dir, String name) throws IOException {
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
        Field props = pe.getDeclaredField("theCaseInsensitiveEnvironment");
        props.setAccessible(true);
        return (Map<String, String>) props.get(null);
    }
}
