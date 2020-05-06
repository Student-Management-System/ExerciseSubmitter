package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * A callback interface to be called when the calculation of an 
 * asynchronous calculation has finished.
 * 
 * @param <F> the result of the calculation
 * 
 * @author eichelberger
 * @since 2.00
 * @version 2.00
 */
public interface ProgressFinishedListener<F> {

    /**
     * Called when the calculation of a progress visualized by a progress
     * bar is finished.
     * 
     * @param finished the result of the calculation
     * @param exception an exception occurred while calculating the result, 
     *        <b>null</b> if no exception occurred
     * 
     * @since 2.00
     */
    public void progressFinished(F finished, Exception exception);

}
