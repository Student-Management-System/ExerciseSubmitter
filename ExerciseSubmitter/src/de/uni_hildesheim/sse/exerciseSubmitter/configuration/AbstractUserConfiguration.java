package de.uni_hildesheim.sse.exerciseSubmitter.configuration;


import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;
import net.ssehub.exercisesubmitter.protocol.utils.JsonUtils;

/**
 * A default implementation for handling user name and password according to the
 * {@link IConfiguration#store()} conventions.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.2
 */
public abstract class AbstractUserConfiguration extends IConfiguration {

    /**
     * Stores the user name.
     * 
     * @since 1.00
     */
    protected String userName = "";

    /**
     * Stores the (plain text) password of the user.
     * 
     * @since 1.00
     */
    protected String password = "";
    
    /**
     * Used by the ExerciseReviewer only: Stores the information which exercise is reviewed at the used workspace.
     * 
     *  @since 2.2
     */
    private Assignment assignment;

    /**
     * Returns the stored password of the current user (user local
     * configuration).
     * 
     * @return the stored (plain text) password or an empty string in the case
     *         that no password was stored so far
     * 
     * @since 1.00
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Changes the password of the current user (user local configuration). Call
     * {@link #store()} to make this change permanent.
     * 
     * @param password
     *            the (plain text) password to be stored
     * 
     * @since 1.00
     */
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the current user name (user local configuration).
     * 
     * @return the current user name or an empty string if no user name was
     *         stored so far
     * 
     * @since 1.00
     */
    @Override
    public String getUserName() {
        return userName;
    }

    /**
     * Changes the user name of the current user (user local configuration).
     * Call {@link #store()} to make this change permanent.
     * 
     * @param userName
     *            the user name to be stored
     * 
     * @since 1.00
     */
    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    /**
     * Returns the currently reviewed {@link Assignment} as JSON string to store this information in the workspace
     * preferences.
     * This is only used by the ExerciseReviewer.
     * @return The serialized {@link Assignment} or <tt>null</tt> if no assignment was specified.
     * 
     * @since 2.2
     */
    protected String getAssignmentAsJSON() {
        return (assignment != null) ? JsonUtils.createParser().serialize(assignment) : null;
    }
    
    /**
     * Sets the currently reviewed {@link Assignment}.
     * This is only used by the ExerciseReviewer.
     * @param assignmentAsJSON The serialized {@link Assignment} or <tt>null</tt> if no assignment was specified.
     */
    protected void setAssignment(String assignmentAsJSON) {
        if (null != assignmentAsJSON && !assignmentAsJSON.isEmpty()) {
            assignment = JsonUtils.createParser().deserialize(assignmentAsJSON, Assignment.class);
        }
    }
    
    /**
     * Returns the currently reviews assignment.
     * This is only used by the ExerciseReviewer and, thus, will return <tt>null</tt> at the ExerciseSubmitter.
     * @return The currently reviewed {@link Assignment} or <tt>null</tt> if not specified.
     */
    public Assignment getAssignment() {
        return assignment;
    }
    
    /**
     * Sets the currently reviewed assignment.
     * This is only used by the ExerciseReviewer.
     * @param assignment Sets the assignment, which is reviewed at the current workspace.
     */
    public void setAsssignment(Assignment assignment) {
        this.assignment = assignment;
    }
}
