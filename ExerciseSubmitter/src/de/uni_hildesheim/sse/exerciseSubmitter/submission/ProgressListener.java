package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * A progress listener in order to visualize the progress of
 * threaded execution. The template parameter denotes the type
 * of the result.
 * 
 * @param <F> the type of the result of the progress
 * 
 * @author Holger Eichelberger
 * @since 2.0
 * @version 2.0
 */
public interface ProgressListener<F> {

    /**
     * Is called when a step was processed.
     * 
     * @param step the progressed step
     * 
     * @since 2.0
     */
    public void processedStep(int step);

    /**
     * Is called when the total number of steps is changed.
     * 
     * @param steps the new total number of steps
     * 
     * @since 2.0
     */
    public void numberofStepsChanged(int steps);

    /**
     * Is called when processing is finished.
     * 
     * @param max set the associated progress bar to its
     *        maximum value (or keep the current value)
     * @param finished the result of the processing
     * 
     * @since 2.0
     */
    public void finished(boolean max, F finished);

    /**
     * Notifies the beginning of the next step by announcing
     * the next task description (optional).
     * 
     * @param description the next task description to be displayed
     * 
     * @since 2.0
     */
    public void notifyNextStep(String description);

    /**
     * Notifies this listener that an unhandled execption occurred.
     * 
     * @param exception the occurred exception
     * 
     * @since 2.0
     */
    public void notifyExceptionOccurred(Exception exception);

    /**
     * Switches from individual step visualization to sweeping
     * infinite visualization.
     * 
     * @param doSweep <code>true</code> if sweeping should be 
     *        activated, <code>false</code> for individual step
     *        display
     *        
     * @since 2.0
     */
    public void sweep(boolean doSweep);

    /**
     * Returns the current step number.
     * 
     * @return the current processing step number 
     * (1-max, see {@link #numberofStepsChanged(int)})
     * 
     * @since 2.0
     */
    public int getStep();
    
}
