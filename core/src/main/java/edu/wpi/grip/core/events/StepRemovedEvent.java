package edu.wpi.grip.core.events;

import com.google.common.base.MoreObjects;
import edu.wpi.grip.core.Step;

/**
 * An event that occurs when a new step is removed from the pipeline.  This is triggered by the user deleting a step
 * from the GUI.
 */
public class StepRemovedEvent {
    private final Step step;

    /**
     * @param step The step being deleted
     */
    public StepRemovedEvent(Step step) {
        this.step = step;
    }

    /**
     * @return The step being deleted.
     */
    public Step getStep() {
        return this.step;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("step", step)
                .toString();
    }
}
