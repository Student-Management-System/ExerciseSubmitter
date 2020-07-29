package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.progress.IProgressService;

import de.uni_hildesheim.sse.exerciseSubmitter.Activator;
import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions.MessageListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Executable;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IPathFactory;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IVersionedSubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ProgressListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Submission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;

/**
 * Implements some utilities for Eclipse GUI handling.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.10
 */
public class GuiUtils {

    /**
     * Stores if submission success should be displayed.
     * 
     * @since 2.00
     */
    private static boolean showSubmSuccess = true;
    
    /**
     * Prevents this class from being instantiated from outside.
     * 
     * @since 2.00
     */
    private GuiUtils() {
    }
    
    /**
     * Handles a throwable by opening a message window.
     * 
     * @param throwable
     *            the throwable to be handled
     * 
     * @since 2.00
     */
    public static void handleThrowable(Throwable throwable) {
        if (IConfiguration.INSTANCE.isDebuggingEnabled()) {
            Activator.log("ExerciseSubmitter", throwable);
        }
        String message;
        if (throwable instanceof CommunicationException) {
            message = ((CommunicationException) throwable).getMessage();
        } else {
            message = throwable.getMessage();
        }
        openDialog(DialogType.ERROR, message);
    }

    /**
     * Defines the type of a dialog to be opened by {@link #openDialog}.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    public enum DialogType {
        /**
         * Defines an error dialog type.
         * 
         * @since 2.00
         */
        ERROR,

        /**
         * Defines an information dialog type.
         * 
         * @since 2.00
         */
        INFORMATION,

