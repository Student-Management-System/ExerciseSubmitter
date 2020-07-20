package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.google.common.collect.Streams;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ServerAuthentication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.SubmissionDirEntry;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;

/**
 * Briefly shows the contents of the last submission.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class ShowSubmissionAction extends AbstractSubmissionAction {

    /**
     * Creates a new action object (called by eclipse via extension information
     * in the manifest/plugin.xml).
     * 
     * @since 2.00
     */
    public ShowSubmissionAction() {
        super();
    }

    /**
     * Performs this action.
     * <p>
     * This method is called by the proxy action when the action has been
     * triggered. Implement this method to do the actual work.
     * </p>
     * <p>
     * <b>Note:</b> If the action delegate also implements
     * <code>IActionDelegate2</code>, then this method is not invoked but
     * instead the <code>runWithEvent(IAction, Event)</code> method is called.
     * </p>
     * 
     * @param action the action proxy that handles the presentation portion 
     *        of the action
     *               
     * @since 2.00
     */
    public void run(IAction action) {
        if (!handleProjectListErrors()) {
            List<SubmissionCommunication> connections = 
                GuiUtils.validateConnections(IConfiguration.INSTANCE, null);
            boolean found = false;
            boolean available = false;
            List<String> dirs = new ArrayList<String>();
            for (Iterator<SubmissionCommunication> iterator = connections
                .iterator(); !found && iterator.hasNext();) {
                SubmissionCommunication comm = iterator.next();
                if (ServerAuthentication.getInstance().
                    authenticate(comm, true)) {
                    List<ISubmissionProject> projects = 
                        getSelectedProjects(comm);
                    for (ISubmissionProject project : projects) {
                        available |= handleEntries(
                            project, comm, dirs, projects.size() > 1);
                    }
                    found = true;
                }
            }
            if (!dirs.isEmpty()) {
                GuiUtils.showListDialog("Submitted files", "Submitted files", 
                    dirs, false);
            }
            if (!connections.isEmpty() && !available) {
                GuiUtils.openDialog(GuiUtils.DialogType.INFORMATION, "The "
                    + "selected task is currently not available, e.g. due "
                    + "to review activities.");
            }
        }
    }

    /**
     * Handles the directory entries and fills <code>result</code> as a side 
     * effect.
     * 
     * @param project the project the latest submissions should be returned for
     * @param comm the communication instance
     * @param result the files list to be modified as a side effect
     * @param showProjectName if the name of the project should be emitted
     * @return <code>true</code> if the project is available, 
     *         <code>false</code> else
     * 
     * @since 1.00
     */
    private boolean handleEntries(ISubmissionProject project, SubmissionCommunication comm, List<String> result, 
        boolean showProjectName) {
        
        boolean available = false;
        try {
            String name = project.getName();
            Assignment assignment = getAssignment(comm, name);
            if (null == assignment) {
                /* 
                 * Current project isn't named as an open assignment
                 * -> Let user choose which assignment he want's to contribute to
                 */
                List<Assignment> exercisesList = new ArrayList<>();
                exercisesList.addAll(comm.getAvailableForSubmission());
                exercisesList.addAll(comm.getAvailableForSubmission());
                Object[] selRes = GuiUtils.showListDialog("Project '" + name + "' does not match an exercise on the "
                    + "server", "Select the corresponding exercise", exercisesList, true);
                if (null != selRes && selRes.length > 0) {
                    name = selRes[0].toString();
                }
            }
            
            assignment = getAssignment(comm, name);
            if (null != assignment) {
                available = true;
                List <SubmissionDirEntry> entries = comm.getLastContents(assignment);
                if (showProjectName) {
                    result.add(name + ":");
                }
                for (SubmissionDirEntry entry : entries) {
                    if (!entry.isDirectory()) {
                        result.add(entry.getPath() + " (" 
                            + entry.getFormattedDate() + ", " 
                            + entry.getFormattedSize() + ")");
                    } else {
                        result.add(entry.getPath() + " (" 
                            + entry.getFormattedDate() + ")");
                    }
                }
            }
        } catch (CommunicationException ce) {
            GuiUtils.handleThrowable(ce);
        }
        return available;
    }
    
    /**
     * Searches for a submission/replay assignment with the specified the project name {@code name} on the server.
     * 
     * @param comm the communication instance
     * @param name the (local) project name
     * @return An open assignment (in submission/replay state) with the given name or <tt>null</tt>.
     */
    private static Assignment getAssignment(SubmissionCommunication comm, final String name) {
        Stream<Assignment> availableAssignments = Streams.concat(comm.getSubmissionsForReplay().stream(),
            comm.getSubmissionsForReplay().stream());
        
        Assignment assignment = availableAssignments
            .filter(a -> a.getName().equals(name))
            .findFirst()
            .orElse(null);
        
        return assignment;
    }

    /**
     * Notifies this action delegate that the selection in the workbench has
     * changed.
     * <p>
     * Implementers can use this opportunity to change the availability of the
     * action or to modify other presentation properties.
     * </p>
     * <p>
     * When the selection changes, the action enablement state is updated based
     * on the criteria specified in the plugin.xml file. Then the delegate is
     * notified of the selection change regardless of whether the enablement
     * criteria in the plugin.xml file is met.
     * </p>
     * 
     * @param action the action proxy that handles presentation portion of the
     *        action
     * @param selection the current selection, or <code>null</code> if there 
     *        is no selection
     *        
     * @since 2.00
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

}
