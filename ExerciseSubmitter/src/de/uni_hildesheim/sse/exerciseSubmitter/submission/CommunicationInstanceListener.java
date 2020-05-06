package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * Defines a listener that informs about the progress of contacting 
 * servers and getting basic information.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public interface CommunicationInstanceListener {

    /**
     * Notifies this instance about the maximum number of servers.
     * 
     * @param number the maximum number of servers
     * 
     * @since 2.00
     */
    public void notifyNumberOfServers(int number);

    /**
     * Notifies that the program executes the specified step.
     * 
     * @param text a description of the the step
     * @param number the number of the step to be executed
     *        
     * @since 2.00
     */
    public void doStep(String text, int number);
    
    /**
     * Notifies this instance that contacting all servers
     * has been finished.
     * 
     * @param error <code>true</code> if an error occurred,
     *        <code>false</code> else
     * 
     * @since 2.00
     */
    public void notifyContactingFinished(boolean error);

    /**
     * Notifies this instance that contacting all servers
     * will be started after returning from this method.
     * 
     * @since 2.00
     */
    public void notifyContactingStarted();

}
