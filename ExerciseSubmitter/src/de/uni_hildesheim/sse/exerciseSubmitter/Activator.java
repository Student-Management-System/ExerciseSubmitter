package de.uni_hildesheim.sse.exerciseSubmitter;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;
import net.ssehub.exercisesubmitter.protocol.backend.NetworkException;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;
import net.ssehub.exercisesubmitter.protocol.frontend.ExerciseReviewerProtocol;
import net.ssehub.exercisesubmitter.protocol.frontend.SubmitterProtocol;

/**
 * The activator class controls the plug-in life cycle.
 * 
 * @since 2.00
 * @version 2.1
 */
public class Activator extends AbstractUIPlugin {

    /**
     * Stores the ID of the plugin.
     * 
     * @since 2.00
     */
    public static final String PLUGIN_ID = "net.ssehub.ExerciseSubmitter";
    
    private static final String REVIEWER_CLASS = "de.uni_hildesheim.sse.exerciseReviewer.eclipse.ReviewUtils";

    /**
     * Stores an instance of this plugin.
     * 
     * @since 2.00
     */
    private static Activator plugin;
    
    /**
     * Stores the base URL of this plugin.
     * 
     * @since 2.00
     */
    private static URL baseURL;
    
    /**
     * A timer for executing timer tasks.
     * 
     * @since 2.00
     */
    private static Timer timer = new Timer();
    
    /**
     * Stores if this plugin should run in reviewer 
     * mode.
     * 
     * @since 2.00
     */
    private static boolean reviewerMode = false;
    
    /**
     * Stores <b>one</b> {@link SubmitterProtocol} instance for the ExerciseSubmitter/Reviewer.
     * If the reviewer Plug-in ins installed, this will automatically be an instance of
     * {@link ExerciseReviewerProtocol}.
     * @since 2.1
     */
    private static SubmitterProtocol mgmtProtocol;
    
    
    /**
     * Initializes the tasks.
     * 
     * @since 2.00
     */
    static {
        ensureTaskTags();
    }

    /**
     * The constructor of this class initializing {@link #plugin}.
     * 
     * @since 2.00
     */
    public Activator() {
        plugin = this;
    }
    
    /**
     * Returns if a string is valid (not <b>null</b> and at least one
     * character).
     * 
     * @param text the text to be checked
     * @return <code>true</code> if the string is valid, <code>false</code> else
     * 
     * @since 2.00
     */
    private static boolean isValidString(String text) {
        return null != text && text.length() > 0;
    }

    /**
     * Changes the reviewer mode.
     * 
     * @param asReviewer the new reviewer mode
     * 
     * @since 2.00
     */
    public static void setAsReviewer(boolean asReviewer) {
        reviewerMode = asReviewer;
    }
    
    /**
     * Returns if this plugin runs in reviewer mode.
     * 
     * @return <code>true</code> if it runs in reviwer mode,
     *         <code>false</code> else
     * 
     * @since 2.00
     */
    public static boolean inReviewerMode() {
        return reviewerMode;
    }
    
    /**
     * Ensures, that the proper task-tags are present.
     * 
     * @since 2.00
     */
    public static void ensureTaskTags() {
        Hashtable<String, String> options = JavaCore.getOptions();

        List<String> tasks = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(options.get(
            JavaCore.COMPILER_TASK_TAGS).toString(), ",");
        while (tokens.hasMoreTokens()) {
            tasks.add(tokens.nextToken());
        }

        List<String> priorities = new ArrayList<String>();
        tokens = new StringTokenizer(options.get(
            JavaCore.COMPILER_TASK_PRIORITIES).toString(), ",");
        while (tokens.hasMoreTokens()) {
            priorities.add(tokens.nextToken());
        }

        while (priorities.size() < tasks.size()) {
            priorities.add("HIGH");
        }
        while (priorities.size() > 0 && priorities.size() > tasks.size()) {
            priorities.remove(priorities.size() - 1);
        }

        boolean found = false;
        for (int i = 0; !found && i < tasks.size(); i++) {
            found |= tasks.get(i).equals("REVIEW");
        }

        if (!found) {
            tasks.add("REVIEW");
            priorities.add("HIGH");
        }

        options.put(JavaCore.COMPILER_TASK_TAGS,
            composeJavaCoreOptionString(tasks));
        options.put(JavaCore.COMPILER_TASK_PRIORITIES,
            composeJavaCoreOptionString(priorities));
        JavaCore.setOptions(options);
    }

