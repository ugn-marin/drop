package lab.drop.pipeline;

import lab.drop.data.Data;
import lab.drop.functional.Reducer;

import java.util.*;

/**
 * An internal worker joining input drops from several pipes into one output pipe. Join is a barrier for each index,
 * meaning that a drop is only pushed once it was received from all input pipes. For that reason, all input pipes must
 * be <b>in the same index scope</b>.<br>
 * The drop pushed into the output pipe for every index is computed by the reducer provided, or if wasn't provided, the
 * last drop for every index is pushed.
 * @param <D> The drops type.
 */
final class Join<D> extends PipelineWorker implements OutputWorker<D> {
    private final Pipe<D>[] inputs;
    private final Pipe<D> output;
    private final Reducer<D> reducer;
    private final Map<Long, List<Drop<D>>> allInputs;
    private final Map<Long, Integer> remainingInputs;

    @SafeVarargs
    Join(Reducer<D> reducer, Pipe<D> output, Pipe<D>... inputs) {
        super(true, Data.requireNoneNull(inputs).length);
        if (inputs.length < 2)
            throw new PipelineConfigurationException("Join requires at least 2 input pipes.");
        if (!Data.instancesOf(List.of(inputs), SupplyGate.class).isEmpty())
            throw new PipelineConfigurationException("Joining different index scopes.");
        this.inputs = inputs;
        this.output = Objects.requireNonNull(output, "Output pipe is required.");
        this.reducer = Objects.requireNonNullElse(reducer, Data::last);
        allInputs = new HashMap<>(inputs.length);
        remainingInputs = new HashMap<>(inputs.length);
    }

    Pipe<D>[] getInputs() {
        return inputs;
    }

    @Override
    public Pipe<D> getOutput() {
        return output;
    }

    @Override
    void work() {
        for (var input : inputs)
            submit(() -> input.drain(this::push));
    }

    private void push(Drop<D> drop) throws Exception {
        long index = drop.index();
        boolean push = false;
        Drop<D> next = null;
        synchronized (remainingInputs) {
            allInputs.computeIfAbsent(index, i -> new ArrayList<>()).add(drop);
            if (!remainingInputs.containsKey(index)) {
                remainingInputs.put(index, inputs.length - 1);
            } else {
                int remaining = remainingInputs.get(index);
                push = remaining == 1;
                if (push)
                    next = getNext(index);
                else
                    remainingInputs.put(index, remaining - 1);
            }
            if (!push) {
                while (remainingInputs.containsKey(index))
                    remainingInputs.wait();
                return;
            }
        }
        output.push(next);
    }

    private Drop<D> getNext(long index) {
        var inputs = allInputs.remove(index).stream().map(Drop::drop).toList();
        remainingInputs.remove(index);
        remainingInputs.notifyAll();
        return new Drop<>(index, reducer.apply(inputs));
    }

    @Override
    void internalClose() {
        output.setEndOfInput();
        synchronized (remainingInputs) {
            allInputs.clear();
            remainingInputs.clear();
            remainingInputs.notifyAll();
        }
    }
}