        /**
         * Defines a confirmation dialog type.
         * 
         * @since 2.00
         */
        CONFIRMATION
    }

    /**
     * Opens a SWT dialog.
     * 
     * @param type
     *            the type of the dialog
     * @param msg
     *            the message to be emitted
     * 
     * @return the result of the dialog in the case of
     *         {@link DialogType#CONFIRMATION}, <code>false</code> else
     */
    public static boolean openDialog(DialogType type, String msg) {
        DialogRunnable runnable = new DialogRunnable(msg, type);
        Display.getDefault().syncExec(runnable);
        return runnable.getResult();
    }

    /**
     * Implements a runnable to run a SWT dialog inside. A runnable is needed
     * due to the thread model of SWT.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private static class DialogRunnable implements Runnable {

        /**
         * Stores the message to be emitted.
         * 
         * @since 2.00
         */
        private String msg;

        /**
         * Stores the dialog type to be opened.
         * 
         * @since 2.00
         */
        private DialogType type;

        /**
         * Stores the result of the dialog.
         * 
         * @since 2.00
         */
        private boolean result;

        /**
         * Creates a new runnable for the specified message and the given dialog
         * type.
         * 
         * @param msg
         *            the message to be emitted
         * @param type
         *            the type of the dialog
         * 
         * @since 2.00
         */
        public DialogRunnable(String msg, DialogType type) {
            this.msg = msg;
            this.type = type;
        }
        
        /**
         * Returns the result of the dialog.
         * 
         * @return the result of the dialog in the case of
         *         {@link DialogType#CONFIRMATION}, <code>false</code> else
         * 
         * @since 2.00
         */
        public boolean getResult() {
            return result;
        }

        /**
         * Opens the dialog.
         * 
         * @since 2.00
         */
        public void run() {
            switch (type) {
            case ERROR:
                MessageDialog.openError(new Shell(), "Exercise submitter", msg);
                break;
            case INFORMATION:
                MessageDialog.openInformation(new Shell(),
                    "Exercise submitter", msg);
                break;
            case CONFIRMATION:
                result = MessageDialog.openConfirm(new Shell(),
                    "Exercise submitter", msg);
                break;
            default:
                break;
            }
        }

    }

    /**
     * Replays the specified <code>replaySubmisson</code> from the repository
     * to <code>submission</code>.
     * 
     * @param name
     *            the name to be displayed in the progress window
     * @param comm
     *            the submission communication object
     * @param submission
     *            the submission object
     * @param replaySubmission
     *            the object representing the version/date to be replayed
     * @param schedulingRule the scheduling rule/resource to be locked
     *        while execution, may be <code>ResourcesPlugin
     *        .getWorkspace().getRoot()</code>, <b>null</b> or a more
     *        specific (resource) instance
     * 
     * @since 2.00
     */
    public static void runReplay(final String name,
        final SubmissionCommunication comm, final ISubmission submission,
        final IVersionedSubmission replaySubmission, 
        ISchedulingRule schedulingRule) {

        /**
         * Implements a <i>long-running operation</i> to replay a submission with a progressbar.
         *
         */
        class RunnableWithProgress implements IRunnableWithProgress {

            private Exception exception;

            /**
             * Returns the received exception.
             * 
             * @return the received exception, may be <b>null</b>
             */
            public Exception getException() {
                return exception;
            }

            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {

                ExecutableMonitorListener<ISubmission> listener = 
                    new ExecutableMonitorListener<ISubmission>(name, monitor);
                try {
                    comm.replaySubmission(submission, replaySubmission, listener);
                } catch (CommunicationException e) {
                    exception = e;
                }
                if (null == exception) {
                    exception = listener.getException();
                }
            }
        }

        RunnableWithProgress runnable = new RunnableWithProgress();
        runRunnable(runnable, schedulingRule);
        if (null != runnable.getException()) {
            GuiUtils.handleThrowable(runnable.getException());
        }
    }
    
    /**
     * Runs a given runnable (currently with considered locking of the UI).
     * 
     * @param runnable the object to run
     * @param schedulingRule the scheduling rule/resource to be locked
     *        while execution, may be <code>ResourcesPlugin
     *        .getWorkspace().getRoot()</code>, <b>null</b> or a more
     *        specific (resource) instance
     * 
     * @since 2.00
     */
    private static void runRunnable(IRunnableWithProgress runnable, 
        ISchedulingRule schedulingRule) {
        try {
            IProgressService progressService = PlatformUI.getWorkbench()
                .getProgressService();
            schedulingRule = ResourcesPlugin.getWorkspace().getRoot();
            //progressService.runInUI(progressService, runnable, 
            //schedulingRule);
            progressService.run(true, false, runnable);
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
        }
    }

    /**
     * Replays the specified <code>assignment</code> from the repository to <code>submission</code>.
     * 
     * @param name
     *            the name to be displayed in the progress window
     * @param comm
     *            the submission communication object
     * @param submission
     *            the submission object
     * @param assignment The task/exercise to be replayed
     * @param schedulingRule the scheduling rule/resource to be locked
     *        while execution, may be <code>ResourcesPlugin
     *        .getWorkspace().getRoot()</code>, <b>null</b> or a more
     *        specific (resource) instance
     * 
     * @since 2.00
     */
    public static void runReplay(final String name, final SubmissionCommunication comm, final ISubmission submission,
        final Assignment assignment, ISchedulingRule schedulingRule) {

        /**
         * Implements a <i>long-running operation</i> to <b>replay</b> a submission with a progressbar.
         *
         */
        class RunnableWithProgress implements IRunnableWithProgress {

            private Exception exception;

            /**
             * Returns the received exception.
             * 
             * @return the received exception, may be <b>null</b>
             */
            public Exception getException() {
                return exception;
            }

            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {

                ExecutableMonitorListener<ISubmission> listener = 
                    new ExecutableMonitorListener<ISubmission>(name, monitor);
                try {
                    comm.replaySubmission(submission, assignment, listener);
                } catch (CommunicationException e) {
                    exception = e;
                }
                if (null == exception) {
                    exception = listener.getException();
                }
            }
        }

        RunnableWithProgress runnable = new RunnableWithProgress();
        runRunnable(runnable, schedulingRule);
        if (null != runnable.getException()) {
            GuiUtils.handleThrowable(runnable.getException());
        }
    }

    
    /**
     * Replays all submissions of the the specified <code>task</code> 
     * from the repository to <code>path</code>.
     * 
     * @param name the name to be displayed in the progress window
     * @param comm the submission communication object
     * @param path the target directory
     * @param assignment the task/exercise to be replayed
     * @param factory an instance able to create paths in the file system
     * @param schedulingRule the scheduling rule/resource to be locked
     *        while execution, may be <code>ResourcesPlugin
     *        .getWorkspace().getRoot()</code>, <b>null</b> or a more
     *        specific (resource) instance
     * 
     * @since 2.00
     */
    // checkstyle: stop parameter number check
    public static void runEntireReplay(final String name, final SubmissionCommunication comm, final File path,
        final Assignment assignment, final IPathFactory factory, ISchedulingRule schedulingRule) {
    // checkstyle: resume parameter number check

        /**
         * Implements a <i>long-running operation</i> to <b>check-out all submissions</b> action with a progress bar.
         *
         */
        class RunnableWithProgress implements IRunnableWithProgress {

            private Exception exception;

            /**
             * Returns the received exception.
             * 
             * @return the received exception, may be <b>null</b>
             */
            public Exception getException() {
                return exception;
            }

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

                ExecutableMonitorListener<ISubmission> listener = 
                    new ExecutableMonitorListener<ISubmission>(name, monitor);
                try {
                    comm.replayEntireTask(path, assignment, listener, factory);
                } catch (CommunicationException e) {
                    exception = e;
                }
                if (null == exception) {
                    exception = listener.getException();
                }
            }
        }

        RunnableWithProgress runnable = new RunnableWithProgress();
        runRunnable(runnable, schedulingRule);
        if (null != runnable.getException()) {
            GuiUtils.handleThrowable(runnable.getException());
        }
    }
    
    
    /**
     * Changes if submission success should be displayed.
     * 
     * @param showSubmissionSuccess <code>true</code> if 
     *        submission success should be displayed, 
     *        <code>false</code> if not
     * 
     * @since 1.00
     */
    public static void showSubmissionSuccess(boolean showSubmissionSuccess) {
        showSubmSuccess = showSubmissionSuccess;
    }
    
    /**
     * Submits an eclipse project.
     * 
     * @param messageListener a listener to receive and translate 
     *        submission messages
     * @param project the project to be submitted
     * @param comm the submission communication instance
     * @param assignment The assignment to submit
     * 
     * @since 2.00
     */
    public static final void submit(MessageListener messageListener, ISubmissionProject project,
        SubmissionCommunication comm, Assignment assignment) {
        
        messageListener.setProject(project);
        project.clearAllMarker();
        try {
            ISubmission abgabe = new Submission();
            File projectFolder = new File(project.getPath());
            if (!projectFolder.exists()) {
                URI projectURI = project.getResource().getLocationURI();
                projectFolder = new File(projectURI);
            }
            abgabe.setPath(projectFolder);
            GuiUtils.runExecutable("Submitting '" + project.getName() + "'", comm.submit(abgabe, assignment), project);
            String message;
            String msg;
            switch (abgabe.getResult()) {
            case SUCCESSFUL:
               // falls through
            case POST_SUCCESS:
                if (showSubmSuccess) {
                    GuiUtils.openDialog(GuiUtils.DialogType.INFORMATION, "Looks good! Your project '"
                        + project.getName() + "' was submitted.");
                }
                break;
            case POST_FAILED:
                msg = abgabe.getUnparsedMessage(messageListener);
                if (IConfiguration.INSTANCE.isDebuggingEnabled()) {
                    System.err.println(msg.toString());
                }
                message = "Your project '" + project.getName() + "' was submitted, but the automatic tests failed due "
                    + "to one or more error(s)/warnings(s).\nSee problems view. Note that these markers must be "
                    + "cleared manually via the context menu of the view, as they are induced by the submission server "
                    + "rather than your local Eclipse.\nWarnings are hints for optimisation (of functionality "
                    + "and points).\nErrors may lead to a reduction of points.";
                GuiUtils.openDialog(GuiUtils.DialogType.ERROR, message);
                break;
            case FAILED:
                msg = abgabe.getUnparsedMessage(messageListener);
                if (0 == messageListener.getCount()) {
                    message = "Your project '" + project.getName() + "' was rejected due to several errors. Server "
                        + "misconfigured for detailed error messages in Eclipse.";
                    if (IConfiguration.INSTANCE.
                        isDebuggingEnabled()) {
                        System.err.println(msg);
                    }
                } else {
                    message = "Your project '" + project.getName() + "' was rejected due to "
                        + messageListener.getCount() + " error";
                    if (messageListener.getCount() > 1) {
                        message += "s (see problems view, note that these markers must be removed manually via the "
                            + "context menu of the view, validate checkstyle configuration).";
                    }
                }
                GuiUtils.openDialog(GuiUtils.DialogType.ERROR, message);
                break;
            case EMPTY:
                if (showSubmSuccess) {
                    GuiUtils.openDialog(GuiUtils.DialogType.INFORMATION, "No relevant file changes were detected. "
                        + "No files have been submitted.");
                }
                break;
            default:
                break;
            }
        } catch (CommunicationException e) {
            GuiUtils.handleThrowable(e);
        }
    }
    
    /**
     * Runs the specified <code>executable</code>.
     * 
     * @param <F>
     *            the return type also the return type of the executable
     * @param name
     *            the name to be displayed in the progress window
     * @param executable
     *            the object to run
     * @param schedulingRule the scheduling rule/resource to be locked
     *        while execution, may be <code>ResourcesPlugin
     *        .getWorkspace().getRoot()</code>, <b>null</b> or a more
     *        specific (resource) instance
     * @return the result of executing <code>executable</code>
     * 
     * @since 2.00
     */
    public static <F> F runExecutable(final String name,
        final Executable<F> executable, ISchedulingRule schedulingRule) {

        /**
         * Implements a <i>long-running operation</i> to execute an {@link Executable} with a progressbar.
         *
         */
        class RunnableWithProgress implements IRunnableWithProgress {

            private Exception exception;

            private F result;

            /**
             * Returns the received exception.
             * 
             * @return the received exception, may be <b>null</b>
             */
            public Exception getException() {
                return exception;
            }

            /**
             * Returns the received result.
             * 
             * @return the received result, may be <b>null</b>
             */
            public F getResult() {
                return result;
            }

            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {

                ExecutableMonitorListener<F> listener = 
                    new ExecutableMonitorListener<F>(name, monitor);
                executable.setProgressListener(listener);
                executable.run();
                exception = listener.getException();
                result = listener.getResult();
            }
        }

        RunnableWithProgress runnable = new RunnableWithProgress();
        runRunnable(runnable, schedulingRule);
        if (null != runnable.getException()) {
            GuiUtils.handleThrowable(runnable.getException());
        }
        return runnable.getResult();
    }

    /**
     * Implements the default progress monitor listener to control the progress bar.
     * 
     * @author Holger Eichelberger
     * @param <F> The type of the result to be returned.
     * @since 2.00
     * @version 2.00
     */
    private static class ExecutableMonitorListener<F> implements ProgressListener<F> {

        /**
         * Stores the attached progress monitor.
         * 
         * @since 2.00
         */
        private IProgressMonitor monitor;

        /**
         * Stores the number of steps executed so far.
         * 
         * @since 2.00
         */
        private int step = 0;

        /**
         * Stores the total number of steps.
         * 
         * @since 2.00
         */
        private int numberOfSteps = 0;

        /**
         * Stores the name of the progress.
         * 
         * @since 2.00
         */
        private String name;

        /**
         * Stores the result.
         * 
         * @since 2.00
         */
        private F result;

        /**
         * Stores an exception occurred while executing the monitored progress.
         * 
         * @since 2.00
         */
        private Exception exception;

        /**
         * Creates a new progress monitor listener.
         * 
         * @param name
         *            the name of the task
         * @param monitor
         *            the Eclipse progress monitor
         * 
         * @since 2.00
         */
        public ExecutableMonitorListener(String name, IProgressMonitor monitor) {
            this.name = name;
            this.monitor = monitor;
        }

        /**
         * Returns the result of the execution.
         * 
         * @return the result
         * 
         * @since 2.00
         */
        public F getResult() {
            return result;
        }

        /**
         * Is called when processing is finished.
         * 
         * @param max
         *            set the associated progress bar to its maximum value (or
         *            keep the current value)
         * @param finished
         *            the result of the processing
         * 
         * @since 2.00
         */
        public void finished(boolean max, F finished) {
            monitor.done();
            result = finished;
        }

        /**
         * Returns the current progress step.
         * 
         * @return the current progress step
         * 
         * @since 2.00
         */
        public int getStep() {
            return step;
        }

        /**
         * Returns an occurred exception.
         * 
         * @return the exception or <b>null</b>
         * 
         * @since 2.00
         */
        public Exception getException() {
            return exception;
        }

        /**
         * Notifies this listener that an unhandled execption occurred.
         * 
         * @param exception
         *            the occurred exception
         * 
         * @since 2.00
         */
        public void notifyExceptionOccurred(Exception exception) {
            this.exception = exception;
        }

        /**
         * Notifies the beginning of the next step by announcing the next task
         * description (optional).
         * 
         * @param description
         *            the next task description to be displayed
         * 
         * @since 2.00
         */
        public void notifyNextStep(String description) {
            monitor.subTask(description);
        }

        /**
         * Is called when the total number of steps is changed.
         * 
         * @param steps
         *            the new total number of steps
         * 
         * @since 2.00
         */
        public void numberofStepsChanged(int steps) {
            numberOfSteps = steps;
            monitor.beginTask(name, steps);
        }

        /**
         * Is called when a step was processed.
         * 
         * @param step
         *            the progressed step
         * 
         * @since 2.00
         */
        public void processedStep(int step) {
            monitor.worked(step);
        }

        /**
         * Switches from individual step visualization to sweeping infinite
         * visualization.
         * 
         * @param doSweep
         *            <code>true</code> if sweeping should be activated,
         *            <code>false</code> for individual step display
         * 
         * @since 2.00
         */
        public void sweep(boolean doSweep) {
            if (doSweep) {
                monitor.beginTask(name, IProgressMonitor.UNKNOWN);

            } else {
                monitor.beginTask(name, numberOfSteps);
            }
        }

    }

    /**
     * Validates the submission server connections. Shows an Eclipse progress
     * bar in order to visualize the progress of connecting and communication
     * with the servers. If the servers were initially contacted so far, only
     * changing information is read on these servers again (like changed access
     * permissions).
     * 
     * @param config
     *            contains the (user) configuration
     * @param submissionUser an optional specialized user name for submission
     * @return the (valid) communication instances
     * 
     * @since 2.00
     */
    public static List<SubmissionCommunication> validateConnections(IConfiguration config, String submissionUser) {
        return validateConnections(config.getUserName(), config.getPassword(), submissionUser);
    }
    
    /**
     * Returns the first replay connection from 
     * {@link #validateConnections(IConfiguration, String)}.
     * 
     * @param config
     *            contains the (user) configuration
     * @param submissionUser an optional specialized user name for submission
     * @return a (valid) communication instance, <b>null</b> for none
     */
    public static SubmissionCommunication getFirstReplayConnection(IConfiguration config, String submissionUser) {
        List<SubmissionCommunication> comm =  GuiUtils.validateConnections(config, submissionUser);
        SubmissionCommunication replayComm = null;
        for (int i = 0; null == replayComm && i < comm.size(); i++) {
            if (comm.get(i).allowsReplay()) {
                replayComm = comm.get(i);
            }
        }
        return replayComm;
    }

    /**
     * Validates the submission server connections. Shows an Eclipse progress
     * bar in order to visualize the progress of connecting and communication
     * with the servers. If the servers were initially contacted so far, only
     * changing information is read on these servers again (like changed access
     * permissions). This call is asynchronous!
     * 
     * @param user
     *            the user name
     * @param password
     *            the password of <code>user</code>
     * @param submissionUser an optional specialized user name for submission
     * @return the (valid) communication instances
     * 
     * @since 2.10
     */
    public static List<SubmissionCommunication> validateConnections(
        final String user, final String password, final String submissionUser) {

        /**
         * Implements a <i>long-running operation</i> to validate connections to the server.
         *
         */
        class RunnableWithProgress implements IRunnableWithProgress {

            private CommunicationException exception = null;

            private List<SubmissionCommunication> result;

            /**
             * Returns the received exception.
             * 
             * @return the received exception, may be <b>null</b>
             */
            public CommunicationException getException() {
                return exception;
            }

            /**
             * Returns the received result.
             * 
             * @return the received result, may be <b>null</b>
             */
            public List<SubmissionCommunication> getResult() {
                return result;
            }

            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
                try {
                    result = SubmissionCommunication.getInstances(user, password, Activator.inReviewerMode(),
                        submissionUser, new SubmissionInstanceListener(monitor));
                } catch (CommunicationException e) {
                    result = new ArrayList<SubmissionCommunication>();
                    exception = e;
                }
            }
        }

        RunnableWithProgress runnable = new RunnableWithProgress();
        runRunnable(runnable, ResourcesPlugin.getWorkspace().getRoot());
        if (null != runnable.getException()) {
            handleThrowable(runnable.getException());
        }
        return runnable.getResult();
    }

    /**
     * Implements a submission listener to be used for updates of a progress bar
     * in another thread.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private static class SubmissionInstanceListener
        implements de.uni_hildesheim.sse.exerciseSubmitter.
        submission.CommunicationInstanceListener {

        /**
         * Stores the associationed progress dialog.
         * 
         * @since 2.00
         */
        private IProgressMonitor monitor;

        /**
         * Creates a new submission listener for a given dialog.
         * 
         * @param monitor
         *            the associated progress monitor
         * 
         * @since 2.00
         */
        public SubmissionInstanceListener(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        /**
         * Notifies that the program executes the specified step.
         * 
         * @param text
         *            a description of this step
         * @param number
         *            the number (0-max) of the server in the list of servers to
         *            be contacted
         * 
         * @since 2.00
         */
        public void doStep(String text, int number) {
            monitor.subTask(text);
            monitor.worked(1);
        }

        /**
         * Notifies this instance that contacting all servers has been finished.
         * 
         * @param error
         *            <code>true</code> if an error occurred,
         *            <code>false</code> else
         * 
         * @since 2.00
         */
        public void notifyContactingFinished(boolean error) {
            monitor.done();
        }

        /**
         * Notifies this instance that contacting all servers will be started
         * after returning from this method.
         * 
         * @since 2.00
         */
        public void notifyContactingStarted() {
        }

        /**
         * Notifies this instance about the maximum number of servers.
         * 
         * @param number
         *            the maximum number of servers
         * 
         * @since 2.00
         */
        public void notifyNumberOfServers(int number) {
            monitor
                .beginTask("Contacting submission servers and collecting "
                + "submission information", number);
        }

    }

    /**
     * Opens a list dialog.
     * 
     * @param <T>
     *            the type of the list items
     * @param title
     *            the text in the title bar of the dialog
     * @param message
     *            the message to be displayed above the selection list
     * @param items
     *            the items to be displayed in the selection list
     * @param multiple 
     *            if multiple selection is allowed
     * 
     * @return the objects in <code>items</code> which were selected by the
     *         user
     * 
     * @since 2.00
     */
    public static <T> Object[] showListDialog(String title, String message, List<T> items, boolean multiple) {
        ListRunnable<T> elr = new ListRunnable<T>(title, message, items, multiple);
        Display.getDefault().syncExec(elr);
        return elr.getResult();
    }
    
    /**
     * Implements a multiple list selection dialog.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private static class MultipleSelectionListDialog extends ListDialog {

        /**
         * Creates a new multiple list selection dialog.
         * 
         * @param parent the parent shell
         * 
         * @since 2.00
         */
        public MultipleSelectionListDialog(Shell parent) {
            super(parent);
        }
        
        /**
         * Return the style flags for the table viewer.
         * @return int
         */
        protected int getTableStyle() {
            return SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
        }
    }

    /**
     * Defines a runnable for displaying lists in the appropriate SWT thread
     * context.
     * 
     * @param <T>
     *            the type of the list items
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private static class ListRunnable<T> implements Runnable {

        /**
         * The elements selected by the user.
         * 
         * @since 2.00
         */
        private Object[] result;

        /**
         * Stores the title of the menu dialog.
         * 
         * @since 2.00
         */
        private String title;

        /**
         * Stores the message to be displayed above the selection list.
         * 
         * @since 2.00
         */
        private String message;

        /**
         * Stores the elements to be displayed in the selection list.
         * 
         * @since 2.00
         */
        private List<T> listElements;
        /**
         * Stores if multiple selection is allowed.
         * 
         * @since 2.00
         */
        private boolean multipleSelection;
        
        /**
         * Creates a new runnable for the list selection dialog.
         * 
         * @param title
         *            the title of the menu dialog
         * @param message
         *            the message to be displayed above the selection list
         * @param listElements
         *            the elements to be displayed in the selection list
         * @param multiple 
         *            if multiple selection is allowed
         * @since 2.00
         */
        public ListRunnable(String title, String message, List<T> listElements, boolean multiple) {
            this.title = title;
            this.message = message;
            this.listElements = listElements;
            this.multipleSelection = multiple;
        }

        /**
         * Opens the dialog.
         * 
         * @since 2.00
         */
        public void run() {
            ListDialog dlg;
            if (multipleSelection) {
                dlg = new MultipleSelectionListDialog(new Shell());
            } else {
                dlg = new ListDialog(new Shell());
            }
            dlg.setTitle(title);
            Object[] data = new Object[listElements.size()];
            listElements.toArray(data);
            dlg.setWidthInChars(80);
            dlg.setInput(data);
            dlg.setMessage(message);
            dlg.setContentProvider(new ArrayContentProvider());
            if (data.length > 0) {
                if (data[0] instanceof Assignment) {
                    dlg.setLabelProvider(new LabelProvider() {
                        
                        @Override
                        public String getText(Object element) {
                            return ((Assignment) element).getName();
                        }
                    });
                } else if (data[0] instanceof IVersionedSubmission) {
                    dlg.setLabelProvider(new LabelProvider() {
                    
                        @Override
                        public String getText(Object element) {
                            IVersionedSubmission submission = (IVersionedSubmission) element;
                            return submission.getDate() + " - " + submission.getAuthor();
                        }
                    });
                } else {
                    dlg.setLabelProvider(new LabelProvider());
                }
            } else {
                dlg.setLabelProvider(new LabelProvider());
            }
            
            
            
            if (1 == listElements.size()) {
                dlg.setInitialElementSelections(listElements);
            }
            dlg.open();

            result = dlg.getResult();
        }

        /**
         * Returns the selected list elements.
         * 
         * @return the selected list elements
         * 
         * @since 2.00
         */
        public Object[] getResult() {
            return result;
        }

    }

}
