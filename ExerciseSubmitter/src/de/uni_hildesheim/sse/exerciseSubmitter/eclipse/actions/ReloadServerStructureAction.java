package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;

/**
 * Implements an action object for clearing all cached server structure 
 * information.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class ReloadServerStructureAction extends AbstractSubmissionAction {

    /**
     * Creates a new action object (called by eclipse via extension information
     * in the manifest/plugin.xml).
     * 
     * @since 2.00
     */
    public ReloadServerStructureAction() {
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
        SubmissionCommunication.clearInstances();
        GuiUtils.validateConnections(IConfiguration.INSTANCE, null);        
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
