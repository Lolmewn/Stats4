package nl.lolmewn.stats;

import java.util.UUID;

public interface Statistic {

    void enable();

    void disable();

    boolean isEnabled();

    StatisticsContainer getContainer(UUID uuid, int level);

    String getName();
}
