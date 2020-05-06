package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.io.File;
import java.io.Serializable;

/**
 * Defines the data of a directory to be submitted. (Initial intention
 * of this interface was a bit different than this version.)
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.00
 */
public abstract interface ISubmission extends Serializable {

    /**
     * Defines the results of a Submission.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    public enum Result {
        
        /**
         * The submission was successful.
         * 
         * @since 1.00
         */
        SUCCESSFUL,
        
        /**
         * The submission was empty.
         * 
         * @since 1.00
         */
        EMPTY,

        /**
         * The submission was erroneous.
         * 
         * @since 1.00
         */
        FAILED,

        /**
         * The POST-Commit-Checks were successful.
         * @since 3.00 
         */
        POST_SUCCESS,

        /**
         * The POST-Commit-Checks were not successful.
         * @since 3.00 
         */
        POST_FAILED;

    }
    
    /**
     * Sets the path of the directory to be submitted or where
     * to replay a submission to.
     * 
     * @param path the path to be considered
     * 
     * @since 2.00
     */
    public void setPath(File path);

    /**
     * Returns the path of the directory to be submitted or where
     * to replay a submission to.
     * 
     * @return the path to be submitted
     * 
     * @since 2.00
     */
    public File getPath();

    /**
     * Returns the result of an operation on this instance.
     * 
     * @return the result of the operation
     * 
     * @since 2.00
     */
    public Result getResult();

    /**
     * Returns the entire message of the last execution of an operation
     * on this instance. An operation must not return a message.
     * 
     * @return the message(s) of the last operation
     * 
     * @since 2.00
     */
    public String getMessage();

    /**
     * Unparses the message of the last execution returned by 
     * {@link #getMessage()}. Unparsing may deliver individual
     * messages if the messages is in an internal XML format.
     * 
     * @param listener a listener to be informed about each 
     *        individual (sub)message
     * @return the message itself unparsed from XML or the same 
     *         result as {@link #getMessage()} if no XML is in 
     *         the message
     * 
     * @since 2.00
     */
    public String getUnparsedMessage(IMessageListener listener);

    /**
     * Stores if the result of the execution.
     * 
     * @param result the result of the execution
     * 
     * @since 2.00
     */
    public void setResult(Result result);

    /**
     * Sets the message to be returned by {@link #getMessage()}.
     * 
     * @param message the new message after executing an operation
     *        on this instance.
     * 
     * @since 2.00
     */
    public void setMessage(String message);

}
