package de.uni_hildesheim.sse.exerciseSubmitter.submission.plugins;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils.DialogType;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.AbstractExecutable;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.CommonStuff;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Executable;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.FileChecksumUtil;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IPathFactory;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IVersionedSubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ProgressListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.SubmissionDirEntry;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.SubmissionPlugin;
import net.ssehub.exercisesubmitter.protocol.backend.NetworkException;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;
import net.ssehub.exercisesubmitter.protocol.frontend.SubmissionTarget;

/**
 * Realizes a specific server communication for subversion (SVN) per https and webdav.
 * This class uses classes from the svnkit.
 * 
 * @author Alexander Schmehl (initial)
 * @since 1.00
 * @version 2.10
 */
public class SvnSubmissionCommunication extends SubmissionCommunication {

    /**
     * Initializes svnkit.
     * 
     * @since 2.00
     */
    static {
        DAVRepositoryFactory.setup();
    }
    
    /**
     * Defines the associated plugin instance.
     * 
     * @since 2.00
     */
    public static final SubmissionPlugin PLUGIN = new SubmissionPlugin() {

        /**
         * Creates an instance of the described submission communication class.
         * 
         * @param username the name of the user which will communicate with a concrete communication server
         * @param password the password of <code>username</code>
         * @param asReviewer initialize this instance in reviewer mode. This may show additional exercises to be
         *        submitted but finally the access permissions on the server should prevent from misuse
         * @return the created instance
         * 
         * @since 2.00
         */
        @Override
        public SubmissionCommunication createInstance(String userName, String password, boolean asReviewer) {
            
            return new SvnSubmissionCommunication(userName, password, asReviewer);
        }

        /**
         * Returns the name of the protocol implemented by this class. The string will be considered when reading the
         * communication based submission configuration. <i>Note:</i> Currently no mechanism for avoiding
         * duplicate protocol names is realized.
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
    private static final Format SUBMISSION_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    /**
     * Defines the commit mode. If <code>true</code>, the repository is cleared by deleting all files in the directory
     * before committing the new resources. Otherwise, incremental changes are identified and sent.
     * 
     * @since 2.00
     */
    private static final boolean DELETE_COMMITED_RESOURCES_BEFORE_COMMIT = false;
    
    /**
     * Stores the server including protocol, hostname, repository and basic path.
     * 
     * @since 1.00
     */
    private String server;

    /**
     * Stores the optional log server including protocol, hostname, repository  and basic path.
     * 
     * @since 1.00
     */
    private String logServer;

    /**
     * Stores the tasks/exercises available for submission.
     * 
     * @since 1.00
     */
    private List<Assignment> availableForSubmission = new ArrayList<>();

    /**
     * Stores the tasks/exercises available for replay.
     * 
     * @since 1.00
     */
    private List<Assignment> availableForReplay = new ArrayList<>();

    /**
     * Stores the tasks/exercises available for review.
     * 
     * @since 2.00
     */
    private List<Assignment> availableForReview = new ArrayList<>();
    
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
     * Stores the authentication manager used to authenticate the user against the repository on {@link #server}.
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
     * @param username the nmme of the user which will communicate with a concrete communication server
     * @param password the password of <code>username</code>
     * @param asReviewer initialize this instance in reviewer mode. This may show additional exercises to be submitted
     *        but finally the access permissions on the server should prevent from misuse
     * 
     * @since 2.10
     */
    private SvnSubmissionCommunication(String username, String password, boolean asReviewer) {
        super(username, password);

        this.asReviewer = asReviewer;
        // do self-configuration
        this.server = normalizeServerName(IConfiguration.INSTANCE.getProperty("svn.server"));
        this.logServer = normalizeServerName(IConfiguration.INSTANCE.getProperty("svn.log.server"));
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
            if (Boolean.valueOf(IConfiguration.INSTANCE.getProperty("svn.https_only"))) {
                if (!server.startsWith("https://")) {
                    server = "https://" + server;
                }                
            } else {
                if (!server.startsWith("https://") && !server.startsWith("http://")) {
                    server = "https://" + server;
                } 
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
     * Returns if this communication instance allows the replay of (reviewed) submissions.
     * 
     * @return <code>true</code> if the communication instance/protocol allows the replay of submissions,
     * <code>false</code> else
     * 
     * @since 2.00
     */
    public boolean allowsReplay() {
        return true;
    }

    /**
     * Authenticates the user by his/her stored data. This method calls {@link #reInitialize}.
     * 
     * @return <code>true</code>, if the user was authenticated, <code>false</code> else
     * @throws CommunicationException
     *             in the case of any (protocol specific) error
     * 
     * @since 1.00
     */
    public boolean authenticateUser() throws CommunicationException {
        try {
            getStudentMgmtProtocol().login(getUserName(false), getPassword());
        } catch (NetworkException e) {
            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER, e);
        }
        if (null != repository) {
            repository.closeSession();
        }
        try {
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(server));
        } catch (SVNException e) {
            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.INVALID_REPOSITORY_URL, e);
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
            authManager = new DefaultSVNAuthenticationManager(null, true, getUserName(false),
                getPassword().toCharArray(), null, null);
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
    public void reInitialize() throws CommunicationException {
        try {
            availableForSubmission = getStudentMgmtProtocol().getOpenAssignments();
            availableForReplay = getStudentMgmtProtocol().getReviewableAssignments();
            availableForReview = getStudentMgmtProtocol().getReviewedAssignments();
            if (asReviewer) {
                availableForSubmission.addAll(availableForReview);
            }
            availableForSubmission.sort((a1, a2) -> a1.getName().compareTo(a2.getName()));
            availableForReplay.sort((a1, a2)     -> a1.getName().compareTo(a2.getName()));
            availableForReview.sort((a1, a2)     -> a1.getName().compareTo(a2.getName()));
        } catch (NetworkException e) {
            GuiUtils.openDialog(DialogType.ERROR, "Could not query Studenten Management System to retrieve list of "
                + "open assignments.");
        }
    }
          // Belongs to reInitialize()
//        Collection<?> entries;
//
//        // retrieve main repository directory contents
//        try {
//            entries = repository.getDir("", -1, null, (Collection<?>) null);
//        } catch (SVNAuthenticationException e) {
//            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.INVALID_USER_PASSWORD, e);
//        } catch (SVNException e) {
//            throw new CommunicationException(CommunicationException.
//                SubmissionPublicMessage.INVALID_REPOSITORY_STRUCTURE, e);
//        }
//
//        // select and store directory entries
//        Iterator<?> iterator = entries.iterator();
//        ArrayList<SVNDirEntry> dirs = new ArrayList<SVNDirEntry>();
//        while (iterator.hasNext()) {
//            SVNDirEntry entry = (SVNDirEntry) iterator.next();
//            if (entry.getKind() == SVNNodeKind.DIR) {
//                dirs.add(entry);
//            }
//        }
//
//        Map<String, PermissionMode> permissions = readPermissions();
//        for (SVNDirEntry dir : dirs) {
//            boolean done = false;
//            if (null != permissions) {
//                String absPath = dir.getName();
//                if (!absPath.startsWith("/")) {
//                    absPath = "/" + absPath;
//                }
//                PermissionMode mode = permissions.get(absPath);
//                if (null != mode) {
//                    done = true;
//                    switch (mode) {
//                    case REPLAY:
//                        availableForReplay.add(dir.getName());
//                        break;
//                    case REVIEW:
//                        availableForReview.add(dir.getName());
//                        if (asReviewer) {
//                            availableForSubmission.add(dir.getName());
//                        }
//                        break;
//                    case SUBMISSION:
//                        availableForSubmission.add(dir.getName());
//                        break;
//                    case INVISIBLE:
//                        break;
//                    default:
//                        done = false;
//                        break;
//                    }
//                }
//            } 
//            if (!done) {
//                try {
//                    if (isRepositoryWritable(server + dir.getName() + "/" + getTargetFolder())) {
//                        availableForSubmission.add(dir.getName());
//                    } else {
//                        repository.getDir(dir.getName(), -1, null, (Collection<?>) null);
//                        availableForReplay.add(dir.getName());
//                        availableForReview.add(dir.getName());
//                    }
//                } catch (SVNException e) {
//                }
//            }
//        }
    /**
     * Returns the top-level path/task/exercise names of exercises that can
     * currently be reviewed.
     * 
     * @return the top-level path/task/exercise names of exercises that can currently be reviewed
     * 
     * @since 2.00
     */
    public List<Assignment> getSubmissionsForReview() {
        return availableForReview;
    }

    @Override
    public List<IVersionedSubmission> getSubmissionsForReplay(Assignment assignment) throws CommunicationException {
        List<IVersionedSubmission> result = new ArrayList<IVersionedSubmission>();
        try {
            SubmissionTarget dest = getStudentMgmtProtocol().getPathToSubmission(assignment);
            if (availableForSubmission.contains(assignment) || asReviewer) {
                String[] targetPaths = new String[] {dest.getAbsolutePathInRepository()};
                Collection<?> revisions = repository.log(targetPaths, null, 0, repository.getLatestRevision(), false,
                    false);
                for (Object o : revisions) {
                    SVNLogEntry entry = (SVNLogEntry) o;
                    result.add(new SVNSubmission(entry.getDate(), entry.getRevision(), entry.getAuthor(), dest));
                }
            } else {
                Collection<SVNDirEntry> dirs = getDirs(repository, -1, dest.getSubmissionPath(), null);
                if (dirs.size() > 0) {
                    SVNSubmission resultSubmission = new SVNSubmission(new Date(), -1, "", dest);
                    resultSubmission.setDate("reviewed");
                    result.add(resultSubmission);
                }
            }
        } catch (SVNException e) {
            throw new CommunicationException(CommunicationException.
                SubmissionPublicMessage.PROBLEM_PREVIOUS_SUBMISSIONS, e);
        } catch (NetworkException e) {
            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER, e);
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
         * Stores the path of the associated task/exercise.
         * 
         * @since 2.00
         */
        private SubmissionTarget remoteDestination;
        
        /**
         * Stores the name of the user responsible for this revision.
         * 
         * @since 2.00
         */
        private String user;

        /**
         * Creates a new versioned submission instance.
         * 
         * @param date The date of the version
         * @param revision The SVN revision number
         * @param user The user, who submitted this revision
         * @param remoteDestination The path of the associated task/exercise.
         * 
         * @since 2.00
         */
        SVNSubmission(Date date, long revision, String user, SubmissionTarget remoteDestination) {
            this.date = SUBMISSION_FORMAT.format(date);
            this.revision = revision;
            this.remoteDestination = remoteDestination;
            this.user = user;
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
         * Returns the location of the submitted assignment..
         * 
         * @return the top-level path
         * 
         * @since 2.00
         */
        public SubmissionTarget getRemotePath() {
            return remoteDestination;
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

        @Override
        public String getAuthor() {
            return user;
        }
    }

    /**
     * Returns the user names (second-level directories).
     * 
     * @return the second-level user names
     * 
     * @since 2.00
     */
    public List<String> getUserNames() {
        List<String> result = new ArrayList<String>();
        Assignment topLevelDir = null;
        if (!availableForSubmission.isEmpty()) {
            topLevelDir = availableForSubmission.get(0);
        } else if (!availableForReplay.isEmpty()) {
            topLevelDir = availableForReplay.get(0);
        }
        if (null != topLevelDir) {
            try {
                Collection<SVNDirEntry> entries = repository.getDir(topLevelDir + "/", -1, null,
                    SVNDirEntry.DIRENT_ALL, (Collection<?>) null);
                for (SVNDirEntry entry : entries) {
                    result.add(entry.getName());
                }
            } catch (SVNException e) {
            }
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public Executable<ISubmission> submit(ISubmission submission, Assignment assignment) throws CommunicationException {
        return new SVNCommExecutable(submission, assignment);
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
    private static Collection<SVNDirEntry> getDirs(SVNRepository repository, long revision, String dir,
        Collection<SVNDirEntry> collection) throws SVNException {
        
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
    private static Collection<SVNDirEntry> getDirs(SVNRepository repository, long revision, String dir,
        String prefixDir, Collection<SVNDirEntry> collection) throws SVNException {
        
        if (null == collection) {
            collection = new ArrayList<SVNDirEntry>();
        }
        Collection<SVNDirEntry> c = repository.getDir(dir, revision, null, SVNDirEntry.DIRENT_ALL,
            (Collection<?>) null);
        for (SVNDirEntry entry : c) {
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
        String parent;
        int pos = path.lastIndexOf("/");
        if (pos == 0) {
            parent = "/";
        } else if (pos > 0) {
            parent = path.substring(0, pos);
        } else {
            parent = "";
        }
        
        return parent;
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
        private FileInfo(char modus, SVNNodeKind kind, File file, String svnPath) {
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
                        if (kind == SVNNodeKind.DIR && !currentPath.isEmpty() && currentPath.peek().equals(name)) {
                            editor.closeDir();
                            currentPath.pop();
                        }
                        editor.deleteEntry(name, -1);
                    } else if (kind == SVNNodeKind.DIR && modus == SVNChangeEntry.TYPE_ADDED) {
                        editor.addDir(name, null, -1);
                        currentPath.push(name);
                    } else {
                        if (modus == SVNChangeEntry.TYPE_ADDED) {
                            editor.addFile(name, null, -1);
                        } else {
                            editor.openFile(name, -1);
                        }
                        editor.applyTextDelta(name, null);
                        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
                        try {
                            BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
                            String checksum = deltaGenerator.sendDelta(name, is, editor, true);
                            editor.closeFile(name, checksum);
                            try {
                                is.close();
                            } catch (IOException ioe) {
                            }
                        } catch (FileNotFoundException e) {
                            // diese Exception fangen wir ab; sie sollte nie
                            // vorkommen, und auch nach einer solchen, sollte
                            // wenigstens versucht werden, den rest zu comitten
                            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
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
         * @param submission The submission to be committed
         * @param assignment The exercise/task top-level path
         * @throws CommunicationExceptio If any exception occurred while submitting
         * 
         * @since 2.00
         */
        private SVNCommExecutable(ISubmission submission, Assignment assignment) throws CommunicationException {
            super(submission, assignment);
            if (availableForSubmission.contains(assignment)) {
                submitDir = submission.getPath();
                numberOfSteps = 1 + 1; // checkIn will follow
                try {
                    SubmissionTarget destination = getStudentMgmtProtocol().getPathToSubmission(assignment);
                    String svnPath = destination.getAbsolutePathInRepository();
                    Collection<SVNDirEntry> dirs = getDirs(repository, -1, svnPath, null);
                    numberOfCheckoutSteps = dirs.size();
                    numberOfSteps += numberOfCheckoutSteps;
                } catch (SVNException e) {
                    throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                        ERROR_READING_REPOSITORY_DIRECTORY_STRUCTURE, e);
                } catch (NetworkException e) {
                    throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                        UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER, e);
                }
            } else {
                numberOfSteps = 0;
                throw new CommunicationException(CommunicationException.SubmissionPublicMessage.INVALID_SUBMISSION,
                    new Throwable());
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
        private void openTransaction() throws SVNException, CommunicationException {
            DAVRepositoryFactory.setup();
            SubmissionTarget destination;
            try {
                destination = getStudentMgmtProtocol().getPathToSubmission(getAssignment());
            } catch (NetworkException e) {
                throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                    UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER, e);
            }
            
            repo = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(destination.getSubmissionURL()));
            repo.setAuthenticationManager(createAuthenticationManager());
            handleCommitedResources(destination);
            if (checkIn.size() > 0) {
                editor = repo.getCommitEditor("Submission of " + getAssignment().getName() + " by "
                    + getUserName(false), null);
                editor.openRoot(-1);
            }
            if (numberOfCheckoutSteps > 0) {
                setStep(numberOfCheckoutSteps + 1);
            }
        }
        
        /**
         * Closes a commit transaction. 
         * 
         * @throws SVNException any exception while communicating with subversion
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
                getListener().notifyNextStep("Waiting for server results...");
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
                        getSubmission().setResult(ISubmission.Result.POST_FAILED);
                    } else {
                        if (msg.length() > 0) {
                            msg = msg + "\n";
                        }
                        msg = msg + message.getFullMessage();
                        message = info.getErrorMessage();
                        getSubmission().setMessage(msg);
                        getSubmission().setResult(ISubmission.Result.SUCCESSFUL);
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
         * Is called to handle the resources to be committed. This includes the handling of resources which have been
         * committed to the repository so far. The behavior of this method depends on
         * {@link #DELETE_COMMITED_RESOURCES_BEFORE_COMMIT}.
         * 
         * @param destination Target location of the assignment to submit
         * @throws SVNException Is thrown in the case of a communication error with the repository
         * @throws CommunicationException Occurs in the case of a wrapped (communication) exception
         * 
         * @since 2.00
         */
        private void handleCommitedResources(SubmissionTarget destination) throws SVNException,
            CommunicationException {
            try {
                if (DELETE_COMMITED_RESOURCES_BEFORE_COMMIT) {
                    final String trgFolder = destination.getAbsolutePathInRepository();
                    Collection<SVNDirEntry> contents = getDirs(repository, repository.getLatestRevision(), trgFolder,
                        null);
                    for (Iterator<SVNDirEntry> iterator = contents.iterator(); 
                        iterator.hasNext();) {
                        SVNDirEntry entry = iterator.next();
                        checkIn.add(new FileInfo(SVNChangeEntry.TYPE_DELETED, entry.getKind(), null,
                            trgFolder + "/" + entry.getRelativePath()));
                    }
                    enumElementsToAdd(submitDir.getAbsolutePath(), submitDir, checkIn);
                } else {
                    preCommitTmpDir = CommonStuff.createTmpDir();
                    svnExport(destination, preCommitTmpDir.getAbsolutePath(), repository.getLatestRevision(),
                        getListener());

                    Map<String, Object> toBeDeleted = new HashMap<String, Object>();
                    enumElements(preCommitTmpDir.getAbsolutePath(), preCommitTmpDir, toBeDeleted, true);
                    enumElements(submitDir.getAbsolutePath(), submitDir, toBeDeleted, false);
                    deleteElements(preCommitTmpDir.getAbsolutePath(), preCommitTmpDir, toBeDeleted, checkIn);
                    physicallyDeleteFiles(checkIn);
                    copyFiles(submitDir, submitDir.getAbsolutePath(), preCommitTmpDir, checkIn);

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
                throw new CommunicationException(CommunicationException.SubmissionPublicMessage.FILE_IO_ERROR, ioe);
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
        private void copyFiles(File source, String prefix, File target, List<FileInfo> elements)
            throws CommunicationException {
            
            File[] filelist = source.listFiles();
            if (null != filelist) {
                for (File file : filelist) {
                    String svnPath = cutPrefix(prefix, file.getAbsolutePath());
                    File tFile = new File(target.getAbsolutePath() + File.separator + svnPath);
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
                                    elements.add(new FileInfo(SVNChangeEntry.TYPE_ADDED, SVNNodeKind.DIR, tf1,
                                        cutPrefix(target.getAbsolutePath() + File.separator, tf1.getAbsolutePath())));
                                }
                            } while (pos >= 0);
                        }
                        copyFiles(file, prefix, target, elements);
                    } else {
                        if (tFile.exists()) {
                            if (!checksumUtil.equalByCheckSum(file, tFile)) {
                                elements.add(new FileInfo(SVNChangeEntry.TYPE_UPDATED, SVNNodeKind.FILE, tFile,
                                    svnPath));
                            }
                        } else {
                            elements.add(new FileInfo(SVNChangeEntry.TYPE_ADDED, SVNNodeKind.FILE, tFile, svnPath));
                        }
                        try (FileChannel sourceChannel = new FileInputStream(file).getChannel();
                             FileChannel targetChannel = new FileOutputStream(tFile).getChannel()) {
                            
                            targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                            sourceChannel.close();
                            targetChannel.close();
                        } catch (IOException ioe) {
                            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
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
            return (path.startsWith(prefix)) ? path.substring(prefix.length(), path.length()) : path;
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

    @Override
    public List<Assignment> getAvailableForSubmission() {
        return availableForSubmission;
    }
    
    @Override
    public List<Assignment> getSubmissionsForReplay() {
        return availableForReplay;
    }

    @Override
    public ISubmission replaySubmission(ISubmission submission, Assignment assignment,
        ProgressListener<ISubmission> listener) throws CommunicationException {
        
        try {
            SubmissionTarget destination = getStudentMgmtProtocol().getPathToSubmission(assignment);
            Collection<SVNDirEntry> contents = getDirs(repository, repository.getLatestRevision(),
                    destination.getAbsolutePathInRepository(), null);
            if (null != listener) {
                listener.numberofStepsChanged(contents.size());
            }
            if (!contents.isEmpty()) {
                CommonStuff.rmdir(submission.getPath(), false);
                
                if (availableForReplay.contains(assignment)) {
                    svnExport(destination, submission.getPath().getAbsolutePath(),
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
            throw new CommunicationException(CommunicationException.SubmissionPublicMessage
                .PROBLEM_PREVIOUS_SUBMISSIONS, e);
        } catch (NetworkException e) {
            throw new CommunicationException(CommunicationException.SubmissionPublicMessage
                .UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER, e);
        }
    }

    @Override
    public void replayEntireTask(File path, Assignment assignment, ProgressListener<ISubmission> listener, 
        IPathFactory factory) throws CommunicationException {
        
        if (null == factory) {
            factory = this;
        }

        SubmissionTarget destFolder = null;
        try {
            destFolder = getStudentMgmtProtocol().getPathToSubmission(assignment);
        } catch (NetworkException e1) {
            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER, e1);
        }
        SVNException thrownException = null;
        try {
            List<SVNDirEntry> skipDirs = new ArrayList<>();
            Collection<SVNDirEntry> dirs = repository.getDir(destFolder.getAssignmentName(),
                repository.getLatestRevision(), null, SVNDirEntry.DIRENT_ALL, (Collection<?>) null);
            if (null != listener) {
                for (SVNDirEntry entry : dirs) {
                    if (entry.getKind() == SVNNodeKind.DIR) {
                        Collection<SVNDirEntry> contents = getDirs(repository, repository.getLatestRevision(),
                            destFolder.getAssignmentName() + "/" + entry.getRelativePath(), null);
                        if (contents.isEmpty()) {
                            skipDirs.add(entry);
                        }
                    }
                }
            }
            for (Object skip : skipDirs) {
                dirs.remove(skip);
            }

            ExportEditor exportEditor = new ExportEditor(null, listener);
            for (SVNDirEntry entry : dirs) {
                if (entry.getKind() == SVNNodeKind.DIR) {
                    File target = factory.createPath(path, entry.getName());
                    CommonStuff.rmdir(target, false);
                    exportEditor.setTargetDirectory(target);
                    try {
                        svnExport(destFolder.getAssignmentName() + "/" + entry.getRelativePath(),
                            repository.getLatestRevision(), exportEditor);
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
    
    @Override
    public List<SubmissionDirEntry> getLastContents(Assignment assignment) 
        throws CommunicationException {
        List<SubmissionDirEntry> result = new ArrayList<SubmissionDirEntry>();
        try {
            String svnPath = getStudentMgmtProtocol().getPathToSubmission(assignment).getAbsolutePathInRepository();
            Collection<SVNDirEntry> contents = getDirs(repository, repository.getLatestRevision(), svnPath, null);
            for (SVNDirEntry entry : contents) {
                SubmissionDirEntry newEnt = new SubmissionDirEntry(entry.getRelativePath(), entry.getSize(),
                    entry.getDate(), SVNNodeKind.DIR == entry.getKind(), entry.getAuthor());
                result.add(newEnt);
            }
        } catch (SVNException e) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.PROBLEM_PREVIOUS_SUBMISSIONS, e);
        } catch (NetworkException e) {
            throw new CommunicationException(
                CommunicationException.SubmissionPublicMessage.UNABLE_TO_CONTACT_STUDENT_MANAGEMENT_SERVER, e);
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
    public ISubmission replaySubmission(ISubmission submission, IVersionedSubmission version,
        ProgressListener<ISubmission> listener) throws CommunicationException {
        
        if (!(version instanceof IVersionedSubmission)) {
            throw new IllegalArgumentException();
        }
        
        try {
            SVNSubmission subm = (SVNSubmission) version;
            SubmissionTarget dest = subm.getRemotePath();
            Collection<SVNDirEntry> contents = getDirs(repository, subm.getRevision(),
                dest.getAbsolutePathInRepository(), null);
            if (null != listener) {
                listener.numberofStepsChanged(contents.size());
            }
            if (!contents.isEmpty()) {
                CommonStuff.rmdir(submission.getPath(), false);
                
                svnExport(dest, submission.getPath().getAbsolutePath(), subm.getRevision(), listener);
                adjustFilesAfterReplay(submission.getPath());
                
                if (null != listener) {
                    listener.finished(true, submission);
                }
                submission.setResult(ISubmission.Result.SUCCESSFUL);
            }
            return submission;
        } catch (SVNException e) {
            throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                PROBLEM_PREVIOUS_SUBMISSIONS, e);
        }
    }

    /**
     * Exports a SVN repository or a sub path to a given directory.
     * 
     * @param destination The path in/to the repository to be exported
     * @param targetPath The file system path where to export the repository contents to
     * @param revision The revision to be exported
     * @param listener an optional progress listener to make the progress visible
     * @throws SVNException A wrapping exception in the case of (communication) errors
     * 
     * @since 1.00
     */
    private void svnExport(SubmissionTarget destination, String targetPath, long revision,
        ProgressListener<ISubmission> listener) throws SVNException {
        
        ExportEditor exportEditor = new ExportEditor(new File(targetPath), listener);
        svnExport(destination, revision, exportEditor, true);
    }

    /**
     * Exports a SVN repository or a sub path to a given directory.
     * 
     * @param destination The path in/to the repository to be exported
     * @param revision The revision to be exported
     * @param exportEditor A (reusable) export editor object
     * @param userSpecific <tt>true</tt>Creates a user specific destination, <tt>false</tt> creates a reviewer location
     *     that provides an URL to export all submissions.
     * @throws SVNException A wrapping exception in the case of (communication) errors
     * 
     * @since 1.00
     */
    private void svnExport(SubmissionTarget destination, long revision, ExportEditor exportEditor, boolean userSpecific)
        throws SVNException {
        
        String url = userSpecific ? destination.getSubmissionURL() : destination.getAllSubmissionsURL();
        svnExport(url, revision, exportEditor);
    }
    
    /**
     * Exports a SVN repository or a sub path to a given directory.
     * 
     * @param url The URL (absolute path) to a folder to export
     * @param revision The revision to be exported
     * @param exportEditor A (reusable) export editor object

     * @throws SVNException A wrapping exception in the case of (communication) errors
     * 
     * @since 2.1
     */
    private void svnExport(String url, long revision, ExportEditor exportEditor) throws SVNException {
        
        SVNRepository tmprepo = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
        tmprepo.setAuthenticationManager(createAuthenticationManager());
        tmprepo.checkout(revision, null, true, exportEditor);
        tmprepo.closeSession();
    }
    
//    /**
//     * Returns if a repository is writable. Therefore, a file of an arbitrary name is added to a temporary instance of
//     * the repository. This operation does not change the repository because it is not really commited.
//     * 
//     * @param repositoryURL complete SVN access URL
//     * @return <code>true</code> if <code>repositoryURL</code> is writable, <code>false</code> else
//     * 
//     * @throws SVNException in the case of an erroneous repository communication
//     * 
//     * @since 1.00
//     */
//    private boolean isRepositoryWritable(String repositoryURL)
//        throws SVNException {
//        SVNRepository tmprepository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(repositoryURL));
//        tmprepository.setAuthenticationManager(createAuthenticationManager());
//        ISVNEditor editor = tmprepository.getCommitEditor("writing test (to be ignored)", null);
//        boolean result = false;
//        try {
//            editor.openRoot(-1);
//            editor.addFile(".schreibtest", null, -1);
//            editor.abortEdit();
//            result = true;
//        } catch (SVNException e) {
//            try {
//                editor.abortEdit();
//            } catch (SVNException e1) {
//            }
//        }
//        tmprepository.closeSession();
//        return result;
//    }

    @Override
    public String getSubmissionLog(String task, String userName) 
        throws CommunicationException {
        if (null == logRepository && null != logServer && logServer.length() > 0) {
            try {
                logRepository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(logServer));
            } catch (SVNException e) {
                throw new CommunicationException(CommunicationException.SubmissionPublicMessage.
                    INVALID_REPOSITORY_URL, e);
            }
            // do not call SVNWCUtil.createDefaultAuthenticationManager because
            // it will return a special authentication manager for eclipse which
            // will not work for our purposes
            logRepository.setAuthenticationManager(createAuthenticationManager());
        }
        String result = null;
        if (null != logRepository) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                logRepository.getFile(composePath(task, 
                    null == userName ? getUserName(true) : userName) + "/review.txt", -1, null, out);
                result = out.toString();
                out.close();
            } catch (SVNException e) {
                throw new CommunicationException(CommunicationException.SubmissionPublicMessage.FILE_IO_ERROR, e);
            } catch (IOException e) {
                // do nothing
            }
        }
        return result;
    }

}
