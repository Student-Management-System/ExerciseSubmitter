package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * An exception for wrapping submission protocol specific
 * exception to a common type. By using the message constants
 * this class should help hiding implementation data from the
 * (unexperienced) user.
 * 
 * @author Holger Eichelberger
 * @author El-Sharkawy
 * @since 2.00
 * @version 2.1
 */
public class CommunicationException extends Exception {

    /**
     * Stores the version identifier for serialization.
     * 
     * @since 2.00
     */
    private static final long serialVersionUID = -7421044743285200445L;

    /**
     * Stores the message to be maid available to
     * the user.
     * 
     * @since 2.00
     */
    private PublicMessage publicMessage;

    /**
     * Creates a new communication exception.
     * 
     * @param publicMessage the message instance
     *        to be provided to the user
     * @param throwable the throwable which is 
     *        responsible for the reason of this
     *        exception
     * 
     * @since 2.00
     */
    public CommunicationException(PublicMessage publicMessage, 
        Throwable throwable) {
        super(throwable);
        this.publicMessage = publicMessage;
    }
    
    /**
     * Returns the message to be made available to
     * the user.
     * 
     * @return the message to be made available to
     *         the user
     * 
     * @since 2.00
     */
    public PublicMessage getPublicMessage() {
        return publicMessage;
    }

    /**
     * Returns the message of this exception, in this case
     * the text of the message to be made available to
     * the user.
     * 
     * @return the textual description
     * 
     * @since 2.00
     */
    public String getMessage() {
        return publicMessage.getMessage();
    }
    
    /**
     * Defines the interface for (extensible) public messages.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    public interface PublicMessage {

        /**
         * Returns the message text.
         * 
         * @return the message text of this error message
         * 
         * @since 2.00
         */
        public String getMessage();

    }

    /**
     * Defines the public messages to be provided to the
     * user in the case of an error.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    public enum SubmissionPublicMessage implements PublicMessage {

        /**
         * Denotes the case of an invalid submission repository URL.
         * 
         * @since 2.00
         */
        INVALID_REPOSITORY_URL("Submission system cannot be contacted."),

        /**
         * Denotes the case of an invalid user/password combination.
         * 
         * @since 2.00
         */
        INVALID_USER_PASSWORD("Invalid user/password combination."),

        /**
         * Denotes the case of an invalid (preconfigured) repository
         * structure.
         * 
         * @since 2.00
         */
        INVALID_REPOSITORY_STRUCTURE("Server not reachable or invalid file "
            + "system structure on submission server. Please check also your "
            + "user name and team name."),

        /**
         * Denotes the case of communication problem when replaying 
         * previous submissions.
         * 
         * @since 2.00
         */
        PROBLEM_PREVIOUS_SUBMISSIONS(
            "Problem with previous submissions or permissions on server. " 
            + "In particular, this message may occur because you did not " 
            + "submit any files before, the submission deadline " 
            + "passed by or the current task is being corrected by the " 
            + "supervisors."),

        /**
         * Denotes the case of a conflict with previous submissions.
         * 
         * @since 2.00
         */
        FILE_CONFLICT("Conflict between submitted file and file on "
            + "the server."),

        /**
         * Denotes the case that writing to temporary directories/files
         * leads to an error.
         * 
         * @since 2.00
         */
        TEMPORARY_FILE_CREATION_ERROR(
            "Error creating temporary file or directory on the "
            + " local computer."),

        /**
         * Denotes an error while reading the directory structure of 
         * all tasks/users in the repository.
         * 
         * @since 2.00
         */
        ERROR_READING_REPOSITORY_DIRECTORY_STRUCTURE(
            "Error reading file system structure on submission server. "
            + "Network is ok?"),

        /**
         * Denotes the case of submitting to an invalid task/exercise.
         * 
         * @since 2.00
         */
        INVALID_SUBMISSION(
            "You have selected an invalid exercise for submission.\n"
            + "Possibly, the deadline has passed by."),

        /**
         * Denotes the case of input/output errors while working on the local
         * file system.
         * 
         * @since 2.00
         */
        FILE_IO_ERROR("General file input/output error on the local computer."),

        /**
         * Denotes the case of errors while reading the global configuration.
         * 
         * @since 2.00
         */
        CONFIGURATION_ERROR("Error in/while reading the global configuration."),

        /**
         * Denotes the case that a concrete user cannot be authenticated.
         * 
         * @since 2.00
         */
        AUTHENTICATION_ERROR("Cannot authenticate user."),

        /**
         * Denotes the case that a concrete submission plugin
         * (configuration) is not handled.
         * 
         * @since 2.00
         */
        PLUGIN_NOT_HANDLED("Submission plugin not handled!"),

        /**
         * Denotes the case that the number of review credits is
         * invalid.
         * 
         * @since 2.00
         */
        INVALID_REVIEW_CREDITS("Number of credits is invalid"),
        
        /**
         * Denotes the case that the number of review credits is
         * invalid.
         * 
         * @since 2.00
         */
        UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER("Could not contact student management server.");

        /**
         * Stores the message assigned to this message constant.
         * 
         * @since 2.00
         */
        private String message;

        /**
         * Creates a new public message constant.
         * 
         * @param message the message text
         * 
         * @since 2.00
         */
        private SubmissionPublicMessage(String message) {
            this.message = message;
        }

        /**
         * Returns the message text.
         * 
         * @return the message text of this error message
         * 
         * @since 2.00
         */
        public String getMessage() {
            return message;
        }
    }

}
