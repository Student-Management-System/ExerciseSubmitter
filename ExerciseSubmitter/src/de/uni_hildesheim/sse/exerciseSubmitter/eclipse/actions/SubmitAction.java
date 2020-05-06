package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.tests.ClientSidedTest;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ServerAuthentication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;

/**
 * Implements an action object for the submission action. This class is not
 * directly referenced from within this project but it will be called by eclipse
 * via extension information in the manifest/plugin.xml.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class SubmitAction extends AbstractSubmissionAction {

    /**
     * Creates a new action object (called by eclipse via extension information
     * in the manifest/plugin.xml).
     * 
     * @since 2.00
     */
    public SubmitAction() {
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
     * 
     * @since 2.00
     */
    public void run(IAction action) {
        if (!handleProjectListErrors() 
            && checkSubmissionPrerequisites(getSelectedProjects())) {
            List<SubmissionCommunication> connections = 
                GuiUtils.validateConnections(IConfiguration.INSTANCE, null);
            MessageListener messageListener = new MessageListener();
            for (Iterator<SubmissionCommunication> iterator = connections
                .iterator(); iterator.hasNext();) {
                SubmissionCommunication comm = iterator.next();
                if (ServerAuthentication.getInstance().
                    authenticate(comm, true)) {
                    Map<String, ISubmissionProject> exercisesMap = mapProjects(
                        comm.getAvailableForSubmission(), false, comm);
                    for (Iterator<Map.Entry<String, 
                            ISubmissionProject>> iter = exercisesMap.
                            entrySet().iterator(); iter.hasNext();) {
                        Map.Entry<String, ISubmissionProject> entry = iter
                            .next();
                        GuiUtils.submit(messageListener, entry.getValue(), 
                            comm, entry.getKey());
                    }
                }
            }
        }
    }

    /**
     * Checks the specified project for client side submission problems.
     * 
     * @param msg the message returned so far, start with <b>null</b>; 
     *        no test is executed / window is shown if this parameter is
     *        not <b>null</b>
     * @param type the type of the client side tests to be executed
     * @param project the project on which the test should be executed
     * @param msgText the message text to provide additional help to
     *        the user in the case that a client side test fails
     * @return the message emitted returned by the first failing test
     *         or <b>null</b>
     * 
     * @since 2.00
     */
    private static String checkProject(String msg, ClientSidedTest.Type type, 
        ISubmissionProject project, String msgText) {
        if (null == msg) {
            GuiUtils.DialogType dlgType;
            switch (type) {
            case OPTIONAL:
                dlgType = GuiUtils.DialogType.INFORMATION;
                break;
            default:
                dlgType = GuiUtils.DialogType.ERROR;
                break;
            }
            msg = ClientSidedTest.allTestsEnableSubmission(type, project);
            if (null != msg) {
                GuiUtils.openDialog(dlgType, msgText + " : " + msg);
            }
        }
        return msg;
    }
    
    /**
     * Checks all submission projects for the fulfillment of 
     * client side tests.
     * 
     * @param projects the projects to be checked
     * @return <code>true</code> if all client side tests
     *         passed, <code>false</code> else
     * 
     * @since 2.00
     */
    public static boolean checkSubmissionPrerequisites(
        Iterator<ISubmissionProject> projects) {
        
        String msg = null;
        if (null != projects) {
            while (null == msg && projects.hasNext()) {
                ISubmissionProject project = projects.next();
                msg = checkProject(msg, ClientSidedTest.Type.CONFIGURATION, 
                    project, "The project(s) cannot be submitted");
                msg = checkProject(msg, ClientSidedTest.Type.REQUIRED, 
                    project, "The project(s) cannot be submitted");
                checkProject(msg, ClientSidedTest.Type.OPTIONAL, 
                    project, "The project(s) will be submitted but "
                    + "consider the following message");
            }
        }
        return null == msg;
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
