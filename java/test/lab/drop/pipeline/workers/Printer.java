package lab.drop.pipeline.workers;

import lab.drop.pipeline.Pipe;
import lab.drop.pipeline.DropConsumer;

import java.io.PrintStream;

public class Printer<I> extends DropConsumer<I> {
    private final PrintStream ps;

    public Printer(PrintStream ps, Pipe<I> input, int parallel) {
        super(input, parallel);
        this.ps = ps;
    }

    @Override
    public void accept(I drop) throws InterruptedException {
        ps.print(drop);
    }

    @Override
    protected void close() {
        ps.println();
        ps.flush();
    }
}
