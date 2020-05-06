package de.uni_hildesheim.sse.exerciseSubmitter.submission.plugins;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDeltaProcessor;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;

import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ProgressListener;

/**
 * Realizes exporting submissions to local paths in the file system.
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.00
 */
class ExportEditor implements ISVNEditor {

    /**
     * Stores the (root) target directory.
     * 
     * @since 1.00
     */
    private File targetDirectory;

    /**
     * Stores the delta processor.
     * 
     * @since 1.00
     */
    private SVNDeltaProcessor deltaProcessor;

    /**
     * Stores the optional progress listener.
     * 
     * @since 1.00
     */
    private ProgressListener<ISubmission> listener;

    /**
     * The current export step number.
     * 
     * @since 1.00
     */
    private int count = 1;

    /**
     * Creates an export editor for the specified <code>root</code>
     * directory. A progress listener may be attached to visualize the
     * progress.
     * 
     * @param targetDirectory
     *            the target root directory
     * @param listener
     *            an optional progress listener, may be <b>null</b>
     * 
     * @since 1.00
     */
    public ExportEditor(File targetDirectory,
        ProgressListener<ISubmission> listener) {
        this.targetDirectory = targetDirectory;
        deltaProcessor = new SVNDeltaProcessor();
        this.listener = listener;
        if (null != listener) {
            count = listener.getStep();
        }
    }
    
