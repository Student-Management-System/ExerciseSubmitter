package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.AssignmentProjectMap;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IVersionedSubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ServerAuthentication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Submission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;

/**
 * Implements an action object for the reload previous version action. This
 * class is not directly referenced from within this project but it will be
 * called by eclipse via extension information in the manifest/plugin.xml.
 * 
 * @author Holger Eichelberger
 * @since 2.0
 * @version 2.0
 */
public class ReloadPreviousVersionAction extends AbstractSubmissionAction {

    /**
     * Creates a new action object (called by eclipse via extension information
     * in the manifest/plugin.xml).
     */
    public ReloadPreviousVersionAction() {
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
     * @param action the action proxy that handles the presentation portion 
     *        of the action
     *        
     * @since 2.0
     */
    public void run(IAction action) {
        if (!handleProjectListErrors()) {
            List<SubmissionCommunication> connections = GuiUtils.validateConnections(IConfiguration.INSTANCE, null);
//            MessageListener messageListener = new MessageListener();
            for (SubmissionCommunication comm : connections) {
                if (comm.allowsReplay() && ServerAuthentication.getInstance().authenticate(comm, false)) {
                    AssignmentProjectMap exercisesMap = mapProjects(getReplaySubmissions(comm), false, comm);
                    
                    for (AssignmentProjectMap.Entry entry : exercisesMap) {
                        try {
                            List<IVersionedSubmission> submissions = comm.getSubmissionsForReplay(
                                entry.getAssignment());
                            
                            if (submissions.isEmpty()) {
                                GuiUtils.openDialog(GuiUtils.DialogType.INFORMATION,
                                    "This task was not submitted so far - no data to replay.");
                            } else {
                                replay(comm, submissions, entry.getProject());
                                break;
                            }
                        } catch (CommunicationException e) {
                            GuiUtils.handleThrowable(e);
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
//                        getReplaySubmissions(comm), false, comm);
//                    for (Iterator<Map.Entry<String, ISubmissionProject>> iter = 
//                            exercisesMap.entrySet().iterator(); 
//                            iter.hasNext();) {
//                        try {
//                            Map.Entry<String, ISubmissionProject> entry = iter
//                                .next();
//                            List<IVersionedSubmission> submissions = comm
//                                .getSubmissionsForReplay(entry.getKey());
//                            
//                            if (submissions.isEmpty()) {
//                                GuiUtils.openDialog(
//                                    GuiUtils.DialogType.INFORMATION, 
//                                    "This task was not submitted so far " 
//                                    + "- no data to replay.");
//                            } else {
//                                replay(comm, submissions, entry);
//                                done = true;
//                            }
//                        } catch (CommunicationException e) {
//                            GuiUtils.handleThrowable(e);
//                        }
//                    }
//                }
//            }
//
//        }
    }
    
    /**
     * Executes the replay.
     * 
     * @param comm the communication instance
     * @param submissions the submissions to be replayed
     * @param project The selected project
     * 
     * @since 1.00
     */
    private void replay(SubmissionCommunication comm, List<IVersionedSubmission> submissions, 
        ISubmissionProject project) {
        
        Object[] result = GuiUtils.showListDialog("Replay project '" + project.getName() + "'",
            "Select the date of the version to be replayed", submissions, true);

        if (null != result && result.length > 0) {
            if (project.confirmOverwritingProject()) {
                ISubmission abgabe = new Submission();
                abgabe.setPath(new File(project.getPath()));
                GuiUtils.runReplay("Replaying submission", comm, abgabe, (IVersionedSubmission) result[0], project);
                project.refresh();
            }
        }
    }

    /**
     * Returns all submissions for replay on <code>comm</code>.
     * 
     * @param comm the communication instance
     * @return all submissions available for replay
     * 
     * @since 1.20
     */
    private List<Assignment> getReplaySubmissions(SubmissionCommunication comm) {
        List<Assignment> submissions = new ArrayList<>();
        submissions.addAll(comm.getAvailableForSubmission());
        submissions.addAll(comm.getSubmissionsForReplay());
        return submissions;
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
     *            
     * @since 2.0
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

}
