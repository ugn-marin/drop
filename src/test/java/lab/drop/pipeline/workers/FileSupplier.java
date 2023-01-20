package lab.drop.pipeline.workers;

import lab.drop.concurrent.LazyRunnable;
import lab.drop.pipeline.DropSupplier;
import lab.drop.pipeline.SupplyPipe;

import java.io.File;
import java.io.FileFilter;
import java.util.*;
import java.util.function.Supplier;

public class FileSupplier extends DropSupplier<File> {
    private static final Supplier<File[]> empty = () -> new File[0];

    private final FileFilter filter;
    private final boolean recursive;
    private final LazyRunnable lazyLoadRoot;
    private final Queue<File> files = new PriorityQueue<>();
    private final Queue<File> directories = new PriorityQueue<>();

    public FileSupplier(SupplyPipe<File> output, File root, FileFilter filter, boolean recursive) {
        super(output);
        if (!Objects.requireNonNull(root, "Root is null.").isDirectory())
            throw new IllegalArgumentException(root + " is not a directory.");
        this.filter = filter;
        this.recursive = recursive;
        lazyLoadRoot = new LazyRunnable(() -> fillQueues(root));
    }

    private boolean fillQueues(File directory) {
        boolean filesQueued = fillQueue(files, directory, filter);
        boolean directoriesQueued = recursive && fillQueue(directories, directory, File::isDirectory);
        return filesQueued || directoriesQueued;
    }

    private boolean fillQueue(Queue<File> queue, File directory, FileFilter filter) {
        return queue.addAll(List.of(Objects.requireNonNullElseGet(directory.listFiles(filter), empty)));
    }

    @Override
    public Optional<File> get() {
        lazyLoadRoot.run();
        if (!files.isEmpty())
            return Optional.of(files.remove());
        if (directories.isEmpty())
            return Optional.empty();
        var directory = directories.poll();
        while (directory != null) {
            if (fillQueues(directory))
                break;
            directory = directories.poll();
        }
        return get();
    }
}