    /**
     * Changes the target directory.
     * 
     * @param targetDirectory the new target directory
     * 
     * @since 2.00
     */
    public void setTargetDirectory(File targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    /**
     * Server reports revision to which application of the further
     * instructions will update working copy to.
     * 
     * @param revision
     *            a revision number
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @since 1.00
     */
    public void targetRevision(long revision) throws SVNException {
    }

    /**
     * Opens the root directory on which the operation was invoked. All
     * property changes as well as entries adding/deletion will be applied
     * to this root directory. When coming back up to this root (after
     * traversing the entire tree) you should close the root by calling
     * {@link #closeDir()}.
     * 
     * @param revision
     *            the revision number of the root directory
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @since 1.00
     */
    public void openRoot(long revision) throws SVNException {
    }

    /**
     * Called when a new directory has to be added.<br/> For each 'addDir'
     * call server will call 'closeDir' method after all children of the
     * added directory are added.<br/> This implementation creates
     * corresponding directory below root directory.
     * 
     * @param path
     *            a directory path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * @param copyFromPath
     *            an ancestor of the added directory
     * @param copyFromRevision
     *            the revision of the ancestor
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @since 1.00
     */
    public void addDir(String path, String copyFromPath,
        long copyFromRevision) throws SVNException {
        if (null != listener) {
            listener.notifyNextStep(path);
        }
        File newDir = new File(targetDirectory, path);
        if (!newDir.exists()) {
            if (!newDir.mkdirs()) {
                SVNErrorMessage err = SVNErrorMessage.create(
                    SVNErrorCode.IO_ERROR,
                    "error: failed to add the directory ''{0}''.", newDir);
                throw new SVNException(err);
            }
        }
        if (null != listener) {
            listener.processedStep(count++);
        }
    }

    /**
     * Called when there is an existing directory that has to be 'opened'
     * either to modify this directory properties or to process other files
     * and directories inside this directory.<br/> In case of export this
     * method will never be called because we reported that our 'working
     * copy' is empty and so server knows that there are no 'existing'
     * directories.
     * 
     * @param path
     *            a directory path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * @param revision
     *            the revision of the directory
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @since 1.00
     */
    public void openDir(String path, long revision) throws SVNException {
    }

    /**
     * Instructs to change opened or added directory property.<br/> This
     * method is called to update properties set by the user as well as
     * those created automatically, like "svn:committed-rev". See
     * SVNProperty class for default property names.
     * 
     * @param name
     *            the name of a property to be changed
     * @param value
     *            new property value, when a property should be deleted
     *            value must be 'null'.
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @see #openDir(String, long)
     * 
     * @since 1.00
     */
    public void changeDirProperty(String name, String value)
        throws SVNException {
    }

    /**
     * Called when a new file has to be created.<br/> For each 'addFile'
     * call server will call 'closeFile' method after sending file
     * properties and contents.<br/> This implementation creates empty file
     * below root directory, file contents will be updated later, and for
     * empty files may not be sent at all.
     * 
     * @param path
     *            a file path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * @param copyFromPath
     *            an ancestor of the added file
     * @param copyFromRevision
     *            the revision of the ancestor
     * 
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @since 1.00
     */
    public void addFile(String path, String copyFromPath,
        long copyFromRevision) throws SVNException {
        File file = new File(targetDirectory, path);
        if (file.exists()) {
/*                SVNErrorMessage err = SVNErrorMessage.create(
                    SVNErrorCode.IO_ERROR,
                    "error: exported file ''{0}'' already exists!", file);
                throw new SVNException(err);*/
            file.delete();
        }
        try {
            if (null != listener) {
                listener.notifyNextStep(path);
            }
            file.createNewFile();
            if (null != listener) {
                listener.processedStep(count++);
            }
        } catch (IOException e) {
            SVNErrorMessage err = SVNErrorMessage.create(
                SVNErrorCode.IO_ERROR,
                "error: cannot create new  file ''{0}''", file);
            throw new SVNException(err);
        }
    }

    /**
     * Called when there is an existing files that has to be 'opened' either
     * to modify file contents or properties.<br>
     * In case of export this method will never be called because we
     * reported that our 'working copy' is empty and so server knows that
     * there are no 'existing' files.
     * 
     * @param path
     *            a file path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * @param revision
     *            the revision of the file
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @since 1.00
     */
    public void openFile(String path, long revision) throws SVNException {
    }

    /**
     * Instructs to add, modify or delete file property. In this example we
     * skip this instruction, but 'real' export operation may inspect
     * 'svn:eol-style' or 'svn:mime-type' property values to transform file
     * contents properly after receiving.
     * 
     * @param path
     *            a file path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * @param name
     *            a file property name
     * @param value
     *            a new value for the property
     * @throws SVNException
     *             in the case of an internal SVN error
     * 
     * @since 1.00
     */
    public void changeFileProperty(String path, String name, String value)
        throws SVNException {
    }

    /**
     * Called before sending 'delta' for a file. Delta may include
     * instructions on how to create a file or how to modify existing file.
     * In this example delta will always contain instructions on how to
     * create a new file and so we set up deltaProcessor with 'null' base
     * file and target file to which we would like to store the result of
     * delta application.
     * 
     * @param path
     *            a file path relative to the edit root directory
     * @param baseChecksum
     *            an MD5 checksum for the base file contents (before the
     *            file is changed)
     * @throws SVNException
     *             if the calculated base file checksum didn't match the
     *             expected <code>baseChecksum</code>
     * 
     * @since 1.00
     */
    public void applyTextDelta(String path, String baseChecksum)
        throws SVNException {
        deltaProcessor.applyTextDelta((File) null,
            new File(targetDirectory, path), false);
    }

    /**
     * Server sends deltas in form of 'diff windows'. Depending on the file
     * size there may be several diff windows. Utility class
     * SVNDeltaProcessor processes these windows for us.<7BR> If there are
     * more than one window for the file, this method is called several
     * times.
     * 
     * @param path
     *            a file path relative to the edit root directory
     * @param diffWindow
     *            a next diff window
     * @return an output stream
     * 
     * @throws SVNException in the case of an erroneous repository 
     *            communication
     * 
     * @since 1.00
     */
    public OutputStream textDeltaChunk(String path, 
        SVNDiffWindow diffWindow) throws SVNException {
        return deltaProcessor.textDeltaChunk(diffWindow);
    }

    /**
     * Called when all diff windows (delta) is transferred.
     * 
     * @param path
     *            a file path relative to the edit root directory
     * 
     * @throws SVNException in the case of an erroneous repository 
     *         communication
     * 
     * @since 1.00
     */
    public void textDeltaEnd(String path) throws SVNException {
        deltaProcessor.textDeltaEnd();
    }

    /**
     * Called when file update is completed. This call always matches
     * addFile or openFile call.
     * 
     * @param path
     *            a file path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * @param textChecksum
     *            an MD5 checksum for the modified file
     * @throws SVNException
     *             if the calculated upon the actual changed contents
     *             checksum does not match the expected
     *             <code>textChecksum</code>
     * 
     * @since 1.00
     */
    public void closeFile(String path, String textChecksum)
        throws SVNException {
    }

    /**
     * Called when all child files and directories are processed. This call
     * always matches an {@link #addDir}, {@link #openDir} or
     * {@link #openRoot} call.
     * 
     * @throws SVNException
     *             if subversion specific errors occur
     * 
     * @since 1.00
     */
    public void closeDir() throws SVNException {
    }

    /**
     * Instructs to delete an entry in the 'working copy'. Of course will
     * not be called during export operation.
     * 
     * @param path
     *            an entry path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * @param revision
     *            the revision number of <code>path</code>
     * 
     * @throws SVNException
     *             if subversion specific errors occur
     * 
     * @since 1.00
     */
    public void deleteEntry(String path, long revision) 
        throws SVNException {
    }

    /**
     * Called when directory at 'path' should be somehow processed, but
     * authenticated user (or anonymous user) doesn't have enough access
     * rights to get information on this directory (properties, children).
     * 
     * @param path
     *            a dir path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * 
     * @throws SVNException
     *             if subversion specific errors occur
     * 
     * @since 1.00
     */
    public void absentDir(String path) throws SVNException {
    }

    /**
     * Called when file at 'path' should be somehow processed, but
     * authenticated user (or anonymous user) doesn't have enough access
     * rights to get information on this file (contents, properties).
     * 
     * @param path
     *            a file path relative to the root directory opened by
     *            {@link #openRoot(long) openRoot()}
     * 
     * @throws SVNException
     *             if subversion specific errors occur
     * 
     * @since 1.00
     */
    public void absentFile(String path) throws SVNException {
    }

    /**
     * Called when update is completed.
     * 
     * @return information on the commit operation (may be <b>null</b>)
     * 
     * @throws SVNException
     *             if subversion specific errors occur
     * 
     * @since 1.00
     */
    public SVNCommitInfo closeEdit() throws SVNException {
        return null;
    }

    /**
     * Called when update is completed with an error or server requests
     * client to abort update operation.
     * 
     * @throws SVNException
     *             if subversion specific errors occur
     * 
     * @since 1.00
     */
    public void abortEdit() throws SVNException {
    }

    @Override
    public void changeDirProperty(String arg0, SVNPropertyValue arg1)
        throws SVNException {
        // TODO Auto-generated method stub
    }

    @Override
    public void changeFileProperty(String arg0, String arg1,
        SVNPropertyValue arg2) throws SVNException {
        // TODO Auto-generated method stub
    }
}