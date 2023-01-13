package lab.drop.pipeline;

/**
 * A drop wrapping containing its assigned index in the scope.
 * @param <D> The drop type.
 */
record Drop<D>(long index, D drop) {}
