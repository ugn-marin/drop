package lab.drop.pipeline.workers;

import lab.drop.pipeline.DropSupplier;
import lab.drop.pipeline.SupplyPipe;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class CharSupplier extends DropSupplier<Character> {
    private final String text;
    private final AtomicInteger index = new AtomicInteger();

    public CharSupplier(String text, SupplyPipe<Character> output, int parallel) {
        super(output, parallel);
        this.text = text;
    }

    @Override
    public Optional<Character> get() throws InterruptedException {
        try {
            return Optional.of(text.charAt(index.getAndIncrement()));
        } catch (StringIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }
}
