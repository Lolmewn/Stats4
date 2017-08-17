package nl.lolmewn.stats.util;

@FunctionalInterface
public interface ValuedRunnable<T> {

    void run(T value);
}
