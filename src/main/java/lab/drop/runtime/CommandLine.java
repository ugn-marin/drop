package lab.drop.runtime;

import lab.drop.computation.Units;
import lab.drop.concurrent.Concurrent;
import lab.drop.data.Data;
import lab.drop.flow.Flow;
import lab.drop.functional.Reducer;
import lab.drop.functional.UnsafeRunnable;
import lab.drop.text.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

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
                var consumer = line.isError() ? err : out;
                if (consumer != null)
                    consumer.accept(line.getLine());
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
    private Consumer<String> out;
    private Consumer<String> err;
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
            setOutputConsumers(System.out::println, System.err::println);
            outputLock = new ReentrantLock(true);
        }
        lock = new ReentrantLock(true);
    }

    /**
     * Turns off printing of the output lines if collected.
     * @return This instance.
     */
    public CommandLine noPrints() {
        return setOutputConsumers(x -> {}, x -> {});
    }

    /**
     * Sets the consumers of the output lines collected. The consumers do not affect the lines collection, unless they
     * throw an exception. The consumers can be switched during the process runtime. The default consumers are the
     * standard system output and error streams. If the process has finished, or command line is not set to collect
     * output, this method has no effect.
     * @param out The consumer of the standard output lines.
     * @param err The consumer of the error output lines.
     * @return This instance.
     */
    public CommandLine setOutputConsumers(Consumer<String> out, Consumer<String> err) {
        this.out = Objects.requireNonNull(out, "OUT consumer is null.");
        this.err = Objects.requireNonNull(err, "ERR consumer is null.");
        return this;
    }

    /**
     * Sets the working directory of the process. Has no effect if already started.
     * @param directory The working directory of the process.
     * @return This instance.
     */
    public CommandLine setDirectory(File directory) {
        processBuilder.directory(directory);
        return this;
    }

    /**
     * Adds an environment entry to the process, in addition to the environment it inherits from the current process.
     * Has no effect if already started.
     * @param key The key.
     * @param value The value.
     * @return This instance.
     */
    public CommandLine addEnvironment(String key, String value) {
        return addEnvironment(Map.of(key, value));
    }

    /**
     * Adds environment entries to the process, in addition to the environment it inherits from the current process. Has
     * no effect if already started.
     * @param environment The environment entries to add.
     * @return This instance.
     */
    public CommandLine addEnvironment(Map<String, String> environment) {
        processBuilder.environment().putAll(Objects.requireNonNull(environment, "Environment map is null."));
        return this;
    }

    @Override
    public CommandLineResult call() throws Exception {
        lock.lockInterruptibly();
        try {
            CommandLineResult result = new CommandLineResult();
            long startNano = System.nanoTime();
            process = processBuilder.start();
            if (collectOutput)
                Concurrent.virtual().run(Reducer.suppressor(), getOutputReader(false, result),
                        getOutputReader(true, result));
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
