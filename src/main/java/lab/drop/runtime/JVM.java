package lab.drop.runtime;

import lab.drop.data.Data;
import lab.drop.text.Text;

import java.nio.file.Path;
import java.util.List;

/**
 * A JVM command line executor, getting a main class from the classpath, and running it in a new process using the same
 * classpath as the current JVM, with optional parameters and arguments.
 */
public class JVM extends CommandLine {
    private static final String executable = Path.of(System.getProperty("sun.boot.library.path"), "java").toString();
    private static final String classpath = System.getProperty("java.class.path");

    /**
     * Constructs a JVM executor.
     * @param mainClass The main class of the new JVM.
     * @param args Optional main arguments. Only non-null elements are used.
     */
    public JVM(Class<?> mainClass, Object... args) {
        this(true, null, mainClass, args);
    }

    /**
     * Constructs a JVM executor.
     * @param collectOutput If true, the standard output lines are collected, else ignored. Default is true.
     * @param jvmParameters Optional JVM parameters to include in the command line.
     * @param mainClass The main class of the new JVM.
     * @param args Optional main arguments. Only non-null elements are used.
     */
    public JVM(boolean collectOutput, List<String> jvmParameters, Class<?> mainClass, Object... args) {
        super(collectOutput, Data.flat(executable, jvmParameters, "-cp", classpath, mainClass.getName(),
                Text.toStrings(args)));
    }
}
