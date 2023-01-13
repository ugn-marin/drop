package lab.drop.pipeline.workers;

import lab.drop.pipeline.DropFunction;
import lab.drop.pipeline.Pipe;

public class CharLowerFunction extends DropFunction<Character, Character> {

    public CharLowerFunction(Pipe<Character> input, Pipe<Character> output, int parallel) {
        super(input, output, parallel);
    }

    @Override
    public Character apply(Character drop) throws InterruptedException {
        return Character.toLowerCase(drop);
    }
}
