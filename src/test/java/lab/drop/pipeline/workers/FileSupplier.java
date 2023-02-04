package lab.drop.pipeline.workers;

import lab.drop.concurrent.LazyRunnable;
import lab.drop.pipeline.DropSupplier;
import lab.drop.pipeline.SupplyPipe;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

public class FileSupplier extends DropSupplier<File> {
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

    private void fillQueues(File directory) {
        fillQueue(files, directory, filter);
        if (recursive)
            fillQueue(directories, directory, File::isDirectory);
    }

    private void fillQueue(Queue<File> queue, File directory, FileFilter filter) {
        var filesList = directory.listFiles(filter);
        if (filesList != null)
            queue.addAll(List.of(filesList));
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
            fillQueues(directory);
            if (!files.isEmpty())
                return Optional.of(files.remove());
            directory = directories.poll();
        }
        return Optional.empty();
    }
}
