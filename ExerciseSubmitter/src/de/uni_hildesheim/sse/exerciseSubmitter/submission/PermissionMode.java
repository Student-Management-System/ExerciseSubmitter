package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * Defines the basic task permissions.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public enum PermissionMode {
    
    /**
     * Denotes an invisible task.
     * 
     * @since 2.00
     */
    INVISIBLE,
    
    /**
     * Denotes a task ready for submission.
     * 
     * @since 2.00
     */
    SUBMISSION,
    
    /**
     * Denotes a task being in the review phase.
     * 
     * @since 2.00
     */
    REVIEW,
    
    /**
     * Denotes a task being ready for replay.
     * 
     * @since 2.00
     */
    REPLAY;
}