package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils.DialogType;
import net.ssehub.exercisesubmitter.protocol.backend.NetworkException;
import net.ssehub.exercisesubmitter.protocol.backend.ServerNotFoundException;
import net.ssehub.exercisesubmitter.protocol.backend.UnknownCredentialsException;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;
import net.ssehub.exercisesubmitter.protocol.frontend.SubmitterProtocol;

/**
 * Defines the communication between submission client (Eclipse plugin) and the server.
 * 
 * @author Alexander Schmehl
 * @author El-Sharkawy
 * @since 1.0
 * @version 2.1
 */
public abstract class SubmissionCommunication implements IPathFactory {

    /**
     * Stores all available communication instances.
     * 
     * @since 2.0
     */
    private static List<SubmissionCommunication> commInstances;
    
    /**
     * Handles the connection to the <b>Student Management System</b>.
     */
    private SubmitterProtocol mgmtProtocol;
    
    /**
     * Stores the name of the user for submissions to the server.
     * 
     * @since 2.00
     */
    private String usernameForSubmission;
    
    /**
     * Stores the name of the user communicating with the server.
     * 
     * @since 1.00
     */
    private String username;

    /**
     * Stores the password of the user communicating with the server.
     * 
     * @since 1.00
     */
    private String password;
    
    /**
     * Creates a new submission communication instance. To be called by an
     * appropriate {@link SubmissionPlugin} instance.
     * 
     * @param username
     *            the name of the user which will communicate with a concrete
     *            communication server (also used as 
     *            {@link #usernameForSubmission})
     * @param password
     *            the password of <code>username</code>
     * 
     * @since 2.10
     */
    protected SubmissionCommunication(String username, String password) {
        this.mgmtProtocol = new SubmitterProtocol(IConfiguration.INSTANCE.getProperty("auth.server"),
            IConfiguration.INSTANCE.getProperty("stdmgmt.server"),
            IConfiguration.INSTANCE.getProperty("course"),
            IConfiguration.INSTANCE.getProperty("svn.server"));
        try {
            mgmtProtocol.login(username, password);
        } catch (UnknownCredentialsException e) {
            GuiUtils.openDialog(DialogType.ERROR, "Credentials are unknown by the student management system, "
                + "please check that you use your RZ credentials.");
        } catch (ServerNotFoundException e) {
            GuiUtils.openDialog(DialogType.ERROR, IConfiguration.INSTANCE.getProperty("stdmgmt.server") + " could not "
                + "be reached, please check your internet connection.");
        }
        this.username = username;
        this.usernameForSubmission = username;
        this.password = password;
    }

    // ---------------------------- getter/setter -----------------------------

    /**
     * Provides the API to query the <b>Student Management System</b>.
     * @return The connection to the <b>Student Management System</b>.
     */
    public SubmitterProtocol getStudentMgmtProtocol() {
        return mgmtProtocol;
    }
    
    /**
     * Cleans up this instance.
     * 
     * @since 1.20
     */
    protected abstract void cleanup();
    
    /**
     * Returns the name of the protocol implemented by this class. The string
     * will be considered when reading the communication based submission
     * configuration. <i>Note:</i> Currently no mechanism for avoiding
     * duplicate protocol names is realized.
     * 
     * @return the name of the implemented protocol
     * 
     * @since 2.00
     */
    public abstract String getProtocol();

    /**
     * Returns if this communication instance allows the replay of (reviewed)
     * submissions.
     * 
     * @return <code>true</code> if the communication instance/protocol allows
     *         the replay of submissions, <code>false</code> else
     * 
     * @since 2.00
     */
    public abstract boolean allowsReplay();

    /**
     * Returns the user name of the user which will communicate with the server.
     * 
     * @param forSubmission if the user name for submission is requested 
     *        otherwise the user name for authentication
     * @return the user name
     * 
     * @since 1.00
     */
    public String getUserName(boolean forSubmission) {
        return (forSubmission) ? usernameForSubmission : username;
    }
    
    /**
     * Specifies a special user name for submission. The user name
     * given in the constructor must have write access to the 
     * appropriate directories on the server.
     * 
     * @param usernameForSubmission the user name used for submissions
     *        (but not for authentication)
     * 
     * @since 1.00
     */
    public void setUserNameForSubmission(String usernameForSubmission) {
        this.usernameForSubmission = usernameForSubmission;
    }

    /**
     * Returns the password of the user which will communicate with the server.
     * 
     * @return the password
     * 
     * @since 1.00
     */
    public String getPassword() {
        return password;
    }

    // ------------------------------- submission operations
    // ---------------------

