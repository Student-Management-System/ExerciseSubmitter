package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * Provides access to the messages created by the submission server.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public interface IMessageListener {

    /**
     * Is called when a message occurs.
     * 
     * @param message a message from the submission server
     * 
     * @since 2.00
     */
    public void notifyMessage(IMessage message);

}
