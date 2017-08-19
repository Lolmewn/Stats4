package nl.lolmewn.stats.util;

@FunctionalInterface
public interface ValuedRunnable<T, V> {

    V run(T value);
}