    /**
     * Authenticates the user by his/her stored data.
     * 
     * @return <code>true</code>, if the user was authenticated,
     *         <code>false</code> else
     * @throws CommunicationException
     *             in the case of any (protocol specific) error
     * 
     * @since 1.00
     */
    protected abstract boolean authenticateUser() throws CommunicationException;

    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be submitted/reviewed/replayed.
     * 
     * @param mode specifies which kind of assignments shall be returned, must not be <tt>null</tt>.
     * @return the top-level path/task/exercise names of exercises that can
     *         currently be submitted/reviwed/replayed
     * 
     * @since 2.1
     */
    public List<Assignment> getAssignments(PermissionMode mode) {
        List<Assignment> availableAssignments;
        try {
            switch (mode) {
            case SUBMISSION:
                availableAssignments = mgmtProtocol.getOpenAssignments();
                break;
            case REVIEW:
                availableAssignments = mgmtProtocol.getReviewableAssignments();
                break;
            case REPLAY:
                availableAssignments = mgmtProtocol.getReviewedAssignments();
                break;
            case INVISIBLE:
                availableAssignments = new ArrayList<>();
                break;
            default:
                System.err.println("Unexpected mode '" + mode + "' returning list of submitable assignments.");
                availableAssignments = mgmtProtocol.getOpenAssignments();
                break;
            }
        } catch (NetworkException e) {
            GuiUtils.openDialog(DialogType.ERROR, "Could not query Studenten Management System to retrieve list of "
                + "open assignments.");
            availableAssignments = new ArrayList<>();
        }
        
        return availableAssignments;
    }
    
    /**
     * Returns the user names (second-level directories).
     * 
     * @return the user names
     * 
     * @since 2.00
     */
    public abstract List<String> getUserNames();

    /**
     * Submits an exercise stored in the specified {@link ISubmission} instance.
     * 
     * @param submission
     *            the information on directory (and its subdirectories) to be
     *            submitted
     * @param assignment
     *            top-level path/task/exercise name representing the
     *            task/exercise to be submitted. Valid values can be obtained by
     *            calling {@link #getAvailableForSubmission()}.
     * @return an executable object which can be executed at once or in
     *         combination with a progress visualization mechanism
     * @throws CommunicationException
     *             error by network, file input/output, ...
     * 
     * @since 1.00
     */
    public abstract Executable<ISubmission> submit(ISubmission submission, Assignment assignment)
        throws CommunicationException;

    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be submitted.
     * 
     * @return the top-level path/task/exercise names of exercises that can
     *         currently be submitted
     * 
     * @since 1.00
     */
    public abstract List<Assignment> getAvailableForSubmission();
    
    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be replayed.
     * 
     * @return the top-level path/task/exercise names of exercises that can
     *         currently be replayed
     * 
     * @since 1.00
     */
    public abstract List<Assignment> getSubmissionsForReplay();

    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be reviewed.
     * 
     * @return the top-level path/task/exercise names of exercises that can
     *         currently be reviewed
     * 
     * @since 2.00
     */
    public abstract List<Assignment> getSubmissionsForReview();
    
    /**
     * Returns the contents of the last submission on <code>task</code>.
     * 
     * @param assignment The task to return the directory listing for
     * @return the directory listing for <code>task</code>
     * @throws CommunicationException error by network, file input/output, ...
     * 
     * @since 2.00
     */
    public abstract List<SubmissionDirEntry> getLastContents(Assignment assignment) throws CommunicationException;
    
    /**
     * Returns the list of versioned /dated submissions of a concrete task.
     * 
     * @param assignment The submission that should be returned
     * 
     * @return a list of versioned/dated submissions, may be empty
     * 
     * @throws CommunicationException
     *             if any communication error occurs
     * 
     * @since 2.00
     */
    public abstract List<IVersionedSubmission> getSubmissionsForReplay(Assignment assignment)
        throws CommunicationException;

    /**
     * Re-initializes data stored in this communication object.
     * 
     * @throws CommunicationException
     *             if any communication error occurs
     * 
     * @since 2.00
     */
    public abstract void reInitialize() throws CommunicationException;

    /**
     * Replays a server-stored submission.
     * 
     * @param submission The information where to store the submission on the local computer
     * @param assignment The assignment representing the task/exercise to be submitted. Valid values can be obtained by
     *        calling {@link #getSubmissionsForReplay()}.
     * @param listener An optional listener to be informed on the progress of the replay operation
     * @return submission (might be refactored to also return an {@link Executable})
     * @throws CommunicationException If any wrapped error occurrences
     * 
     * @since 1.00
     */
    public abstract ISubmission replaySubmission(ISubmission submission, Assignment assignment,
        ProgressListener<ISubmission> listener) throws CommunicationException;
    
