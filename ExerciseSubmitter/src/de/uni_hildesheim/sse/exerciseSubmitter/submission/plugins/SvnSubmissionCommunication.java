package de.uni_hildesheim.sse.exerciseSubmitter.submission.plugins;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.channels.FileChannel;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.admin.SVNChangeEntry;

import de.uni_hildesheim.sse.exerciseSubmitter.Activator;
import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.FileChecksumUtil;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IPathFactory;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.AbstractExecutable;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Executable;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.CommonStuff;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.SubmissionDirEntry;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.SubmissionPlugin;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ProgressListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IVersionedSubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;

/**
 * Realizes a specific server communication for subversion (SVN) per https und
 * webdav. This class uses classes from the svnkit.
 * 
 * @author Alexander Schmehl (initial)
 * @since 1.00
 * @version 2.10
 */
public class SvnSubmissionCommunication extends SubmissionCommunication {

    /**
     * Defines the associated plugin instance.
     * 
     * @since 2.00
     */
    public static final SubmissionPlugin PLUGIN = new SubmissionPlugin() {

        /**
         * Creates an instance of the described submission communication class.
         * 
         * @param username
         *            the name of the user which will communicate with a
         *            concrete communication server
         * @param password
         *            the password of <code>username</code>
         * @param asReviewer initialize this instance in reviewer mode - 
         *        this may show additional exercises to be submitted but
         *        finally the access permissions on the server should 
         *        prevent from misuse
         * @param explicitTargetFolder the explicit target folder if not to be 
         *            derived from username (<b>null</b> otherways)
         * @return the created instance
         * 
         * @since 2.00
         */
        @Override
        public SubmissionCommunication createInstance(String userName,
            String password, boolean asReviewer, String explicitTargetFolder) {
            return new SvnSubmissionCommunication(userName, 
                password, asReviewer, explicitTargetFolder);
        }

        /**
         * Returns the name of the protocol implemented by this class. The
         * string will be considered when reading the communication based
         * submission configuration. <i>Note:</i> Currently no mechanism for
         * avoiding duplicate protocol names is realized.
         * 
         * @return the name of the implemented protocol
         * 
         * @since 2.00
         */
        @Override
        public String getProtocol() {
            return "svn";
        }
    };

    /**
     * Stores the date formatter for the versioned submissions.
     * 
     * @since 2.00
     */
    private static final Format SUBMISSION_FORMAT = 
        new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    /**
     * Defines the commit mode. If <code>true</code>, the repository is
     * cleared by deleting all files in the directory before committing the new
     * resources. Otherwise, incremental changes are identified and sent.
     * 
     * @since 2.00
     */
    private static final boolean DELETE_COMMITED_RESOURCES_BEFORE_COMMIT 
        = false;
    
    /**
     * Stores the server including protocol, hostname, repository and basic
     * path.
     * 
     * @since 1.00
     */
    private String server;

    /**
     * Stores the optional log server including protocol, hostname, repository 
     * and basic path.
     * 
     * @since 1.00
     */
    private String logServer;

    /**
     * Stores the tasks/exercises available for submission.
     * 
     * @since 1.00
     */
    private ArrayList<String> availableForSubmission = new ArrayList<String>();

    /**
     * Stores the tasks/exercises available for replay.
     * 
     * @since 1.00
     */
    private ArrayList<String> availableForReplay = new ArrayList<String>();

    /**
     * Stores the tasks/exercises available for review.
     * 
     * @since 2.00
     */
    private ArrayList<String> availableForReview = new ArrayList<String>();
    
    /**
     * Stores the instance representing the SVN repository.
     * 
     * @since 1.00
     */
    private SVNRepository repository;

    /**
     * Stores the instance representing the SVN log-repository.
     * 
     * @since 1.00
     */
    private SVNRepository logRepository;

    /**
     * Stores the authentication manager used to authenticate the user against
     * the repository on {@link #server}.
     * 
     * @since 2.00
     */
    private ISVNAuthenticationManager authManager;
    
    /**
     * Stores if this communication instance is used by a reviewer.
     * 
     * @since 2.00
     */
    private boolean asReviewer;
    
    /**
     * Creates a new submission communication instance. To be called by the
     * {@link #PLUGIN} instance.
     * 
     * @param username
     *            the name of the user which will communicate with a concrete
     *            communication server
     * @param password
     *            the password of <code>username</code>
     * @param asReviewer initialize this instance in reviewer mode - 
     *        this may show additional exercises to be submitted but
     *        finally the access permissions on the server should 
     *        prevent from misuse
     * @param explicitTargetFolder the explicit target folder if not to be 
     *            derived from username (<b>null</b> otherways)
     * 
     * @since 2.10
     */
    private SvnSubmissionCommunication(String username, String password, 
        boolean asReviewer, String explicitTargetFolder) {
        super(username, password, explicitTargetFolder);

        this.asReviewer = asReviewer;
        // do self-configuration
        this.server = normalizeServerName(
            IConfiguration.INSTANCE.getProperty("svn.server"));
        this.logServer = normalizeServerName(
            IConfiguration.INSTANCE.getProperty("svn.log.server"));
    }

    /**
     * Normalizes a server name.
     * 
     * @param server the server name
     * @return the normalized server name
     */
    private String normalizeServerName(String server) {
        if (null != server) {
            // normalize values
            if (!server.startsWith("https://")) {
                server = "https://" + server;
            }
            if (!server.endsWith("/")) {
                server = server + "/";
            }
        }
        return server;
    }
    
    /**
     * Cleans up this instance.
     * 
     * @since 1.20
     */
    protected void cleanup() {
        if (null != repository) {
            repository.closeSession();
        }
        if (null != logRepository) {
            logRepository.closeSession();
        }
    }

    /**
     * Returns the name of the protocol implemented by this class. The string
     * will be considered when reading the communication based submission
     * configuration. <i>Note:</i> Currently no mechanism for avoiding
     * duplicate protocol names is realized.
     * 
     * @return the name of the implemented protocol, the result returned from
     *         the {@link #PLUGIN} instance
     * 
     * @since 2.00
     */
    public String getProtocol() {
        return PLUGIN.getProtocol();
    }

    /**
     * Returns if this communication instance allows the replay of (reviewed)
     * submissions.
     * 
     * @return <code>true</code> if the communication instance/protocol allows
     *         the replay of submissions, <code>false</code> else
     * 
     * @since 2.00
     */
    public boolean allowsReplay() {
        return true;
    }

    /**
     * Initializes svnkit.
     * 
     * @since 2.00
     */
    static {
        DAVRepositoryFactory.setup();
    }

