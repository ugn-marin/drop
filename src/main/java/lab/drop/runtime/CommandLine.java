package lab.drop.runtime;

import lab.drop.computation.Units;
import lab.drop.concurrent.Concurrent;
import lab.drop.data.Data;
import lab.drop.flow.Flow;
import lab.drop.functional.Reducer;
import lab.drop.functional.UnsafeRunnable;
import lab.drop.text.Text;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An OS command line process executor. Designed as a simplified ProcessBuilder wrapper, to be used directly or by
 * extending this class. Can be called multiple times, but only allows one runtime at any given moment.
 */
public class CommandLine implements Callable<CommandLine.CommandLineResult> {

    /**
     * This class represents the result of the command line process execution, like status and output lines.
     */
    public final class CommandLineResult {
        private int exitStatus = -1;
        private final List<CommandLineOutputLine> output;
        private long nanoTimeTook;

        CommandLineResult() {
            output = new ArrayList<>(collectOutput ? 1 : 0);
        }

        /**
         * Returns true if the exit status is 0.
         */
        public boolean isSuccessful() {
            return exitStatus == 0;
        }

        /**
         * Returns the process exit status.
         */
        public int getExitStatus() {
            return exitStatus;
        }

        /**
         * Returns the total process execution time in nanoseconds.
         */
        public long getNanoTimeTook() {
            return nanoTimeTook;
        }

        /**
         * Returns the output lines if collected. Else, an empty list.
         */
        public List<CommandLineOutputLine> getOutput() {
            return List.copyOf(output);
        }

        void addOutput(CommandLineOutputLine line) throws InterruptedException {
            outputLock.lockInterruptibly();
            try {
                output.add(line);
                var stream = line.isError() ? err : out;
                if (stream != null)
                    stream.println(line.getLine());
            } finally {
                outputLock.unlock();
            }
        }

        CommandLineResult returned(int exitStatus, long nanoStart) {
            this.exitStatus = exitStatus;
            nanoTimeTook = Units.Time.sinceNano(nanoStart);
            return this;
        }

        @Override
        public String toString() {
            return String.format("%s returned %d in %s", Data.first(processBuilder.command()), exitStatus,
                    Units.Time.describeNano(nanoTimeTook));
        }
    }

    /**
     * A command line output line.
     */
    public static final class CommandLineOutputLine {
        private final Date timestamp = new Date();
        private final String line;
        private final boolean isError;

        CommandLineOutputLine(String line, boolean isError) {
            this.line = line;
            this.isError = isError;
        }

        /**
         * Returns the time the line finished printing.
         */
        public Date getTimestamp() {
            return timestamp;
        }

        /**
         * Returns the line content.
         */
        public String getLine() {
            return line;
        }

        /**
         * Returns true if error output, or an IO exception in outputs interception, else false.
         */
        public boolean isError() {
            return isError;
        }

        @Override
        public String toString() {
            return line;
        }
    }

    protected ProcessBuilder processBuilder;
    private final boolean collectOutput;
    private PrintStream out;
    private PrintStream err;
    private final ReentrantLock lock;
    private ReentrantLock outputLock;
    private Process process;

    /**
     * Constructs a command line.
     * @param command The command. Only non-null elements are used.
     */
    public CommandLine(Object... command) {
        this(true, command);
    }

    /**
     * Constructs a command line.
     * @param collectOutput If true, the standard output lines are collected, else ignored. Default is true.
     * @param command The command. Only non-null elements are used.
     */
    public CommandLine(boolean collectOutput, Object... command) {
        var commandList = List.of(Text.toStrings(command));
        if (commandList.isEmpty())
            throw new IllegalArgumentException("No valid command components.");
        processBuilder = new ProcessBuilder(commandList);
        this.collectOutput = collectOutput;
        if (collectOutput) {
            setOutputStreams(System.out, System.err);
            outputLock = new ReentrantLock(true);
        }
        lock = new ReentrantLock(true);
    }

    /**
     * Turns off printing of the output lines if collected.
     * @return This instance.
     */
    public CommandLine noPrints() {
        setOutputStreams(null, null);
        return this;
    }

    /**
     * Sets the output streams printing the output lines collected. If a null stream is passed, the lines of the
     * according stream will not be printed. In any case the streams do not affect the lines collection, unless printing
     * throws an exception. The streams can be switched during the process runtime. The default streams are the standard
     * system output and error streams. If the process has finished, or command line is not set to collect output, this
     * method has no effect.
     */
    public void setOutputStreams(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    /**
     * Sets the working directory of the process. Has no effect if already started.
     * @param directory The working directory of the process.
     */
    public void setDirectory(File directory) {
        processBuilder.directory(directory);
    }

    /**
     * Adds an environment entry to the process, in addition to the environment it inherits from the current process.
     * Has no effect if already started.
     * @param key The key.
     * @param value The value.
     */
    public void addEnvironment(String key, String value) {
        processBuilder.environment().put(key, value);
    }

    /**
     * Adds environment entries to the process, in addition to the environment it inherits from the current process. Has
     * no effect if already started.
     * @param environment The environment entries to add.
     */
    public void addEnvironment(Map<String, String> environment) {
        processBuilder.environment().putAll(Objects.requireNonNull(environment, "Environment map is null."));
    }

    @Override
    public CommandLineResult call() throws Exception {
        lock.lockInterruptibly();
        try {
            CommandLineResult result = new CommandLineResult();
            long startNano = System.nanoTime();
            process = processBuilder.start();
            if (collectOutput)
                Concurrent.run(Reducer.suppressor(), getOutputReader(false, result), getOutputReader(true, result));
            return result.returned(process.waitFor(), startNano);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Executes the command line.
     * @return True if result is successful, else false.
     */
    public boolean attempt() {
        return Flow.success(this, CommandLineResult::isSuccessful).get();
    }

    private UnsafeRunnable getOutputReader(boolean isError, CommandLineResult result) {
        return () -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(isError ? process.getErrorStream() :
                    process.getInputStream()))) {
                Flow.forEach(br.lines(), line -> result.addOutput(new CommandLineOutputLine(line, isError)));
            } catch (IOException e) {
                result.addOutput(new CommandLineOutputLine(e.toString(), true));
            }
        };
    }

    /**
     * Destroy the process if started.
     */
    public void stop() {
        if (process != null)
            process.destroy();
    }
}
