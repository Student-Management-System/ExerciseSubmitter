package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;

/**
 * Implements an action object for clearing a selected marker (in the problem
 * view). This class is not directly referenced from within this project but it
 * will be called by eclipse via extension information in the
 * manifest/plugin.xml.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public class ClearSelectedMarkerAction extends AbstractSubmissionAction {

    /**
     * Stores the target part from the latest call of
     * {@link #setActivePart(IAction, IWorkbenchPart)}.
     * 
     * @since 2.00
     */
    private IWorkbenchPart targetPart;
    
    /**
     * Creates a new action object (called by eclipse via extension information
     * in the manifest/plugin.xml).
     * 
     * @since 2.00
     */
    public ClearSelectedMarkerAction() {
        super();
    }

    /**
     * Sets the active part for the delegate. The active part is commonly used
     * to get a working context for the action, such as the shell for any dialog
     * which is needed.
     * <p>
     * This method will be called every time the action appears in a popup menu.
     * The targetPart may change with each invocation.
     * </p>
     * 
     * @param action
     *            the action proxy that handles presentation portion of the
     *            action; must not be <code>null</code>.
     * @param targetPart
     *            the new part target; must not be <code>null</code>.
     * 
     * @since 2.00
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.targetPart = targetPart;
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
        if (targetPart instanceof ViewPart) {
            ViewPart vp = (ViewPart) targetPart;
            ISelection selection = vp.getViewSite().getPage().getSelection();
            if (null != selection && !selection.isEmpty()
                && selection instanceof StructuredSelection) {
                StructuredSelection ssel = (StructuredSelection) selection;
                List<IMarker> toDelete = new ArrayList<IMarker>();

                for (Iterator<?> iter = ssel.iterator(); iter.hasNext();) {
                    Object selected = iter.next();
                    if (selected instanceof IMarker) {
                        IMarker marker = (IMarker) selected;
                        try {
                            if (MessageListener.SUBMIT_MARKER.equals(marker
                                .getType())) {
                                toDelete.add(marker);
                            }
                        } catch (CoreException e) {
                        }
                    }
                }

                for (IMarker marker : toDelete) {
                    try {
                        marker.delete();
                    } catch (CoreException e) {
                    }
                }

                int otherCount = ssel.size() - toDelete.size();
                if (otherCount > 0) {
                    String plural1;
                    String plural2;
                    if (otherCount > 1) {
                        plural1 = "s";
                        plural2 = "";
                    } else {
                        plural1 = "";
                        plural2 = "es";
                    }
                    GuiUtils.openDialog(GuiUtils.DialogType.ERROR,
                        "Cannot delete " + otherCount + " marker"
                        + plural1 + " which do" + plural2
                        + " not belong to the ExerciseSubmitter "
                        + "plugin.");
                }
            }
        }
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
