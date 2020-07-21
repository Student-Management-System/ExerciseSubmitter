package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * Represents a concrete submission of a solution.
 * 
 * @author Holger Eichelberger
 * @author El-Sharkawy
 * @since 2.00
 * @version 2.1
 */
public interface IVersionedSubmission {

    /**
     * Returns the date of a submission.
     * 
     * @return the date of a submission
     * 
     * @since 2.00
     */
    public String getDate();
    
    /**
     * Returns the Author of the submission.
     * 
     * @return The Author of the submission
     * 
     * @since 2.1
     */
    public String getAuthor();

}
