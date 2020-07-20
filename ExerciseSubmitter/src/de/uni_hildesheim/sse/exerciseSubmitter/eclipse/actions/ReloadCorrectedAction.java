package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.io.File;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.AssignmentProjectMap;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ServerAuthentication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Submission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;

/**
 * Implements an action object for the reload corrected submission action. This
 * class is not directly referenced from within this project but it will be
 * called by eclipse via extension information in the manifest/plugin.xml.
 * 
 * @author Holger Eichelberger
 * @since 2.0
 * @version 2.0
 */
public class ReloadCorrectedAction extends AbstractSubmissionAction {

    /**
     * Creates a new action object (called by eclipse via extension information
     * in the manifest/plugin.xml).
     */
    public ReloadCorrectedAction() {
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
     * This method validates the server connections.
     * 
     * @param action the action proxy that handles the presentation 
     *        portion of the action
     */
    public void run(IAction action) {
        if (!handleProjectListErrors()) {
            List<SubmissionCommunication> connections = GuiUtils.validateConnections(IConfiguration.INSTANCE, null);
            for (SubmissionCommunication comm : connections) {
                if (comm.allowsReplay() && ServerAuthentication.getInstance().authenticate(comm, false)) {
                    AssignmentProjectMap exercisesMap = mapProjects(comm.getSubmissionsForReplay(), true, comm);
                    
                    for (AssignmentProjectMap.Entry entry : exercisesMap) {
                        ISubmissionProject project = entry.getProject();
                        if (project.confirmOverwritingProject()) {
                            ISubmission abgabe = new Submission();
                            abgabe.setPath(new File(entry.getProject().getPath()));
                            GuiUtils.runReplay("Replaying corrected submission", comm, abgabe, entry.getAssignment(), 
                                project);
                            project.refresh();
                        }
                    }
                }
            }
        }
        
        
//        if (!handleProjectListErrors()) {
//            List<SubmissionCommunication> connections = 
//                GuiUtils.validateConnections(IConfiguration.INSTANCE, null);
//
//            boolean done = false;
//            for (Iterator<SubmissionCommunication> iterator = connections
//                .iterator(); !done && iterator.hasNext();) {
//                SubmissionCommunication comm = iterator.next();
//                if (comm.allowsReplay() && ServerAuthentication.getInstance().
//                    authenticate(comm, false)) {
//                    Map<String, ISubmissionProject> exercisesMap = mapProjects(
//                            comm.getSubmissionsForReplay(), true, comm);
//                    for (Iterator<Map.Entry<String, ISubmissionProject>> 
//                        iter = exercisesMap.entrySet().iterator(); 
//                        iter.hasNext();) {
//                        Map.Entry<String, ISubmissionProject> entry = iter
//                            .next();
//
//                        if (entry.getValue().confirmOverwritingProject()) {
//                            ISubmission abgabe = new Submission();
//                            abgabe.setPath(new File(entry.getValue().
//                                getPath()));
//                            GuiUtils.runReplay("Replaying corrected "
//                                + "submission", comm, abgabe, entry.getKey(), 
//                                entry.getValue());
//                            entry.getValue().refresh();
//                        }
//                    }
//                    done = true;
//                }
//            }
//        }
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
     * @param action
     *            the action proxy that handles presentation portion of the
     *            action
     * @param selection
     *            the current selection, or <code>null</code> if there is no
     *            selection.
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

}
