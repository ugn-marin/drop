package lab.drop.runtime;

import java.lang.management.ManagementFactory;

/**
 * Common environment information.
 */
public abstract class Environment {

    private Environment() {}

    /**
     * Returns the operating system name system property.
     */
    public static String os() {
        return System.getProperty("os.name");
    }

    /**
     * Returns the user name system property.
     */
    public static String userName() {
        return System.getProperty("user.name");
    }

    /**
     * Returns the user home directory path system property.
     */
    public static String userHome() {
        return System.getProperty("user.home");
    }

    /**
     * Returns the process working directory path system property.
     */
    public static String workingDir() {
        return System.getProperty("user.dir");
    }

    /**
     * Returns the temporary files directory path system property.
     */
    public static String tempDir() {
        return System.getProperty("java.io.tmpdir");
    }

    /**
     * Returns true if a debugger is defined on the JVM.
     */
    public static boolean isDebuggerPresent() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(arg -> arg.contains("jdwp"));
    }

    /**
     * Returns true if current operating system is Windows.
     */
    public static boolean isWindows() {
        return os().startsWith("Windows");
    }
}
