package lab.drop.pipeline.workers;

import lab.drop.pipeline.DropTransformer;
import lab.drop.pipeline.Pipe;
import lab.drop.pipeline.SupplyPipe;

import java.util.Collection;
import java.util.List;

public class WordsTransformer extends DropTransformer<Character, String> {
    private final StringBuilder sb = new StringBuilder();

    public WordsTransformer(Pipe<Character> input, SupplyPipe<String> output) {
        super(input, output);
    }

    public WordsTransformer(Pipe<Character> input, SupplyPipe<String> output, int parallel) {
        super(input, output, parallel);
    }

    @Override
    public Collection<String> apply(Character drop) {
        if (drop == ' ' || drop == '\n')
            return nextWord(drop == '\n');
        sb.append(drop);
        return null;
    }

    @Override
    protected Collection<String> getLastDrops() {
        if (!sb.isEmpty())
            return nextWord(true);
        return null;
    }

    private Collection<String> nextWord(boolean endOfLine) {
        String word = sb.toString();
        sb.delete(0, sb.length());
        if (endOfLine)
            word += '\n';
        return List.of(word);
    }
}