    /**
     * Replays an entire task stored (i.e. all submissions) to a local 
     * directory.
     * 
     * @param path The target-path where to replay the submissions to (contents will be deleted before replaying the
     *     submissions)
     * @param assignment The assignment representing the task/exercise to be submitted. Valid values can be obtained by
     *     calling {@link #getSubmissionsForReplay()}.
     * @param listener An optional listener to be informed on the progress of the replay operation
     * @param factory an instance able to create paths in the file system
     * @throws CommunicationException If any wrapped error occurrences
     * 
     * @since 2.00
     */
    public abstract void replayEntireTask(File path, Assignment assignment, ProgressListener<ISubmission> listener, 
        IPathFactory factory) throws CommunicationException;

    /**
     * Replays a server-stored (dated) submission.
     * 
     * @param submission
     *            the information where to store the submission on the local
     *            computer
     * @param version
     *            the information carrying the date/time of the submission.
     *            Valid values can be obtained by calling
     *            {@link #getSubmissionsForReplay(String)}.
     * @param listener
     *            an optional listener to be informed on the progress of the
     *            replay operation
     * @return submission (might be refactored to also return an
     *         {@link Executable})
     * @throws CommunicationException
     *             any wrapped error occurrences
     * 
     * @since 1.00
     */
    public abstract ISubmission replaySubmission(ISubmission submission,
        IVersionedSubmission version, ProgressListener<ISubmission> listener)
        throws CommunicationException;

    /**
     * Creates the <code>subdirectory</code> in the specified
     * <code>path</code>.
     * 
     * @param path the target path
     * @param subdirectory the sub directory to be created
     * @return the created directory as File object
     * 
     * @since 2.00
     */
    public File createPath(File path, String subdirectory) {
        File result = new File(path, subdirectory);
        result.mkdirs();
        return result;
    }

    /**
     * Adjusts the files after replay.
     * 
     * @param targetDir the entire path to the eclipse directory
     * 
     * @since 2.00
     */
    public void adjustFilesAfterReplay(File targetDir) {
        IConfiguration.INSTANCE.adjustFilesAfterReplay(targetDir);
    }
    
    /**
     * Returns the submission log (if available), i.e., information about 
     * errors and warnings occurred while testing the submission.
     * 
     * @param task
     *            the task identifier describing the reviewed task/exercise
     * @param userName
     *            the user name uniquely identifying an individual user or a
     *            user group (see {@link #getRealUsers(String)}
     * @return the log (may be <b>null</b> for none)
     * @throws CommunicationException if any error occurs
     */
    public abstract String getSubmissionLog(String task, String userName) 
        throws CommunicationException;
    
    // --------- static instance administration -----------------------

    // checkstyle: stop parameter number check
    
    /**
     * Returns the concrete communication instances available for the specified
     * user. On the first call, {@link #commInstances} will be initialized
     * according to the existing plugins ({@link #authenticateUser()}). On a
     * following call, the stored data on the instances will be validated by
     * calling {@link #reInitialize()}.
     * 
     * @param userName
     *            the user name of the user to be connected to the server(s)
     * @param password
     *            the password of <code>userName</code>
     * @param submissionUser an optional specialized user name for submission
     * @param listener
     *            an optional listener for the notification on the progress of
     *            the execution
     * @param asReviewer initialize this instance in reviewer mode - 
     *        this may show additional exercises to be submitted but
     *        finally the access permissions on the server should 
     *        prevent from misuse
     * @return the list of communication instances
     * @throws CommunicationException
     *             thrown if any error occurs
     * 
     * @since 2.00
     */
    public static final synchronized List<SubmissionCommunication> getInstances(String userName, String password,
        boolean asReviewer, String submissionUser, CommunicationInstanceListener listener)
        throws CommunicationException {

        // use the dummy listener if none is provided
        if (null == listener) {
            listener = new EmptySubmissionInstanceListener();
        }

        // do an update if instances are present
        if (null != commInstances && commInstances.size() > 0 && null == submissionUser) {
            return updateCommunicationInstances(listener);
        }

        // otherwise read configuration and do plugin instantiation
        listener.notifyContactingStarted();
        clearInstances();
        commInstances = new ArrayList<SubmissionCommunication>();
        int count = getConfiguratedProtocolsCount();
        listener.notifyNumberOfServers(3 * count);
        try {
            int step = 1;
            for (int i = 1; i <= count; i++) {
                String protocol = IConfiguration.INSTANCE.getProperty("protocol." + i, "");
                listener.doStep("Contacting Server " + i, step++);
                SubmissionCommunication comm = null;
                for (SubmissionPlugin plugin : SubmissionPlugin.getPlugins()) {
                    if (plugin.getProtocol().equalsIgnoreCase(protocol)) {
                        comm = plugin.createInstance(userName, password, asReviewer);
                    }
                }
                if (null == comm) {
                    throw new CommunicationException(CommunicationException.SubmissionPublicMessage.PLUGIN_NOT_HANDLED,
                        new Throwable());
                } else {
                    if (null != submissionUser && submissionUser.length() > 0) {
                        comm.setUserNameForSubmission(submissionUser);
                    }
                    listener.doStep("Validating user data and reading file system structure on server " + i, step++);
                    if (!comm.authenticateUser()) {
                        throw new CommunicationException(
                            CommunicationException.SubmissionPublicMessage.AUTHENTICATION_ERROR, new Throwable());
                    }
                    commInstances.add(comm);
                }
                listener.doStep("Finished contacting server " + i, step++);
            }
            listener.notifyContactingFinished(false);
        } catch (CommunicationException exception) {
            clearInstances();
            listener.notifyContactingFinished(true);
            throw exception;
        }

        if (commInstances.isEmpty()) {
            GuiUtils.openDialog(GuiUtils.DialogType.ERROR, "No server accessible!\nPlease contact your supervisor.");
        }
        return commInstances;
    }

