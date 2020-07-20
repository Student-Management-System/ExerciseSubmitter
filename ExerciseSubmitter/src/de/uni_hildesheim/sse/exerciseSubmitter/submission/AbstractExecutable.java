package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;

/**
 * An abstract basic implementation of the progress executable
 * with fixed generics type for this application.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public abstract class AbstractExecutable implements
        Executable<ISubmission> {

    /**
     * Stores the total number of steps to be executed.
     * 
     * @since 2.00
     */
    protected int numberOfSteps;

    /**
     * Stores the current/next step number to be executed.
     * 
     * @since 2.00
     */
    private int step;

    /**
     * Stores the submission to be executed.
     * 
     * @since 2.00
     */
    private ISubmission submission;

    /**
     * Represents the task/exercise on the server to determine the path of the submission.
     * 
     * @since 2.00
     */
    private Assignment assignment;

    /**
     * Stores the progress listener for this executable.
     * 
     * @since 2.00
     */
    private ProgressListener<ISubmission> listener;

    /**
     * Creates a new executable.
     * 
     * @param submission an object describing the parameters of the submission
     * @param assignment Representation of the task/exercise on the server to determine the path of the submission.
     * 
     * @since 2.00
     */
    public AbstractExecutable(ISubmission submission, Assignment assignment) {
        numberOfSteps = 1;
        step = 1;
        this.submission = submission;
        this.assignment = assignment;
    }

    /**
     * Returns the total number of steps to be executed.
     * 
     * @return the total number of steps
     * 
     * @since 2.00
     */
    public final int getNumberOfSteps() {
        return numberOfSteps;
    }

    /**
     * Changes the current step index.
     * 
     * @param step the new step index
     * 
     * @since 2.00
     */
    protected void setStep(int step) {
        this.step = step;
    }

    /**
     * Returns the associated submission data.
     * 
     * @return the associated submission data
     * 
     * @since 2.00
     */
    public ISubmission getSubmission() {
        return submission;
    }
    
    /**
     * Returns the {@link Assignment} to be submitted/replayed.
     * 
     * @return the exercise/task
     * 
     * @since 2.00
     */
    public Assignment getAssignment() {
        return assignment;
    }

    /**
     * Returns if the execution is interruptible.
     * 
     * @return <code>true</code> if the execution is interruptible,
     *         <code>false</code> else
     * 
     * @since 2.00
     */
    public boolean isInterruptible() {
        return false;
    }

    /**
     * Sets the progress listener for this executable.
     * 
     * @param listener the progress listener
     * 
     * @since 2.00
     */
    public void setProgressListener(ProgressListener<ISubmission> listener) {
        this.listener = listener;
    }

    /**
     * Returns the progress listener for this executable.
     * 
     * @return the progress listener (may be <b>null</b>)
     * 
     * @since 2.00
     */
    public ProgressListener<ISubmission> getListener() {
        return listener;
    }

    /**
     * Executes the specified step.
     * 
     * @param step the step to be executed (1-{@link #getNumberOfSteps()}
     * @throws Exception an arbitrary exception to be translated to
     *         a {@link CommunicationException}.
     * 
     * @since 2.00
     */
    // checkstyle: stop exception type check
    protected abstract void executeStep(int step) throws Exception;
    // checkstyle: resume exception type check

    /**
     * Executes all steps ({@link #executeStep(int)}) and notifies the listener.
     * 
     * @since 2.00
     */
    public void run() {
        try {
            while (step <= getNumberOfSteps()) {
                if (null != listener) {
                    listener.notifyNextStep(getDescription(step));
                }
                executeStep(step);
                step++;
                if (null != listener) {
                    listener.processedStep(step);
                }
            }
        // checkstyle: stop exception type check
        } catch (Exception e) {
        // checkstyle: resume exception type check
            if (null != listener) {
                listener.notifyExceptionOccurred(e);
            }
        }
        if (null != listener) {
            listener.finished(finishedToMax(), submission);
        }
    }

    /**
     * Returns if a progress bar should be set to the maximum
     * value at the end of this executable.
     * 
     * @return <code>true</code> if a progress bar should be set
     *         to the maximum, <code>false</code> else
     * 
     * @since 2.00
     */
    protected boolean finishedToMax() {
        return true;
    }

    /**
     * Interrupts the execution. Does nothing in this
     * implementation.
     * 
     * @since 2.00
     */
    public void interrupt() {
    }

    /**
     * Returns the description of the specified step.
     * 
     * @param step the step the description should be returned for
     * @return the description of step <code>i</code>
     * 
     * @since 2.00
     */
    protected abstract String getDescription(int step);

    /**
     * Execute all steps in the current thread.
     * 
     * @throws CommunicationException in the case of a 
     *         communication or I/O error
     * 
     * @since 2.00
     */
    public void executeAllSteps() throws CommunicationException {
        run();
    }

}
