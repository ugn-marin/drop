package lab.drop.pipeline.workers;

import lab.drop.pipeline.Pipe;
import lab.drop.pipeline.DropFunction;

public class IntToCharFunction extends DropFunction<Integer, Character> {

    public IntToCharFunction(Pipe<Integer> input, Pipe<Character> output, int parallel) {
        super(input, output, parallel);
    }

    @Override
    public Character apply(Integer drop) {
        return (char) drop.intValue();
    }
}
