package lab.drop.pipeline.workers;

import lab.drop.pipeline.DropFunction;
import lab.drop.pipeline.Pipe;

public class CharUpperFunction extends DropFunction<Character, Character> {

    public CharUpperFunction(Pipe<Character> input, Pipe<Character> output, int parallel) {
        super(input, output, parallel);
    }

    @Override
    public Character apply(Character drop) throws InterruptedException {
        return Character.toUpperCase(drop);
    }
}