    // checkstyle: resume parameter number check
    
    /**
     * Updates ({@link #reInitialize()}) the stored communication instances
     * and returns the available instances.
     * 
     * @param listener
     *            an optional listener for the notification on the progress of
     *            the execution
     * @return the list of communication instances
     * @throws CommunicationException
     *             thrown if any error occurs
     * 
     * @since 2.00
     */
    private static List<SubmissionCommunication> updateCommunicationInstances(
        CommunicationInstanceListener listener) throws CommunicationException {
        listener.notifyContactingStarted();
        listener.notifyNumberOfServers(commInstances.size());
        for (int i = 0; i < commInstances.size(); i++) {
            listener.doStep("Querying file structure of server " + (i + 1), i + 1);
            //commInstances.get(i).reInitialize();
        }
        listener.notifyContactingFinished(false);
        return commInstances;
    }

    /**
     * Returns the number of configured protocols in the global configuration.
     * 
     * @return the number of protocols configured in the global communication
     * 
     * @since 2.00
     */
    private static int getConfiguratedProtocolsCount() {
        int count = 1;
        while (count >= 0) {
            String protocol = IConfiguration.INSTANCE.getProperty("protocol." + count, "").toLowerCase();
            if (protocol.length() == 0) {
                break;
            }
            count++;
        }
        count--;
        return count;
    }

    /**
     * Returns the concrete communication instances available for the specified
     * user configuration. This method calls
     * {@link #getInstances(String, String, String, 
     * CommunicationInstanceListener)}.
     * 
     * @param conf
     *            the user configuration
     * @param listener
     *            an optional listener for the notification on the progress of
     *            the execution
     * @param submissionUser an optional specialized user name for submission
     * @param asReviewer initialize this instance in reviewer mode - 
     *        this may show additional exercises to be submitted but
     *        finally the access permissions on the server should 
     *        prevent from misuse
     * @return the list of communication instances
     * @throws CommunicationException
     *             thrown if any error occurs
     * 
     * @since 2.00
     */
    public static final List<SubmissionCommunication> getInstances(IConfiguration conf, String submissionUser,
         boolean asReviewer, CommunicationInstanceListener listener) throws CommunicationException {
        
        return getInstances(conf.getUserName(), conf.getPassword(), asReviewer, submissionUser, listener);
    }

    /**
     * Clears all stored communication instances. This method may be called in
     * order to invalidate all stored communication data and to reload it when
     * calling {@link #getInstances(String, String, String, 
     * CommunicationInstanceListener)} or {@link #getInstances(IConfiguration, 
     * String, CommunicationInstanceListener)} again.
     * 
     * @since 2.00
     */
    public static final synchronized void clearInstances() {
        if (null != commInstances) {
            for (SubmissionCommunication comm : commInstances) {
                comm.cleanup();
            }
        }
        commInstances = null;
    }

    /**
     * Returns the number of stored communication instances.
     * 
     * @return the number of stored communication instances
     * 
     * @since 2.00
     */
    public static final int getInstancesCount() {
        return commInstances.size();
    }

    /**
     * Returns a specified stored communication instance.
     * 
     * @param index
     *            the index of the instance to be returned
     * @return the specified communication instance
     * @throws ArrayIndexOutOfBoundsException if
     *         <code>index&lt;=0 || index&gt;=
     *         {@link #getInstancesCount()}</code>
     * 
     * @since 2.00
     */
    public static final SubmissionCommunication getInstance(int index) {
        return commInstances.get(index);
    }

}
