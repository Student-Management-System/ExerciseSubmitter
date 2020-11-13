package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaProject;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.actions.MessageListener;

/**
 * An internal project interface to provide an unique element for representing
 * the different not type compatible project classes in Eclipse.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public abstract class ISubmissionProject implements ISchedulingRule {

    /**
     * Returns the name of the project.
     * 
     * @return the name of the project
     * 
     * @since 2.00
     */
    public abstract String getName();

    /**
     * Returns the entire OS path to the project.
     * 
     * @return the path to the project
     * 
     * @since 2.00
     */
    public abstract String getPath();
    
    /**
     * Returns the associated eclipse project instance.
     * 
     * @return the associated eclipse project instance
     * 
     * @since 2.00
     */
    public abstract IProject getProject();

    /**
     * Returns an Eclipse resource object to the specified
     * relative path or file.
     * 
     * @param path a relative path or file within the project
     * 
     * @return the resource object representing <code>path</code>
     *         or <b>null</b> if the element dies not exist
     *         
     * @since 2.00
     */
    public abstract IResource getResource(String path);

    /**
     * Returns the root resource of this project.
     * 
     * @return the root resource of this project
     * 
     * @since 2.00
     */
    public abstract IResource getResource();

    /**
     * Creates a marker object (of a plugin local type) for the
     * specified project relative file or path.
     * 
     * @param type the type of the marker
     * @param path the file or path to create the marker for
     * 
     * @return the marker object or <b>null</b> if the marker cannot
     *         be created
     *         
     * @since 2.00
     */
    public IMarker createMarker(String type, String path) {
        IMarker result = null;
        try {
            IResource resource = null == path ? getProject()
                : getResource(path);
            if (null == resource) {
                // fallback
                resource = getProject();
            }
            if (null != resource) {
                result = resource.createMarker(type);
            }
        } catch (CoreException e) {
        }
        return result;
    }
    
    /**
     * Returns all markers matching the given specification.
     * 
     * @param type the type of the marker
     * @param includeSubtypes should subtypes be included
     * @param depth the depth of the search, constants see below
     * @return the markers or <b>null</b> if none was found or
     *         an internal error occurred
     * 
     * @see IResource#DEPTH_ZERO
     * @see IResource#DEPTH_ONE
     * @see IResource#DEPTH_INFINITE
     * 
     * @since 2.00
     */
    public IMarker[] getMarker(String type, boolean includeSubtypes, 
        int depth) {
        IMarker[] result;
        try {
            result = getResource().findMarkers(type, includeSubtypes, depth);
        } catch (CoreException e) {
            result = null;
        }
        return result;
    }

    /**
     * Removes all marker objects on this project created by this
     * plugin.
     * 
     * @since 2.00
     */
    public void clearAllMarker() {
        try {
            getResource().deleteMarkers(MessageListener.SUBMIT_MARKER, true,
                IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            // something went wrong
        }
    }

    /**
     * Refreshes, i.e. synchronizes this project with the file system.
     * 
     * @since 2.00
     */
    public abstract void refresh();

    /**
     * Refreshes, i.e. synchronizes the top-level path of this project with 
     * the file system.
     * 
     * @since 2.00
     */
    public abstract void refreshTopLevel();
    
    /**
     * Returns a wrapping project instance for Eclipse Java projects.
     * 
     * @param project the Java project instance to be wrapped
     * @return the wrapping instance
     * 
     * @since 2.00
     */
    public static final ISubmissionProject createSubmissionProject(
        IJavaProject project) {
        return new JavaSubmissionProject(project);
    }

    /**
     * Returns a wrapping project instance for general Eclipse projects.
     * 
     * @param project the general Eclipse project instance to be wrapped
     * @return the wrapping instance
     * 
     * @since 2.00
     */
    public static final ISubmissionProject createSubmissionProject(
        IProject project) {
        return new SubmissionProject(project);
    }
    
    /**
     * Ask the user for a confirming on overwriting the contents of this
     * project. 
     * 
     * @return <code>true</code> if the project files can be overwritten,
     *         <code>false</code> if the project files should not be modified
     *         
     * @since 2.00
     */
    public boolean confirmOverwritingProject() {
        return GuiUtils.openDialog(GuiUtils.DialogType.CONFIRMATION,
            "The contents of project '" + getName()
            + "' will be overwritten. Continue?");
    }
    
    /**
     * Returns the specified value from the project preferences
     * (if available).
     * 
     * @param qualifiedName the qualified name of the preference
     * @return the value or <b>null</b> if no value can be 
     *         retrieved for the specified <code>qualifiedName</code>
     * 
     * @since 2.00
     */
    public abstract Object getPreference(String qualifiedName);

    /**
     * Returns the specified value from the project preferences
     * (if available).
     * 
     * @param project the project to be considered
     * @param qualifiedName the qualified name of the preference
     * @return the value or <b>null</b> if no value can be 
     *         retrieved for the specified <code>qualifiedName</code>
     * 
     * @since 2.00
     */
    protected static Object getPreference(IProject project, 
        String qualifiedName) {
        QualifiedName qn;
        int pos = qualifiedName.lastIndexOf('.');
        if (pos > 0) {
            qn = new QualifiedName(qualifiedName.substring(0, pos), 
                qualifiedName.substring(pos + 1, qualifiedName.length()));
        } else {
            qn = new QualifiedName("", qualifiedName);
        }
        String result;
        try {
            result = project.getPersistentProperty(qn);
        } catch (CoreException e) {
            result = null;
        }
        return result;
    }
    
    /**
     * Returns whether this scheduling rule completely contains another 
     * scheduling rule. Rules can only be nested within a thread if the 
     * inner rule is completely contained within the outer rule. <p>
     * Implementations of this method must obey the rules of a partial 
     * order relation on the set of all scheduling rules. In particular, 
     * implementations must be reflexive (a.contains(a) is always true), 
     * antisymmetric (a.contains(b) and b.contains(a) iff a.equals(b), 
     * and transitive (if a.contains(b) and b.contains(c), then 
     * a.contains(c)). Implementations of this method must return 
     * <code>false</code> when compared to a rule they know nothing about.
     * 
     * @param rule the rule to check for containment
     * @return <code>true</code> if this rule contains the given rule, and 
     * <code>false</code> otherwise.
     * 
     * @since 2.00
     */
    public boolean contains(ISchedulingRule rule) {
        return getProject().contains(rule);
    }

    /**
     * Returns whether this scheduling rule is compatible with another 
     * scheduling rule. If <code>true</code> is returned, then no job with 
     * this rule will be run at the same time as a job with the conflicting 
     * rule. If <code>false</code> is returned, then the job manager is free 
     * to run jobs with these rules at the same time.<p>
     * Implementations of this method must be reflexive, symmetric, and 
     * consistent, and must return <code>false</code> when compared to a 
     * rule they know nothing about.
     *
     * @param rule the rule to check for conflicts
     * @return <code>true</code> if the rule is conflicting, and 
     *         <code>false</code> otherwise.
     *  
     * @since 2.00
     */
    public boolean isConflicting(ISchedulingRule rule) {
        return getProject().isConflicting(rule);
    }


}