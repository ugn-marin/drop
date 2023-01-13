package lab.drop.runtime;

/**
 * Common environment properties.
 */
public interface Environment {
    String userName = System.getProperty("user.name");
    String userHome = System.getProperty("user.home");
    String workingDir = System.getProperty("user.dir");
    String tempDir = System.getProperty("java.io.tmpdir");
}
