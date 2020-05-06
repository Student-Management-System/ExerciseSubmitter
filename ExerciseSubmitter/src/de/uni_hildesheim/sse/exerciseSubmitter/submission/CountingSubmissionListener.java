package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * A default submission listener that simply counts the number of messages.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class CountingSubmissionListener implements IMessageListener {

    /**
     * Stores the number of (error) messages.
     * 
     * @since 2.00
     */
    private int count = 0;
    
    /**
     * Is called when a message occurs. Transfers the relevant
     * information from the message instance into a newly created
     * Eclipse marker.
     * 
     * @param message a message from the submission server
     * 
     * @since 2.00
     */
    public void notifyMessage(IMessage message) {
        count++;
    }
    
    /**
     * Returns the number of messages registered by the listener and
     * emitted by the submission server.
     * 
     * @return the number of messages
     * 
     * @since 2.00
     */
    public int getCount() {
        return count;
    }
    
    /**
     * Resets the counter to zero.
     * 
     * @since 2.00
     */
    protected void resetCounter() {
        count = 0;
    }

}
