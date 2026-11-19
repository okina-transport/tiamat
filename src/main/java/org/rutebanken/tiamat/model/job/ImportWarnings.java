package org.rutebanken.tiamat.model.job;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the warnings raised while an import job runs, wherever they are raised (mappers, entities...).
 * The warnings are bound to the thread of the import job: {@link #start()} and {@link #stop()} must be called
 * by this thread. Outside an import job, the warnings are ignored.
 */
public final class ImportWarnings {

    private static final ThreadLocal<List<JobError>> WARNINGS = new ThreadLocal<>();

    private ImportWarnings() {
    }

    public static void start() {
        WARNINGS.set(new ArrayList<>());
    }

    public static void add(JobError warning) {
        List<JobError> warnings = WARNINGS.get();
        if (warnings != null) {
            warnings.add(warning);
        }
    }

    /**
     * @return the warnings collected since {@link #start()}
     */
    public static List<JobError> stop() {
        List<JobError> warnings = WARNINGS.get();
        WARNINGS.remove();
        return warnings != null ? List.copyOf(warnings) : List.of();
    }
}