    /**
     * Authenticates the user by his/her stored data. This method calls
     * {@link #reInitialize}.
     * 
     * @return <code>true</code>, if the user was authenticated,
     *         <code>false</code> else
     * @throws CommunicationException
     *             in the case of any (protocol specific) error
     * 
     * @since 1.00
     */
    public boolean authenticateUser() 
        throws CommunicationException {
        if (null != repository) {
            repository.closeSession();
        }
        try {
            repository = SVNRepositoryFactory.create(SVNURL
                .parseURIEncoded(server));
        } catch (SVNException e) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.
                INVALID_REPOSITORY_URL, e);
        }
        // do not call SVNWCUtil.createDefaultAuthenticationManager because
        // it will return a special authentication manager for eclipse which
        // will not work for our purposes
        repository.setAuthenticationManager(createAuthenticationManager());
        reInitialize();
        return true;
    }

    /**
     * Factory method for creating an appropriate authentication manager for the
     * SVN repository. Uses {@link #authManager} as instance singleton
     * attribute. This method was introduced, because this class creates several
     * temporary repository. In older versions, each repository instance created
     * its own authentication manager which caused problems running this plugin
     * in the eclipse environment.
     * 
     * @return the authentication manager appropriate for the stored user data
     *         and password
     * 
     * @since 2.00
     */
    private ISVNAuthenticationManager createAuthenticationManager() {
        // do not call SVNWCUtil.createDefaultAuthenticationManager because
        // it will return a special authentication manager for eclipse which
        // will not work for our purposes
        if (null == authManager) {
            authManager = new DefaultSVNAuthenticationManager(null, true,
                getUserName(false), getPassword());
        }
        return authManager;
    }

    /**
     * Re-initializes data stored in this communication object.
     * 
     * @throws CommunicationException
     *             if any communication error occurs
     * 
     * @since 2.00
     */
    @SuppressWarnings("unchecked")
    public void reInitialize()
        throws CommunicationException {
        availableForSubmission.clear();
        availableForReplay.clear();
        availableForReview.clear();
        Collection entries;

        // retrieve main repository directory contents
        try {
            entries = repository.getDir("", -1, null, (Collection) null);
        } catch (SVNAuthenticationException e) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.
                INVALID_USER_PASSWORD, e);
        } catch (SVNException e) {
            throw new CommunicationException(CommunicationException.
                SubmissionPublicMessage.INVALID_REPOSITORY_STRUCTURE, e);
        }

        // select and store directory entries
        Iterator iterator = entries.iterator();
        ArrayList<SVNDirEntry> dirs = new ArrayList<SVNDirEntry>();
        while (iterator.hasNext()) {
            SVNDirEntry entry = (SVNDirEntry) iterator.next();
            if (entry.getKind() == SVNNodeKind.DIR) {
                dirs.add(entry);
            }
        }

        Map<String, PermissionMode> permissions = readPermissions();
        for (SVNDirEntry dir : dirs) {
            boolean done = false;
            if (null != permissions) {
                String absPath = dir.getName();
                if (!absPath.startsWith("/")) {
                    absPath = "/" + absPath;
                }
                PermissionMode mode = permissions.get(absPath);
                if (null != mode) {
                    done = true;
                    switch (mode) {
                    case REPLAY:
                        availableForReplay.add(dir.getName());
                        break;
                    case REVIEW:
                        availableForReview.add(dir.getName());
                        if (asReviewer) {
                            availableForSubmission.add(dir.getName());
                        }
                        break;
                    case SUBMISSION:
                        availableForSubmission.add(dir.getName());
                        break;
                    case INVISIBLE:
                        break;
                    default:
                        done = false;
                        break;
                    }
                }
            } 
            if (!done) {
                try {
                    if (isRepositoryWritable(server + dir.getName() + "/"
                        + getTargetFolder())) {
                        availableForSubmission.add(dir.getName());
                    } else {
                        repository.getDir(dir.getName(), -1, null,
                            (Collection) null);
                        availableForReplay.add(dir.getName());
                        availableForReview.add(dir.getName());
                    }
                } catch (SVNException e) {
                }
            }
        }
    }
    
    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be reviewed.
     * 
     * @return the top-level path/task/exercise names of exercises that can
     *         currently be reviewed
     * 
     * @since 2.00
     */
    public String[] getSubmissionsForReview() {
        return sortnconvert(availableForReview);
    }
    
    /**
     * Defines the basic task permissions.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private enum PermissionMode {
        
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
    
    /**
     * Reads all permissions from the repository.
     * 
     * @return all permissions stored in the repository, may be
     *         <b>null</b> if this mechanism is not activated 
     *         (legacy mode)
     * 
     * @since 2.00
     */
    private Map<String, PermissionMode> readPermissions() {
        Map<String, PermissionMode> result = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            repository.getFile("permissions", -1, null, out);
            LineNumberReader in = new LineNumberReader(
                new InputStreamReader(new ByteArrayInputStream(
                    out.toByteArray())));

            result = new HashMap<String, PermissionMode>();
            String line;
            do {
                line = in.readLine();
                if (null != line) {
                    StringTokenizer tokenizer = new StringTokenizer(line, "\t");
                    if (2 == tokenizer.countTokens()) {
                        String path = tokenizer.nextToken();
                        String mode = tokenizer.nextToken();
                        if (path.length() > 0 && mode.length() > 0) {
                            try {
                                result.put(path, PermissionMode.valueOf(mode));
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            } while (null != line && in.ready());
            in.close();
            out.close();
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * Returns the list of versioned /dated submissions of a concrete task.
     * Reviewers will always receive the entire list of submissions, 
     * participants will see the entire list only while submission but the 
     * latest (reviewed) version while replay phase.
     * 
     * @param task
     *            the top-level path/task/exercise the dated submissions should
     *            be returned for
     * 
     * @return a list of versioned/dated submissions, may be empty
     * 
     * @throws CommunicationException
     *             if any communication error occurs
     * 
     * @since 2.00
     */
    @SuppressWarnings("unchecked")
    public List<IVersionedSubmission> getSubmissionsForReplay(String task)
        throws CommunicationException {
        List<IVersionedSubmission> result = 
            new ArrayList<IVersionedSubmission>();
        try {
            if (availableForSubmission.contains(task) || asReviewer) {
                String[] targetPaths = new String[1];
                targetPaths[0] = task + "/" + getTargetFolder();
                Collection revisions = repository.log(targetPaths, null, 0,
                    repository.getLatestRevision(), false, false);
                for (Object o : revisions) {
                    SVNLogEntry entry = (SVNLogEntry) o;
                    //unclear use of alternative, disabled 2009-11-02
                    //if (getUserName(true).equals(entry.getAuthor())) {
                    result.add(new SVNSubmission(entry.getDate(), entry
                        .getRevision(), task, getTargetFolder()));
                    //}
                }
            } else {
                Collection dirs = getDirs(repository, -1, 
                    task + "/" + getTargetFolder(), null);
                if (dirs.size() > 0) {
                    SVNSubmission resultSubmission = new SVNSubmission(
                        new Date(), -1, task, getTargetFolder());
                    resultSubmission.setDate("reviewed");
                    result.add(resultSubmission);
                }
            }
        } catch (SVNException e) {
            throw new CommunicationException(CommunicationException.
                SubmissionPublicMessage.PROBLEM_PREVIOUS_SUBMISSIONS, e);
        }
        return result;
    }
    
    /**
     * Returns a composed path.
     * 
     * @param task the task name
     * @param user the user name
     * @return the composed path
     */
    private static String composePath(String task, String user) {
        return task + "/" + user;
    }

    /**
     * Implements the versioned submission interface for SVN repositories.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private class SVNSubmission implements IVersionedSubmission {

        /**
         * Stores the date of the submission.
         * 
         * @since 2.00
         */
        private String date;

        /**
         * Stores the SVN revision denoted by this version instance.
         * 
         * @since 2.00
         */
        private long revision;

        /**
         * Stores the name of the user responsible for this revision.
         * 
         * @since 2.00
         */
        private String user;

        /**
         * Stores the top-level path of the associated task/exercise.
         * 
         * @since 2.00
         */
        private String task;

        /**
         * Creates a new versioned submission instance.
         * 
         * @param date
         *            the date of the version
         * @param revision
         *            the SVN revision number
         * @param task
         *            the top-level path of the associated task/exercise
         * @param user
         *            the user responsible for this revision
         * 
         * @since 2.00
         */
        SVNSubmission(Date date, long revision, String task, String user) {
            this.date = SUBMISSION_FORMAT.format(date);
            this.revision = revision;
            this.user = user;
            this.task = task;
        }

        /**
         * Returns the date of this submission.
         * 
         * @return the date of this submission
         * 
         * @since 2.00
         */
        public String getDate() {
            return date;
        }
        
        /**
         * Changes the date of this submission version.
         * 
         * @param date the new date
         * 
         * @since 2.00
         */
        void setDate(String date) {
            this.date = date;
        }

        /**
         * Returns the SVN revision number associated with this submission.
         * 
         * @return the SVN revision number
         * 
         * @since 2.00
         */
        public long getRevision() {
            return revision;
        }

        /**
         * Returns the entire SVN directory path within the current repository
         * including the user name.
         * 
         * @return the entire SVN directory path
         * 
         * @since 2.00
         */
        public String getDirectory() {
            return composePath(task, user);
        }

        /**
         * Returns the task as the top-level path of the associated
         * task/exercise.
         * 
         * @return the top-level path
         * 
         * @since 2.00
         */
        public String getTask() {
            return task;
        }

        /**
         * Returns the textual representation of this versioned submission.
         * 
         * @return the textual representation (of the date)
         * 
         * @since 2.00
         */
        public String toString() {
            return date;
        }
    }

    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be submitted.
     * 
     * @return the top-level path/task/exercise names of exercises that can
     *         currently be submitted
     * 
     * @since 1.00
     */
    public String[] getAvailableForSubmission() {
        return sortnconvert(availableForSubmission);
    }
    
    /**
     * Returns the user names (second-level directories).
     * 
     * @return the second-level user names
     * 
     * @since 2.00
     */
    @SuppressWarnings("unchecked")
    public List<String> getUserNames() {
        List<String> result = new ArrayList<String>();
        String topLevelDir = null;
        if (!availableForSubmission.isEmpty()) {
            topLevelDir = availableForSubmission.get(0);
        } else if (!availableForReplay.isEmpty()) {
            topLevelDir = availableForReplay.get(0);
        }
        if (null != topLevelDir) {
            try {
                Collection entries = repository.getDir(
                    topLevelDir + "/", -1, null, (Collection) null);
                for (Iterator iter = entries.iterator(); iter.hasNext();) {
                    SVNDirEntry entry = (SVNDirEntry) iter.next();
                    result.add(entry.getName());
                }
            } catch (SVNException e) {
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Submits an exercise stored in the specified {@link ISubmission} instance.
     * 
     * @param submission
     *            the information on directory (and its sub directories) to be
     *            submitted
     * @param task
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
    public Executable<ISubmission> submit(ISubmission submission, String task)
        throws CommunicationException {
        return new SVNCommExecutable(submission, task);
    }

    /**
     * Returns all repository directories (recursively) contained in the
     * specified <code>prefix</code> directory.
     * 
     * @param repository
     *            the repository the directories should be returned for
     * @param revision
     *            the SVN revision number to be considered
     * @param dir
     *            the directory path for which the directory entries should be
     *            returned for
     * @param collection
     *            a collection where to put into the results, may be <b>null</b>
     * @return the subversion entries representing the directories in the given
     *         path, <code>collection</code> if this argument was not 
     *         <b>null</b>
     * @throws SVNException
     *             if any exception occurred
     * 
     * @since 1.00
     */
    private static Collection<SVNDirEntry> getDirs(SVNRepository repository,
        long revision, String dir, Collection<SVNDirEntry> collection)
        throws SVNException {
        String prefixDir = dir;
        if (!prefixDir.endsWith("/")) {
            prefixDir += "/";
        }
        return getDirs(repository, revision, dir, prefixDir, collection);
    }

    /**
     * Returns all repository directories (recursively) contained in the
     * specified <code>prefix</code> directory.
     * 
     * @param repository
     *            the repository the directories should be returned for
     * @param revision
     *            the SVN revision number to be considered
     * @param dir
     *            the directory path for which the directory entries should be
     *            returned for
     * @param prefixDir 
     *            the directory path to be removed from the beginning of 
     *            <code>dir</code>
     * @param collection
     *            a collection where to put into the results, may be <b>null</b>
     * @return the subversion entries representing the directories in the given
     *         path, <code>collection</code> if this argument was not 
     *         <b>null</b>
     * @throws SVNException
     *             if any exception occurred
     * 
     * @since 1.00
     */
    @SuppressWarnings("unchecked")
    private static Collection<SVNDirEntry> getDirs(SVNRepository repository,
        long revision, String dir, String prefixDir, 
        Collection<SVNDirEntry> collection)
        throws SVNException {
        if (null == collection) {
            collection = new ArrayList<SVNDirEntry>();
        }
        Collection c = repository
            .getDir(dir, revision, null, (Collection) null);
        for (Iterator i = c.iterator(); i.hasNext();) {
            SVNDirEntry entry = (SVNDirEntry) i.next();
            String myDir = dir + "/" + entry.getRelativePath();
            if (entry.getKind() == SVNNodeKind.DIR) {
                getDirs(repository, revision, myDir, prefixDir, collection);
            }
            if (myDir.startsWith(prefixDir)) {
                myDir = myDir.substring(prefixDir.length());
            }
            myDir = getParent(myDir);
            if (myDir.length() > 0) {
                myDir += "/";
            }
            entry.setRelativePath(myDir + entry.getRelativePath());
            collection.add(entry);
        }
        return collection;
    }
    
    /**
     * Returns the parent of the specified textual path.
     * 
     * @param path
     *            the path the parent should be returned for
     * @return the parent element of <code>path</code>
     * 
     * @since 2.00
     */
    public static final String getParent(String path) {
        int pos = path.lastIndexOf("/");
        if (pos == 0) {
            return "/";
        } else if (pos > 0) {
            return path.substring(0, pos);
        } else {
            return "";
        }
    }

    /**
     * Implements a data structure containing the information about how to
     * handle a given file.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private class FileInfo {

        /**
         * The subversion file modus for changed, added, removed.
         * 
         * @since 2.00
         */
        private char modus;

        /**
         * Stores the file to be handled.
         * 
         * @since 2.00
         */
        private File file;

        /**
         * Stores the type of {@link #file} as subversion type.
         * 
         * @since 2.00
         */
        private SVNNodeKind kind;

        /**
         * Stores the associated path in the repository.
         * 
         * @since 2.00
         */
        private String svnPath;

        /**
         * Creates a new file info object.
         * 
         * @param modus
         *            subversion file modus for changed, added, removed
         * @param kind
         *            subversion type of <code>file</code>
         * @param file
         *            the file to be handled
         * @param svnPath
         *            associated path in the repository
         * 
         * @since 2.00
         */
        private FileInfo(char modus, SVNNodeKind kind, File file, 
            String svnPath) {
            this.modus = modus;
            this.file = file;
            this.kind = kind;
            this.svnPath = svnPath.replace("\\", "/");
            while (this.svnPath.startsWith("/")) {
                this.svnPath = this.svnPath.substring(1);
            }
        }

        /**
         * Handle the current file information object.
         * 
         * @param editor the submission editor
         * @param currentPath the stack of currently processed paths
         * @throws SVNException if an SVN communication error occurs
         * @throws CommunicationException if a wrapped communication 
         *         error occurs
         * 
         * @since 1.10
         */
        public void handle(ISVNEditor editor, Stack<String> currentPath)
            throws SVNException, CommunicationException {
            while (!currentPath.isEmpty()
                && !svnPath.startsWith(currentPath.peek())) {
                editor.closeDir();
                currentPath.pop();
            }
            String path = svnPath;
            int pos;
            int startPos = 0;
            int opened = 0;
            do {
                pos = path.indexOf("/", startPos);
                if (pos >= 0) {
                    String name = path.substring(0, pos);
                    if (currentPath.isEmpty()
                        || !currentPath.peek().startsWith(name)) {
                        editor.openDir(name, -1);
                        currentPath.push(name);
                    }
                    opened++;
                    startPos = pos + 1;
                } else {
                    String name = path;
                    if (modus == SVNChangeEntry.TYPE_DELETED) {
                        if (kind == SVNNodeKind.DIR && !currentPath.isEmpty() 
                            && currentPath.peek().equals(name)) {
                            editor.closeDir();
                            currentPath.pop();
                        }
                        editor.deleteEntry(name, -1);
                    } else if (kind == SVNNodeKind.DIR
                        && modus == SVNChangeEntry.TYPE_ADDED) {
                        editor.addDir(name, null, -1);
                        currentPath.push(name);
                    } else {
                        if (modus == SVNChangeEntry.TYPE_ADDED) {
                            editor.addFile(name, null, -1);
                        } else {
                            editor.openFile(name, -1);
                        }
                        editor.applyTextDelta(name, null);
                        SVNDeltaGenerator deltaGenerator = 
                            new SVNDeltaGenerator();
                        try {
                            BufferedInputStream is = new BufferedInputStream(
                                new FileInputStream(file));
                            String checksum = deltaGenerator.sendDelta(name,
                                is, editor, true);
                            editor.closeFile(name, checksum);
                            try {
                                is.close();
                            } catch (IOException ioe) {
                            }
                        } catch (FileNotFoundException e) {
                            // diese Exception fangen wir ab; sie sollte nie
                            // vorkommen, und auch nach einer solchen, sollte
                            // wenigstens versucht werden, den rest zu comitten
                            throw new CommunicationException(
                                CommunicationException.SubmissionPublicMessage.
                                FILE_CONFLICT, e);
                        }

                    }
                }
            } while (pos >= 0);
        }
    }

    /**
     * Implements an executable object for a subversion commit.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private class SVNCommExecutable extends AbstractExecutable {

        /**
         * Stores the elements to be committed.
         * 
         * @since 2.00
         */
        private List<FileInfo> checkIn = new ArrayList<FileInfo>();

        /**
         * Stores the currently nested paths.
         * 
         * @since 2.00
         */
        private Stack<String> currentPath = new Stack<String>();

        /**
         * Stores the directory in which the files to be submitted are located.
         * 
         * @since 2.00
         */
        private File submitDir = null;

        /**
         * Stores the repository to which the submission should be sent.
         * 
         * @since 2.00
         */
        private SVNRepository repo;

        /**
         * Stores the editor object guiding the submission/commit action.
         * 
         * @since 2.00
         */
        private ISVNEditor editor;
        
        /**
         * Stores the temporary directory used while commiting resources.
         * 
         * @since 2.00
         */
        private File preCommitTmpDir = null;

        /**
         * Stores the numbers of additional checkout steps in the case of the
         * incremental commit mode.
         * 
         * @since 2.00
         */
        private int numberOfCheckoutSteps = 0;
        
        /**
         * Stores the file checksum utility instance.
         * 
         * @since 2.00
         */
        private FileChecksumUtil checksumUtil = new FileChecksumUtil();
        
        /**
         * Creates a new executable.
         * 
         * @param submission
         *            the submission to be committed
         * @param task
         *            the exercise/task top-level path
         * @throws CommunicationException
         *             any exception occurred while submitting
         * 
         * @since 2.00
         */
        private SVNCommExecutable(ISubmission submission, String task) 
            throws CommunicationException {
            super(submission, task);
            if (availableForSubmission.contains(task)) {
                submitDir = submission.getPath();
                numberOfSteps = 1 + 1; // checkIn will follow
                try {
                    numberOfCheckoutSteps = getDirs(repository, -1,
                        "/" + task + "/" + getTargetFolder(), null).size();
                    numberOfSteps += numberOfCheckoutSteps;
                } catch (SVNException e) {
                    boolean doThrow = true;
                    // single user submission fallback? Try it with own user
                    // name, if successful, use that and set explicit target
                    // folder
                    String userName = getUserName(false);
                    if (!getTargetFolder().equals(userName)) {
                        try {
                            numberOfCheckoutSteps = getDirs(repository, -1,
                                "/" + task + "/" + userName, null).size();
                            numberOfSteps += numberOfCheckoutSteps;
                            setExplicitTargetFolder(userName);
                            doThrow = false;
                        } catch (SVNException e1) {
                            // just ignore and report original error
                        }
                    }
                    if (doThrow) {
                        throw new CommunicationException(
                            CommunicationException.SubmissionPublicMessage.
                            ERROR_READING_REPOSITORY_DIRECTORY_STRUCTURE, e);
                    }
                }
            } else {
                numberOfSteps = 0;
                throw new CommunicationException(
                    CommunicationException.SubmissionPublicMessage.
                    INVALID_SUBMISSION, new Throwable());
            }
        }

        /**
         * Returns if a progress bar should be set to the maximum value at the
         * end of this executable.
         * 
         * @return <code>true</code> if a progress bar should be set to the
         *         maximum, <code>false</code> else
         * 
         * @since 2.00
         */
        protected boolean finishedToMax() {
            return false;
        }

        /**
         * Executes the specified step.
         * 
         * @param step
         *            the step to be executed (1-{@link #getNumberOfSteps()}
         * @throws CommunicationException
         *             an arbitrary exception to be translated to a
         *             {@link CommunicationException}.
         * 
         * @since 2.00
         */
        public void executeStep(int step) throws CommunicationException {
            try {
                if (step == 1) {
                    openTransaction();
                } else if (step == numberOfSteps) {
                    if (checkIn.size() > 0) {
                        closeTransaction();
                    } else {
                        getSubmission().setResult(ISubmission.Result.EMPTY);
                    }
                    repo.closeSession();
                } else {
                    if (checkIn.size() > 0) {
                        FileInfo file = checkIn.get(step - numberOfCheckoutSteps
                            - 2); // 2!!!!!!!!!
                        getListener().notifyNextStep(file.svnPath);
                        file.handle(editor, currentPath);
                    }
                }
            } catch (SVNException e) {
                if (!e.getMessage().trim().endsWith("already exists")) {
                    throw new CommunicationException(
                        CommunicationException.SubmissionPublicMessage.
                        FILE_CONFLICT, e);
                }
            }
        }
        
        /**
         * Opens a commit transaction. 
         * 
         * @throws SVNException any exception while communicating 
         *         with subversion
         * @throws CommunicationException any (wrapped) communication error
         * 
         * @since 2.00
         */
        private void openTransaction() throws SVNException, 
            CommunicationException {
            DAVRepositoryFactory.setup();
            repo = SVNRepositoryFactory.create(SVNURL
                .parseURIEncoded(server + getTask() + "/" 
                    + getTargetFolder()));
            repo.setAuthenticationManager(
                createAuthenticationManager());
            handleCommitedResources(getTask());
            if (checkIn.size() > 0) {
                editor = repo.getCommitEditor(
                    "Submission of " + getTask()
                    + " by " + getUserName(false), null);
                editor.openRoot(-1);
            }
            if (numberOfCheckoutSteps > 0) {
                setStep(numberOfCheckoutSteps + 1);
            }
        }
        
        /**
         * Closes a commit transaction. 
         * 
         * @throws SVNException any exception while communicating 
         *         with subversion
         * @throws CommunicationException any (wrapped) communication error
         * 
         * @since 2.00
         */
        private void closeTransaction() throws SVNException, 
            CommunicationException {
            while (!currentPath.isEmpty()) {
                editor.closeDir();
                currentPath.pop();
            }
            editor.closeDir();

            if (null != getListener()) {
                getListener().notifyNextStep(
                    "Waiting for server results...");
                // getListener().sweep(true);
            }
            try {
                SVNCommitInfo info = editor.closeEdit();
                SVNErrorMessage message = info.getErrorMessage();
                String msg = "";
                if (message != null) {
                    if (message.getErrorCode().equals(
                        SVNErrorCode.REPOS_POST_COMMIT_HOOK_FAILED)) {
                        msg = msg + message.getFullMessage();
                        msg = msg.substring(msg.indexOf('\n'));
                        getSubmission().setMessage(msg);
                        getSubmission().setResult(
                            ISubmission.Result.POST_FAILED);
                    } else {
                        if (msg.length() > 0) {
                            msg = msg + "\n";
                        }
                        msg = msg + message.getFullMessage();
                        message = info.getErrorMessage();
                        getSubmission().setMessage(msg);
                        getSubmission().setResult(
                            ISubmission.Result.SUCCESSFUL);
                    }
                } else {
                    getSubmission().setMessage(msg);
                    getSubmission().setResult(ISubmission.Result.POST_SUCCESS);
                }
            } catch (SVNException e) {
                getSubmission().setResult(ISubmission.Result.FAILED);
                try {
                    editor.abortEdit();
                } catch (SVNException abEx) {
                    // do nothing
                }
                if (0 == e.getErrorMessage().getType()) {
                    boolean done = false;
                    String message = e.getMessage();
                    int pos = message.indexOf('\n');
                    if (pos > 0) { // ensure multiple lines
                        message = message.substring(pos + 1);
                        pos = message.lastIndexOf('\n');
                        if (pos > 0) { // ensure multiple lines
                            message = message.substring(0, pos);
                            done = true;
                        }
                        getSubmission().setMessage(message);
                    }
                    if (!done) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
            if (null != preCommitTmpDir) {
                CommonStuff.rmdir(preCommitTmpDir, true);
            }
            reInitialize();
        }

        /**
         * Is called to handle the resources to be committed. This includes the
         * handling of resources which have been committed to the repository so
         * far. The behavior of this method depends on
         * {@link #DELETE_COMMITED_RESOURCES_BEFORE_COMMIT}.
         * 
         * @param task
         *            the top-level directory denoting the exercise/task in the
         *            repository to be handled
         * @throws SVNException
         *             is thrown in the case of a communication error with the
         *             repository
         * @throws CommunicationException
         *             occurs in the case of a wrapped (communication) exception
         * 
         * @since 2.00
         */
        private void handleCommitedResources(String task) throws SVNException,
            CommunicationException {
            try {
                if (DELETE_COMMITED_RESOURCES_BEFORE_COMMIT) {
                    Collection<SVNDirEntry> contents = getDirs(repository,
                        repository.getLatestRevision(), task + "/" 
                        + getTargetFolder(), null);
                    for (Iterator<SVNDirEntry> iterator = contents.iterator(); 
                        iterator.hasNext();) {
                        SVNDirEntry entry = iterator.next();
                        checkIn.add(new FileInfo(SVNChangeEntry.TYPE_DELETED,
                            entry.getKind(), null, task + "/" 
                            + getTargetFolder() + "/" 
                            + entry.getRelativePath()));
                    }
                    enumElementsToAdd(submitDir.getAbsolutePath(), submitDir,
                        checkIn);
                } else {
                    preCommitTmpDir = CommonStuff.createTmpDir();
                    svnExport(task, preCommitTmpDir.getAbsolutePath(),
                        repository.getLatestRevision(), getListener());

                    Map<String, Object> toBeDeleted = 
                        new HashMap<String, Object>();
                    enumElements(preCommitTmpDir.getAbsolutePath(),
                        preCommitTmpDir, toBeDeleted, true);
                    enumElements(submitDir.getAbsolutePath(), submitDir,
                        toBeDeleted, false);
                    deleteElements(preCommitTmpDir.getAbsolutePath(),
                        preCommitTmpDir, toBeDeleted, checkIn);
                    physicallyDeleteFiles(checkIn);
                    copyFiles(submitDir, submitDir.getAbsolutePath(),
                        preCommitTmpDir, checkIn);

                    List<FileInfo> add = new ArrayList<FileInfo>();
                    List<FileInfo> rest = new ArrayList<FileInfo>();
                    for (FileInfo fi : checkIn) {
                        if (fi.modus == SVNChangeEntry.TYPE_ADDED) {
                            add.add(fi);
                        } else {
                            rest.add(fi);
                        }
                    }
                    checkIn.clear();
                    checkIn.addAll(add);
                    checkIn.addAll(rest);

                    submitDir = preCommitTmpDir;
                }
                numberOfSteps += checkIn.size();
                getListener().numberofStepsChanged(numberOfSteps);
            } catch (IOException ioe) {
                throw new CommunicationException(
                    CommunicationException.SubmissionPublicMessage.
                    FILE_IO_ERROR, ioe);
            }
        }

        /**
         * Enumerates the files and directories in the given <code>path</code>.
         * 
         * @param prefix
         *            prefix path to be removed from <code>path</code>
         * @param path
         *            the path to be recursively enumerated
         * @param map
         *            stores the enumerated elements (to be modified as a side
         *            effect)
         * @param put
         *            <code>true</code> put the new elements into
         *            <code>map</code>, <code>false</code> if the new
         *            elements should be removed from <code>map</code>
         * 
         * @since 2.00
         */
        private void enumElements(String prefix, File path,
            Map<String, Object> map, boolean put) {
            File[] filelist = path.listFiles();
            if (null != filelist) {
                for (File file : filelist) {
                    if ((file.isDirectory()) && file.canRead()) {
                        if (put) {
                            map.put(cutPrefix(prefix, file.getAbsolutePath()),
                                null);
                        } else {
                            map.remove(cutPrefix(prefix, file
                                .getAbsolutePath()));
                        }
                        enumElements(prefix, file, map, put);
                    }
                    if (file.isFile() && file.canRead()) {
                        if (put) {
                            map.put(cutPrefix(prefix, file.getAbsolutePath()),
                                null);
                        } else {
                            map.remove(cutPrefix(prefix, file
                                .getAbsolutePath()));
                        }
                    }
                }
            }
        }

        /**
         * Recursively enumerates all elements in <code>path</code> in order
         * to build up the list of elements to be committed (added).
         * 
         * @param prefix
         *            prefix path to be removed from <code>path</code>
         * @param path
         *            the path to be recursively enumerated
         * @param elements
         *            the result to be modified as a side effect
         * 
         * @since 2.00
         */
        private void enumElementsToAdd(String prefix, File path,
            List<FileInfo> elements) {
            File[] filelist = path.listFiles();
            if (null != filelist) {
                for (File file : filelist) {
                    if ((file.isDirectory()) && file.canRead()
                        && file.getName().equalsIgnoreCase(".svn")) {
                        enumElementsToAdd(prefix, path, elements);
                        elements.add(new FileInfo(SVNChangeEntry.TYPE_ADDED,
                            SVNNodeKind.DIR, file, cutPrefix(prefix, file
                                .getAbsolutePath())));
                    }
                    if (file.isFile() && file.canRead()) {
                        elements.add(new FileInfo(SVNChangeEntry.TYPE_ADDED,
                            SVNNodeKind.FILE, file, cutPrefix(prefix, file
                                .getAbsolutePath())));
                    }
                }
            }
        }

        /**
         * Recursively enumerates all elements in <code>path</code> in order
         * to build up the list of elements to be removed from the repository.
         * 
         * @param prefix
         *            prefix path to be removed from <code>path</code>
         * @param path
         *            the path to be recursively enumerated
         * @param map
         *            the map built up as the result of running
         *            {@link #enumElements(String, File, Map, boolean)} twice,
         *            for the new elements and for the elements in the
         *            repository
         * @param elements
         *            the result to be modified as a side effect
         * 
         * @since 2.00
         */
        private void deleteElements(String prefix, File path,
            Map<String, Object> map, List<FileInfo> elements) {
            File[] filelist = path.listFiles();
            if (null != filelist) {
                for (File file : filelist) {
                    if ((file.isDirectory()) && file.canRead()) {
                        deleteElements(prefix, file, map, elements);
                        String svnPath = cutPrefix(prefix, file
                            .getAbsolutePath());
                        if (map.containsKey(svnPath)) {
                            elements.add(new FileInfo(
                                SVNChangeEntry.TYPE_DELETED, SVNNodeKind.DIR,
                                file, svnPath));
                        }
                    }
                    if (file.isFile() && file.canRead()) {
                        String svnPath = cutPrefix(prefix, file
                            .getAbsolutePath());
                        if (map.containsKey(svnPath)) {
                            elements.add(new FileInfo(
                                SVNChangeEntry.TYPE_DELETED, SVNNodeKind.FILE,
                                file, svnPath));
                        }
                    }
                }
            }
        }
        
        /**
         * Physically delete the files which were marked as to be deleted in 
         * {@link #deleteElements(String, File, Map, List)}. Deletion is not
         * done in {@link #deleteElements(String, File, Map, List)} due to
         * a recursive descent. If this method is not executed after 
         * {@link #deleteElements(String, File, Map, List)}, this implementation
         * will not correctly handle renamed files. In particular, on Windows
         * systems problems may occur because Windows does not know the 
         * difference between lower and upper case characters in file names.
         * 
         * @param elements the list of file modifications
         * 
         * @since 2.00
         */
        private void physicallyDeleteFiles(List<FileInfo> elements) {
            for (FileInfo fi : elements) {
                if (SVNChangeEntry.TYPE_DELETED == fi.modus 
                    && fi.file.exists()) {
                    if (SVNNodeKind.FILE == fi.kind) {
                        fi.file.delete();
                    } else {
                        CommonStuff.rmdir(fi.file, true);
                    }
                }
            }
        }

        /**
         * Copies all files from <code>source</code> to <code>target</code>
         * and adds appropriate information on adding or updating the files in
         * <code>elements</code>.
         * 
         * @param source
         *            the source directory
         * @param prefix
         *            the prefix path to be removed from <code>source</code>
         * @param target
         *            the target directory
         * @param elements
         *            the repository operation description list to be modified
         *            as a side effect
         * @throws CommunicationException
         *             a wrapping exception in the case of (communication
         *             errors)
         * 
         * @since 2.00
         */
        private void copyFiles(File source, String prefix, File target,
            List<FileInfo> elements) throws CommunicationException {
            File[] filelist = source.listFiles();
            if (null != filelist) {
                for (File file : filelist) {
                    String svnPath = cutPrefix(prefix, file.getAbsolutePath());
                    File tFile = new File(target.getAbsolutePath()
                        + File.separator + svnPath);
                    if (file.isDirectory()) {
                        if (!tFile.exists()) {
                            String tf = tFile.getAbsolutePath();
                            int pos = prefix.length();
                            do {
                                pos = tf.indexOf(File.separator, pos + 1);
                                File tf1;
                                if (pos > 0) {
                                    tf1 = new File(tf.substring(0, pos));
                                } else {
                                    tf1 = tFile;
                                }
                                if (!tf1.exists()) {
                                    tf1.mkdir();
                                    elements.add(new FileInfo(
                                        SVNChangeEntry.TYPE_ADDED,
                                        SVNNodeKind.DIR, tf1, cutPrefix(target
                                            .getAbsolutePath()
                                            + File.separator, tf1
                                            .getAbsolutePath())));
                                }
                            } while (pos >= 0);
                        }
                        copyFiles(file, prefix, target, elements);
                    } else {
                        if (tFile.exists()) {
                            if (!checksumUtil.equalByCheckSum(file, tFile)) {
                                elements.add(new FileInfo(
                                    SVNChangeEntry.TYPE_UPDATED, 
                                    SVNNodeKind.FILE, tFile, svnPath));
                            }
                        } else {
                            elements.add(new FileInfo(
                                SVNChangeEntry.TYPE_ADDED, SVNNodeKind.FILE,
                                tFile, svnPath));
                        }
                        try {
                            FileChannel sourceChannel = new FileInputStream(
                                file).getChannel();
                            FileChannel targetChannel = new FileOutputStream(
                                tFile).getChannel();
                            targetChannel.transferFrom(sourceChannel, 0,
                                sourceChannel.size());
                            sourceChannel.close();
                            targetChannel.close();
                        } catch (IOException ioe) {
                            throw new CommunicationException(
                                CommunicationException.SubmissionPublicMessage.
                                FILE_IO_ERROR, ioe);
                        }
                    }
                }
            }
        }

        /**
         * Removes the specified <code>prefix</code> from the given
         * <code>path</code>.
         * 
         * @param prefix
         *            the prefix to be removed
         * @param path
         *            the path <code>prefix</code> should be removed from
         * @return <code>path</code> without <code>prefix</code>
         * 
         * @since 2.00
         */
        private String cutPrefix(String prefix, String path) {
            if (path.startsWith(prefix)) {
                return path.substring(prefix.length(), path.length());
            } else {
                return path;
            }
        }

        /**
         * Returns the description of the executable to be displayed in a
         * progress listener.
         * 
         * @return the description of this executable
         * 
         * @since 2.00
         */
        public String getDescription() {
            return "Submission is running";
        }

        /**
         * Returns the description of the specified step.
         * 
         * @param step
         *            the step the description should be returned for
         * @return the description of step <code>i</code>
         * 
         * @since 2.00
         */
        public String getDescription(int step) {
            return "";
        }

    }

    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be replayed.
     * 
     * @return the top-level path/task/exercise names of exercises that can
     *         currently be replayed
     * 
     * @since 1.00
     */
    public String[] getSubmissionsForReplay() {
        return sortnconvert(availableForReplay);
    }

    /**
     * Replays a server-stored submission.
     * 
     * @param submission
     *            the information where to store the submission on the local
     *            computer
     * @param task
     *            top-level path/task/exercise name representing the
     *            task/exercise to be submitted. Valid values can be obtained by
     *            calling {@link #getSubmissionsForReplay()}.
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
    public ISubmission replaySubmission(ISubmission submission, String task,
        ProgressListener<ISubmission> listener) throws CommunicationException {
        try {
            Collection<SVNDirEntry> contents = getDirs(repository,
                repository.getLatestRevision(), task + "/" 
                + getTargetFolder(), null);
            if (null != listener) {
                listener.numberofStepsChanged(contents.size());
            }
            if (!contents.isEmpty()) {
                CommonStuff.rmdir(submission.getPath(), false);
                
                if (availableForReplay.contains(task)) {
                    svnExport(task, submission.getPath().getAbsolutePath(),
                        repository.getLatestRevision(), listener);
                    adjustFilesAfterReplay(submission.getPath());
                }
    
                if (null != listener) {
                    listener.finished(true, submission);
                }
    
                submission.setResult(ISubmission.Result.SUCCESSFUL);
            }
            return submission;
        } catch (SVNException e) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.
                PROBLEM_PREVIOUS_SUBMISSIONS, e);
        }
    }

    /**
     * Replays an entire task stored (i.e. all submissions) to a local
     * directory.
     * 
     * @param path 
     *            the target-path where to replay the submissions to,
     *            individual project directories in the path will be
     *            cleared on the file system.  
     * @param task 
     *            top-level path/task/exercise name representing the
     *            task/exercise to be submitted. Valid values can be obtained by
     *            calling {@link #getSubmissionsForReplay()}.
     * @param listener
     *            an optional listener to be informed on the progress of the
     *            replay operation
     * @param factory an instance able to create paths in the file system
     * @throws CommunicationException
     *             any wrapped error occurrences
     * 
     * @since 2.00
     */
    @SuppressWarnings("unchecked")
    public void replayEntireTask(File path, String task, 
        ProgressListener<ISubmission> listener, 
        IPathFactory factory) throws CommunicationException {
        
        if (null == factory) {
            factory = this;
        }

        SVNException thrownException = null;
        try {
            List<Object> skipDirs = new ArrayList<Object>();
            Collection dirs = repository.getDir(task, 
                repository.getLatestRevision(), null, (Collection) null);
            if (null != listener) {
                for (Object object : dirs) {
                    SVNDirEntry entry = (SVNDirEntry) object;
                    if (entry.getKind() == SVNNodeKind.DIR) {
                        Collection<SVNDirEntry> contents = getDirs(repository,
                            repository.getLatestRevision(), task + "/" 
                            + entry.getRelativePath(), null);
                        if (contents.isEmpty()) {
                            skipDirs.add(object);
                        }
                    }
                }
            }
            
            for (Object skip : skipDirs) {
                dirs.remove(skip);
            }

            ExportEditor exportEditor = new ExportEditor(null, listener);
            for (Object object : dirs) {
                SVNDirEntry entry = (SVNDirEntry) object;
                if (entry.getKind() == SVNNodeKind.DIR) {
                    File target = factory.createPath(path, entry.getName());
                    CommonStuff.rmdir(target, false);
                    exportEditor.setTargetDirectory(target);
                    try {
                        svnExport(task + "/" + entry.getRelativePath(), 
                            repository.getLatestRevision(), exportEditor, 
                            false);
                        adjustFilesAfterReplay(target);
                    } catch (SVNException e) {
                        if (null == thrownException) {
                            thrownException = e;
                        } else {
                            if (IConfiguration.INSTANCE.isDebuggingEnabled()) {
                                Activator.log("ExerciseSubmitter", e);
                            }
                        }
                    }
                }
            }
            
            if (null != listener) {
                listener.finished(true, null);
            }

        } catch (SVNException e) {
            thrownException = e;
        }
        if (null != thrownException) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.
                PROBLEM_PREVIOUS_SUBMISSIONS, thrownException);
        }
    }
    
    /**
     * Returns the contents of the last submission on <code>task</code>.
     * 
     * @param task the task name to return the directory listing for
     * @return the directory listing for <code>task</code>
     * 
     * @throws CommunicationException in the case of communication errors
     * 
     * @since 2.00
     */
    public List<SubmissionDirEntry> getLastContents(String task) 
        throws CommunicationException {
        List<SubmissionDirEntry> result = new ArrayList<SubmissionDirEntry>();
        try {
            Collection<SVNDirEntry> contents = getDirs(repository,
                repository.getLatestRevision(), task + "/" + getTargetFolder(),
                null);
            for (SVNDirEntry entry : contents) {
                SubmissionDirEntry newEnt = 
                    new SubmissionDirEntry(entry.getRelativePath(), 
                        entry.getSize(), entry.getDate(), 
                        SVNNodeKind.DIR == entry.getKind(), entry.getAuthor());
                result.add(newEnt);
            }
        } catch (SVNException e) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.
                PROBLEM_PREVIOUS_SUBMISSIONS, e);
        }
        Collections.sort(result);
        return result;
    }

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
    public ISubmission replaySubmission(ISubmission submission,
        IVersionedSubmission version, ProgressListener<ISubmission> listener)
        throws CommunicationException {
        if (!(version instanceof IVersionedSubmission)) {
            throw new IllegalArgumentException();
        }
        try {
            SVNSubmission subm = (SVNSubmission) version;
            Collection<SVNDirEntry> contents = getDirs(repository, 
                subm.getRevision(), subm.getDirectory(), null);
            if (null != listener) {
                listener.numberofStepsChanged(contents.size());
            }
            if (!contents.isEmpty()) {
                CommonStuff.rmdir(submission.getPath(), false);
                
                svnExport(subm.getTask(), submission.getPath().
                    getAbsolutePath(), subm.getRevision(), listener);
                adjustFilesAfterReplay(submission.getPath());
                
                if (null != listener) {
                    listener.finished(true, submission);
                }
                submission.setResult(ISubmission.Result.SUCCESSFUL);
            }
            return submission;
        } catch (SVNException e) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.
                PROBLEM_PREVIOUS_SUBMISSIONS, e);
        }
    }

    /**
     * Exports a SVN repository or a sub path to a given directory.
     * 
     * @param svnPath
     *            the path in the repository to be exported
     * @param targetPath
     *            the file system path where to export the repository contents
     *            to
     * @param revision
     *            the revision to be exported
     * @param listener an optional progress listener to make the 
     *            progress visible
     * @throws SVNException
     *             a wrapping exception in the case of (communication) errors
     * 
     * @since 1.00
     */
    private void svnExport(String svnPath, String targetPath, long revision,
        ProgressListener<ISubmission> listener) throws SVNException {
        ExportEditor exportEditor = new ExportEditor(new File(targetPath),
            listener);
        svnExport(svnPath, revision, exportEditor, true);
    }

    /**
     * Exports a SVN repository or a sub path to a given directory.
     * 
     * @param svnPath
     *            the path in the repository to be exported
     * @param revision
     *            the revision to be exported
     * @param exportEditor
     *            a (reusable) export editor object
     * @param addUser add the user name to the path
     * @throws SVNException
     *             a wrapping exception in the case of (communication) errors
     * 
     * @since 1.00
     */
    private void svnExport(String svnPath, long revision,
        ExportEditor exportEditor, boolean addUser) throws SVNException {
        String url = server + svnPath;
        if (addUser) {
            url += "/" + getTargetFolder();
        }
        SVNRepository tmprepo = SVNRepositoryFactory.create(SVNURL
            .parseURIEncoded(url));
        tmprepo.setAuthenticationManager(createAuthenticationManager());
        tmprepo.checkout(revision, null, true, exportEditor);
        tmprepo.closeSession();
    }
    
    /**
     * Converts an ArrayList of strings into a sorted array of strings.
     * 
     * @param list
     *            the list to be sorted/converted
     * @return the sorted/converted array
     * 
     * @since 1.00
     */
    private String[] sortnconvert(List<String> list) {
        String[] foo = {};
        foo = list.toArray(foo);
        java.util.Arrays.sort(foo);
        return foo;
    }

    /**
     * Returns if a repository is writable. Therefore, a file of an arbitrary
     * name is added to a temporary instance of the repository. This operation
     * does not change the repository because it is not really commited.
     * 
     * @param repositoryURL
     *            complete SVN access URL
     * @return <code>true</code> if <code>repositoryURL</code> is writable,
     *         <code>false</code> else
     * 
     * @throws SVNException in the case of an erroneous repository communication
     * 
     * @since 1.00
     */
    private boolean isRepositoryWritable(String repositoryURL)
        throws SVNException {
        SVNRepository tmprepository = SVNRepositoryFactory.create(SVNURL
            .parseURIEncoded(repositoryURL));
        tmprepository.setAuthenticationManager(createAuthenticationManager());
        ISVNEditor editor = tmprepository.getCommitEditor(
            "writing test (to be ignored)", null);
        boolean result = false;
        try {
            editor.openRoot(-1);
            editor.addFile(".schreibtest", null, -1);
            editor.abortEdit();
            result = true;
        } catch (SVNException e) {
            try {
                editor.abortEdit();
            } catch (SVNException e1) {
            }
        }
        tmprepository.closeSession();
        return result;
    }

    @Override
    public String getSubmissionLog(String task, String userName) 
        throws CommunicationException {
        if (null == logRepository && null != logServer 
            && logServer.length() > 0) {
            try {
                logRepository = SVNRepositoryFactory.create(SVNURL
                    .parseURIEncoded(logServer));
            } catch (SVNException e) {
                throw new CommunicationException(
                    CommunicationException.SubmissionPublicMessage.
                    INVALID_REPOSITORY_URL, e);
            }
            // do not call SVNWCUtil.createDefaultAuthenticationManager because
            // it will return a special authentication manager for eclipse which
            // will not work for our purposes
            logRepository.setAuthenticationManager(
                createAuthenticationManager());
        }
        String result = null;
        if (null != logRepository) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                logRepository.getFile(composePath(task, null == userName 
                    ? getUserName(true) : userName) + "/review.txt", -1, 
                    null, out);
                result = out.toString();
                out.close();
            } catch (SVNException e) {
                throw new CommunicationException(
                    CommunicationException.SubmissionPublicMessage.
                    FILE_IO_ERROR, e);
            } catch (IOException e) {
                // do nothing
            }
        }
        return result;
    }

}
