package lab.drop.pipeline.workers;

import lab.drop.pipeline.DropConsumer;
import lab.drop.pipeline.Pipe;

public class CharAccumulator extends DropConsumer<Character> {
    private final StringBuilder sb = new StringBuilder();

    public CharAccumulator(Pipe<Character> input, int parallel) {
        super(input, parallel);
    }

    @Override
    public void accept(Character drop) throws InterruptedException {
        synchronized (sb) {
            sb.append(drop);
        }
    }

    public String getValue() {
        return sb.toString();
    }
}
