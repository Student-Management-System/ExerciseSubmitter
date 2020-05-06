package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * Defines a basic executable in order to control and visualize
 * progress.
 * 
 * @param <F> the type of the result of the associated progress
 *        listener
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public interface Executable<F> extends Runnable {

    /**
     * Sets the progress listener for this executable.
     * 
     * @param listener the progress listener
     * 
     * @since 2.00
     */
    public void setProgressListener(ProgressListener<F> listener);

    /**
     * Returns the description of the executable to be displayed
     * in a progress listener.
     * 
     * @return the description of this executable
     * 
     * @since 2.00
     */
    public String getDescription();

    /**
     * Returns if the execution is interruptible.
     * 
     * @return <code>true</code> if the execution is interruptible,
     *         <code>false</code> else
     * 
     * @since 2.00
     */
    public boolean isInterruptible();

    /**
     * Interrupts the execution.
     * 
     * @since 2.00
     */
    public void interrupt();

    /**
     * Returns the total number of steps to be executed.
     * 
     * @return the total number of steps
     * 
     * @since 2.00
     */
    public int getNumberOfSteps();

    /**
     * Returns the progress listener for this executable.
     * 
     * @return the progress listener (may be <b>null</b>)
     * 
     * @since 2.00
     */
    public ProgressListener<F> getListener();

    /**
     * Execute all steps in the current thread.
     * 
     * @throws CommunicationException in the case of a communication 
     *         or I/O error
     * 
     * @since 2.00
     */
    public void executeAllSteps() throws CommunicationException;

}
