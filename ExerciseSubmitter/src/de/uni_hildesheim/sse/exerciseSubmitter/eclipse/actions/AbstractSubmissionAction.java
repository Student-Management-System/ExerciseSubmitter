package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.AssignmentProjectMap;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.ISubmissionProject;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.ServerAuthentication;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    SubmissionCommunication;
import net.ssehub.exercisesubmitter.protocol.frontend.Assignment;

/**
 * Realizes an abstract action class for implementing submission actions to the
 * submission server.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public abstract class AbstractSubmissionAction extends AbstractHandler
    implements IObjectActionDelegate {

    static final IAction LEGACY_ACTION = new Action() {
    };
    
    /**
     * Stores the selected projects (valid immediately after
     * {@link #setActivePart(IAction, IWorkbenchPart)}.
     * 
     * @since 2.00
     */
    private List<ISubmissionProject> selectedProjects = 
        new ArrayList<ISubmissionProject>();

    /**
     * Returns the selected projects (valid immediately after
     * {@link #setActivePart(IAction, IWorkbenchPart)}.
     * 
     * @param comm the communication instance (may be <b>null</b>) needed
     *        to call {@link ServerAuthentication#
     *        mapProjects(List, SubmissionCommunication)}
     * @return the selected projects
     * 
     * @since 2.00
     */
    protected List<ISubmissionProject> getSelectedProjects(SubmissionCommunication comm) {
        return ServerAuthentication.getInstance().mapProjects(selectedProjects, comm);
    }
    
    /**
     * Returns the set of selected projects.
     * 
     * @return the selected projects
     * 
     * @since 2.00
     */
    protected Iterator<ISubmissionProject> getSelectedProjects() {
        return selectedProjects.iterator();
    }

    /**
     * Handles typical errors dependent on the length or the contents of the
     * project list. Uses the methods in {@link GuiUtils}.
     * 
     * @return <code>true</code> if errors were detected, <code>false</code>
     *         else
     * 
     * @since 2.00
     */
    protected boolean handleProjectListErrors() {
        boolean hasErrors = false;
        if (selectedProjects.isEmpty()) {
            GuiUtils.openDialog(GuiUtils.DialogType.INFORMATION, "No project selected for submission.");
            hasErrors = true;
        }
        if (IConfiguration.INSTANCE.getUserName().length() == 0) {
            GuiUtils.openDialog(GuiUtils.DialogType.INFORMATION, "No user name provided in preferences (see "
                + "Window|Preferences|Exercise Submitter).");
            hasErrors = true;
        }
        return hasErrors;
    }

    /**
     * Maps an array of exercise (directory) names to the projects selected in
     * Eclipse by their name. If no obvious match can be found, a selection list
     * is displayed.
     * 
     * @param exercises
     *            The array of exercise (directory) names on the server
     * @param replay
     *            <code>true</code> if an replay action called this method,
     *            <code>false</code> if a submission action initiated the call
     * @param comm the communication instance (may be <b>null</b>) needed to 
     *        call {@link #getSelectedProjects(SubmissionCommunication)}.
     * @return a map of exercise (directory) names and internal project
     *         instances representing projects in Eclipse (which are not
     *         required to belong to a common interface or class type)
     * 
     * @since 2.00
     */
    protected AssignmentProjectMap mapProjects(List<Assignment> exercises, boolean replay,
        SubmissionCommunication comm) {
        
        List<ISubmissionProject> projects = getSelectedProjects(comm);
        AssignmentProjectMap exercisesMap = new AssignmentProjectMap();

        // enumerate the exercise (directory) names first
        List<String> exercisesList = new ArrayList<String>();
        for (Assignment exercise : exercises) {
            exercisesList.add(exercise.getName());
            exercisesMap.put(exercise, null);
        }

        // destructive mapping between Eclipse projects and (directory) names
        List<ISubmissionProject> tmpProjects = new ArrayList<ISubmissionProject>(projects);
        for (Iterator<ISubmissionProject> iter = tmpProjects.iterator(); iter.hasNext();) {
            ISubmissionProject project = iter.next();
            String name = project.getName();
            if (exercisesMap.containsKey(name)) {
                iter.remove();
                exercisesList.remove(name);
                exercisesMap.set(name, project);
            }
        }

        // ask the user for remaining, unmapped Eclipse projects
        boolean cancel = false;
        for (Iterator<ISubmissionProject> iter = tmpProjects.iterator(); 
            exercisesList.size() > 0 && iter.hasNext();) {
            ISubmissionProject project = iter.next();

            Object[] result = GuiUtils.showListDialog("Project '" + project.getName() + "' does not match an exercise "
                + "on the server", "Select the corresponding exercise", exercisesList, true);
            if (null != result && result.length > 0) {
                // exercisesList should contain only names of already stored Assignments -> exercisesMap.set is usable
                String name = (String) result[0];
                exercisesMap.set(name, project);
                exercisesList.remove(result[0]);
            } else {
                cancel = true;
            }
        }

        // remove unmapped exercise (directories)
        exercisesMap.prune();

        // emit an error if required
        if (!cancel && exercisesMap.isEmpty()) {
            GuiUtils.openDialog(GuiUtils.DialogType.INFORMATION, "Currently no exercises for "
                + (replay ? "replay are available" : "submission are enabled") + " on the server.");
        }

        return exercisesMap;
    }

    /**
     * Returns the projects which are selected in the specified
     * <code>targetPart</code> of Eclipse. This method is declared static,
     * because it is intended to be reusable as an utility from outside the
     * inheritance hierarchy of this class.
     * 
     * @param targetPart
     *            the target part on which an action is executed
     * 
     * @return the list of selected projects
     * 
     * @since 2.00
     */
    public static final List<ISubmissionProject> getSelectedProjects(
        IWorkbenchPart targetPart) {
        List<ISubmissionProject> result = new ArrayList<ISubmissionProject>();
        /*if (targetPart instanceof IPackagesViewPart) {
            IPackagesViewPart pvp = (IPackagesViewPart) targetPart;
            TreeViewer tv = pvp.getTreeViewer();
            Tree tree = tv.getTree();
            TreeItem[] item = tree.getSelection();
            for (int i = 0; i < item.length; i++) {
                if (item[i].getData() instanceof IJavaProject) {
                    result.add(ISubmissionProject.createSubmissionProject(
                        (IJavaProject) item[i].getData()));
                }
            }
        } else*/ 
        if (targetPart instanceof IViewPart) {
            IViewPart viewPart = (IViewPart) targetPart;
            IViewSite site = viewPart.getViewSite();
            if (null != site) {
                ISelectionProvider selectionProvider = 
                    site.getSelectionProvider();
                if (null != selectionProvider) {
                    ISelection selection = selectionProvider.getSelection();
                    if (selection instanceof IStructuredSelection) {
                        IStructuredSelection structuredSelection = 
                            (IStructuredSelection) selection;
                        Object[] items = structuredSelection.toArray();
                        getSelectedProjects(result, items);
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Changes the given list in order to add all selected project (wrapper)
     * instances.
     * 
     * @param projects the project list to be modified as a side effect
     * @param items the UI items to be considered
     * 
     * @since 2.00
     */
    private static void getSelectedProjects(List<ISubmissionProject> projects, 
        Object[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof IJavaProject) {
                projects.add(ISubmissionProject.createSubmissionProject(
                    (IJavaProject) items[i]));
            }
        }
    }
    
    /**
     * Sets the active part for the delegate. The active part is commonly used
     * to get a working context for the action, such as the shell for any dialog
     * which is needed.
     * <p>
     * This method will be called every time the action appears in a popup menu.
     * The targetPart may change with each invocation.
     * </p>
     * This method stores the result of 
     * {@link #getSelectedProjects(SubmissionCommunication)} in
     * {@link #selectedProjects}.
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
        selectedProjects = AbstractSubmissionAction
            .getSelectedProjects(targetPart);
    }
    
    /**
     * Returns the first index of <code>value</code> in 
     * <code>array</code>.
     * 
     * @param <T> the value type of the array to be considered
     * @param array the array <code>value</code> should be searched in
     * @param value the value to be searched for
     * @return <code>-1</code> if <code>value</code> is not member of 
     *         <code>array</code>, the position of <code>value</code>
     *         in <code>array</code> else
     * 
     * @since 2.00
     */
    public static <T> int indexOf(T[] array, T value) {
        int pos = -1;
        for (int i = 0; pos < 0 && i < array.length; i++) {
            if ((value == null && array[i] == null) || (value != null && value.equals(array[i]))) {
                pos = i;
            }
        }
        return pos;
    }

    /**
     * Returns the active workbench part.
     * 
     * @return the active part
     */
    public static IWorkbenchPart getActiveWorkbenchPart() {
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
        IWorkbenchPage page = win.getActivePage();
        return page.getActivePart();
    }
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        setActivePart(LEGACY_ACTION, getActiveWorkbenchPart());
        run(LEGACY_ACTION);
        return null;
    }

}
