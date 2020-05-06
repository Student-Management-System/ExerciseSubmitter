package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ISubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IVersionedSubmission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ServerAuthentication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.Submission;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;

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
            List<SubmissionCommunication> connections = 
                GuiUtils.validateConnections(IConfiguration.INSTANCE, null);
            
            boolean done = false;
            for (Iterator<SubmissionCommunication> iterator = connections
                .iterator(); !done && iterator.hasNext();) {
                SubmissionCommunication comm = iterator.next();
                if (comm.allowsReplay() && ServerAuthentication.getInstance().
                    authenticate(comm, false)) {
                    Map<String, ISubmissionProject> exercisesMap = mapProjects(
                        getReplaySubmissions(comm), false, comm);
                    for (Iterator<Map.Entry<String, ISubmissionProject>> iter = 
                            exercisesMap.entrySet().iterator(); 
                            iter.hasNext();) {
                        try {
                            Map.Entry<String, ISubmissionProject> entry = iter
                                .next();
                            List<IVersionedSubmission> submissions = comm
                                .getSubmissionsForReplay(entry.getKey());
                            
                            if (submissions.isEmpty()) {
                                GuiUtils.openDialog(
                                    GuiUtils.DialogType.INFORMATION, 
                                    "This task was not submitted so far " 
                                    + "- no data to replay.");
                            } else {
                                replay(comm, submissions, entry);
                                done = true;
                            }
                        } catch (CommunicationException e) {
                            GuiUtils.handleThrowable(e);
                        }
                    }
                }
            }

        }
    }
    
    /**
     * Executes the replay.
     * 
     * @param comm the communication instance
     * @param submissions the submissions to be replayed
     * @param entry the selected entry
     * 
     * @since 1.00
     */
    private void replay(SubmissionCommunication comm, 
        List<IVersionedSubmission> submissions, 
        Map.Entry<String, ISubmissionProject> entry) {
        Object[] result = GuiUtils.showListDialog(
            "Replay project '" + entry.getValue().getName()
            + "'", "Select the date of the version to be "
            + "replayed", submissions, true);

        if (null != result && result.length > 0) {
            if (entry.getValue().
                confirmOverwritingProject()) {
                ISubmission abgabe = new Submission();
                abgabe.setPath(new File(entry.getValue().
                    getPath()));
                GuiUtils.runReplay("Replaying submission", 
                    comm, abgabe, 
                    (IVersionedSubmission) result[0], 
                    entry.getValue());
                entry.getValue().refresh();
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
    private String[] getReplaySubmissions(SubmissionCommunication comm) {
        List<String> submissions = new ArrayList<String>();
        for (String s : comm.getAvailableForSubmission()) {
            submissions.add(s);
        }
        for (String s : comm.getSubmissionsForReplay()) {
            submissions.add(s);
        }
        String[] replaySubmissions = new String[submissions.size()];
        submissions.toArray(replaySubmissions);
        return replaySubmissions;
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
