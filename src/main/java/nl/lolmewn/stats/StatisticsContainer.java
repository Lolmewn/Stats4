package nl.lolmewn.stats;

import com.sun.istack.internal.Nullable;
import nl.lolmewn.stats.util.ValuedRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatisticsContainer {

    private final Map<String, String> values = new HashMap<>();
    private final ValuedRunnable<UUID, Map<String, String>> nextGranularity;

    public StatisticsContainer(Map<String, String> values, @Nullable ValuedRunnable<UUID, Map<String, String>> runnable) {
        this.values.putAll(values);
        this.nextGranularity = runnable;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public boolean hasNextGranularity() {
        return this.nextGranularity != null;
    }

    public ValuedRunnable<UUID, Map<String, String>> getNextGranularity() {
        return nextGranularity;
    }
}
