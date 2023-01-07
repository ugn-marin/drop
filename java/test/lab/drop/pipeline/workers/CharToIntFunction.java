package lab.drop.pipeline.workers;

import lab.drop.pipeline.Pipe;
import lab.drop.pipeline.DropFunction;

public class CharToIntFunction extends DropFunction<Character, Integer> {

    public CharToIntFunction(Pipe<Character> input, Pipe<Integer> output, int parallel) {
        super(input, output, parallel);
    }

    @Override
    public Integer apply(Character drop) {
        return (int) drop;
    }
}