    /**
     * Composes a java core option string.
     * 
     * @param list
     *            the list of options to be composed
     * @return the core option sting
     * 
     * @since 2.00
     */
    private static String composeJavaCoreOptionString(List<String> list) {
        StringBuilder builder = new StringBuilder("");
        for (String element : list) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(element);
        }
        return builder.toString();
    }
    
    /**
     * Starts up this plug-in.<br/> If this method throws an exception, it is
     * taken as an indication that plug-in initialization has failed; as a
     * result, the plug-in will not be activated; moreover, the plug-in will be
     * marked as disabled and ineligible for activation for the duration.<br/>
     * Plug-in startup code should be robust. In the event of a startup failure,
     * the plug-in's <code>shutdown</code> method will be invoked
     * automatically, in an attempt to close open files, etc.<br/> <b>Clients
     * must never explicitly call this method.</b>
     * 
     * @param context
     *            the bundle context for this plug-in
     * @exception Exception
     *                if this plug-in did not start up properly
     * @since 2.00
     */
    // checkstyle: stop exception type check: Forced by Eclipse API
    public void start(BundleContext context) throws Exception {
    // checkstyle: resume exception type check
        super.start(context);
        baseURL = context.getBundle().getEntry("/");
        // Needs to be initialized before the communication instance is created.
        initSubmitterProtocol();
        
        if (isValidString(IConfiguration.INSTANCE.getUserName())
            && isValidString(IConfiguration.INSTANCE.getPassword())) {
            
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        SubmissionCommunication.getInstances(IConfiguration.INSTANCE, null, reviewerMode, null);
                    } catch (CommunicationException e) {
                    }
                }
            });
            t.start();
        }
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 1);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        timer.schedule(new ResetCommunicationTimerTask(), cal.getTime(), 24 * 60 * 1000);
    }

    /**
     * Creates the single {@link SubmitterProtocol}, which should be used.
     */
    private void initSubmitterProtocol() {
        /* 
         * Check if reviewer is installed and load optimal Protocol.
         * Critical routes should be protected independently of used protocol.
         * Thus both plug-ins may use the ReviewerProtocol. However, for modularity reasons / separation of concerns
         * we use the different protocol classes here
         */
        String authServer = IConfiguration.INSTANCE.getProperty("auth.server");
        String stdmgmtServer = IConfiguration.INSTANCE.getProperty("stdmgmt.server");
        String course = IConfiguration.INSTANCE.getProperty("course");
        String svnServer = IConfiguration.INSTANCE.getProperty("svn.server");
        String semester = IConfiguration.INSTANCE.getProperty("debug.semester");
        try {
            Class.forName(REVIEWER_CLASS);
            reviewerMode = true;
            ExerciseReviewerProtocol protocol = new ExerciseReviewerProtocol(authServer, stdmgmtServer, course,
                svnServer);
            if (null != semester && !semester.isBlank()) {
                protocol.setSemester(semester);
            }
            
            Assignment assignment = IConfiguration.INSTANCE.getAssignment();
            if (null != assignment) {
                // Init protocol for current assignment
                try {
                    protocol.loadAssessments(assignment);
                } catch (NetworkException e) {
                    e.printStackTrace();
                }
            }
            
            Activator.mgmtProtocol = protocol;
        } catch (ClassNotFoundException e) {
            Activator.mgmtProtocol = new SubmitterProtocol(authServer, stdmgmtServer, course, svnServer);
            if (null != semester && !semester.isBlank()) {
                Activator.mgmtProtocol.setSemester(semester);
            }
        }
        
    }
    
    /**
     * A task to reset the communication information.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private class ResetCommunicationTimerTask extends TimerTask {

        /**
         * Executes the task.
         * 
         * @since 2.00
         */
        @Override
        public void run() {
            SubmissionCommunication.clearInstances();
        }
        
    }

    /**
     * Stops this plug-in.<br/> <b>Clients must never explicitly call this
     * method.</b>
     * 
     * @param context
     *            the bundle context for this plug-in
     * @exception Exception
     *                if this method fails to shut down this plug-in
     * @since 2.00
     */
    // checkstyle: stop exception type check: Forced by Eclipse API
    public void stop(BundleContext context) throws Exception {
    // checkstyle: resume exception type check
        timer.cancel();
        plugin = null;
        SubmissionCommunication.clearInstances();
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     * 
     * @return the shared instance
     * 
     * @since 2.00
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path.
     * 
     * @param path
     *            the path
     * @return the image descriptor
     * 
     * @since 2.00
     * @version 2.10
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, path).get();
    }
    
    /**
     * Returns the base URL of this plugin.
     * 
     * @return the base URL.
     * 
     * @since 2.00
     */
    public static URL getBaseURL() {
        return baseURL;
    }
    
    /**
     * Returns the {@link SubmitterProtocol} to query the <b>student management server</b>.
     * This protocol contains also the common business logic.
     * If the ExerciseReviewer fragment is installed, this will automatically be an instance of
     * {@link ExerciseReviewerProtocol}.
     * Critical routes should be protected independently of used protocol.
     * Thus both plug-ins may use the ReviewerProtocol. However, for modularity reasons / separation of concerns
     * we use the different protocol classes here
     * @return An instance of {@link SubmitterProtocol} or {@link ExerciseReviewerProtocol}. The user won't be logged in
     *     at this point.
     */
    public static SubmitterProtocol getProtocol() {
        return mgmtProtocol;
    }
    
    /**
     * Logs the given throwable.
     * 
     * @param message an arbitrary text
     * @param throwable the throwable
     * 
     * @since 2.00
     */
    public static void log(String message, Throwable throwable) {
        plugin.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, 
            message, throwable));
    }
    
}
