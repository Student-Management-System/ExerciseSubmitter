package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CountingSubmissionListener;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IMessage;

/**
 * Implements the submission message listener in order to insert
 * messages as markers.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class MessageListener extends CountingSubmissionListener {

    /**
     * Defines the type of the Eclipse markers handled by this plugin.
     * 
     * @since 2.00
     */
    public static final String SUBMIT_MARKER = 
        "de.uni_hildesheim.sse.exerciseSubmitter.submitProblem";
    
    /**
     * Stores the project on which this message listener was created.
     * 
     * @since 2.00
     */
    private ISubmissionProject project;
    
    /**
     * Is called when a message occurs. Transfers the relevant
     * information from the message instance into a newly created
     * Eclipse marker.
     * 
     * @param message a message from the submission server
     * 
     * @since 2.00
     */
    public void notifyMessage(IMessage message) {
        IMarker marker = project.createMarker(
            SUBMIT_MARKER, message.getFile());
        if (null != marker) {
            setMarkerAttribute(marker, IMarker.USER_EDITABLE, true);
            if (message.getLine() >= 0) {
                setMarkerAttribute(marker, IMarker.LINE_NUMBER, message
                    .getLine());
                setMarkerAttribute(marker, IMarker.LOCATION, "line "
                    + message.getLine());
            }
            if (hasContents(message.getMessage())) {
                String msg = message.getMessage();
                msg = msg.replace('\n', ' ');
                if (marker.getResource() instanceof IProject 
                    && null != message.getFile()) {
                    // fight the fallback, don't loose file location information
                    // if the resource cannot be found and project is used as 
                    // a fallback 
                    msg += " in " + message.getFile();
                }
                setMarkerAttribute(marker, IMarker.MESSAGE, msg);
            }
            if (hasContents(message.getTool())) {
                setMarkerAttribute(marker, "tool", message.getTool());
            }
            switch (message.getType()) {
            case ERROR:
                setMarkerAttribute(marker, IMarker.SEVERITY,
                        IMarker.SEVERITY_ERROR);
                setMarkerAttribute(marker, IMarker.PRIORITY,
                        IMarker.PRIORITY_HIGH);
                break;
            case UNKNOWN:
                setMarkerAttribute(marker, IMarker.SEVERITY,
                        IMarker.SEVERITY_INFO);
                setMarkerAttribute(marker, IMarker.PRIORITY,
                        IMarker.PRIORITY_NORMAL);
                break;
            case WARNING:
                setMarkerAttribute(marker, IMarker.SEVERITY,
                        IMarker.SEVERITY_WARNING);
                setMarkerAttribute(marker, IMarker.PRIORITY,
                        IMarker.PRIORITY_LOW);
                break;
            default:
                break;
            }
        }
        super.notifyMessage(message);
    }

    /**
     * Changes an object value on the given <code>marker</code>.
     * 
     * @param marker the marker to be modified
     * @param attributeName the name of the marker attribute to be modified
     * @param value the value to be set
     * 
     * @since 2.00
     */
    private void setMarkerAttribute(IMarker marker, String attributeName,
            Object value) {
        try {
            marker.setAttribute(attributeName, value);
        } catch (CoreException e) {
        }
    }

    /**
     * Changes an int value on the given <code>marker</code>.
     * 
     * @param marker the marker to be modified
     * @param attributeName the name of the marker attribute to be modified
     * @param value the value to be set
     * 
     * @since 2.00
     */
    private void setMarkerAttribute(IMarker marker, String attributeName,
            int value) {
        try {
            marker.setAttribute(attributeName, value);
        } catch (CoreException e) {
        }
    }

    /**
     * Changes a boolean value on the given <code>marker</code>.
     * 
     * @param marker the marker to be modified
     * @param attributeName the name of the marker attribute to be modified
     * @param value the value to be set
     * 
     * @since 2.00
     */
    private void setMarkerAttribute(IMarker marker, String attributeName,
            boolean value) {
        try {
            marker.setAttribute(attributeName, value);
        } catch (CoreException e) {
        }
    }

    /**
     * Changes the attached project.
     * 
     * @param project the new project instance
     * 
     * @since 2.00
     */
    public void setProject(ISubmissionProject project) {
        this.project = project;
        resetCounter();
    }
    
    /**
     * Returns if the specified string instance exists and is not empty.
     * 
     * @param string the string to be tested
     * 
     * @return <code>true</code> if the string has contents, 
     *         <code>false</code> else
     */
    static final boolean hasContents(String string) {
        return string != null && string.length() > 0;
    }

}