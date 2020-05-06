package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

/**
 * An internal project class for general Eclipse projects.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
class SubmissionProject extends ISubmissionProject {

    /**
     * Stores the project.
     * 
     * @since 2.00
     */
    private IProject project;

    /**
     * Creates a new project wrapper instance.
     * 
     * @param project
     *            the project instance to be considered
     * 
     * @since 2.00
     */
    public SubmissionProject(IProject project) {
        this.project = project;
    }

    /**
     * Returns the name of the project.
     * 
     * @return the name of the project
     * 
     * @since 2.00
     */
    public String getName() {
        return project.getName();
    }

    /**
     * Returns the entire OS path to the project.
     * 
     * @return the path to the project
     * 
     * @since 2.00
     */
    public String getPath() {
        return project.getLocation().toString();
    }
    
    /**
     * Returns the associated eclipse project instance.
     * 
     * @return the associated eclipse project instance
     * 
     * @since 2.00
     */
    public IProject getProject() {
        return project;
    }

    /**
     * Returns an Eclipse resource object to the specified
     * path or file relative to this project (root directory).
     * 
     * @param path a relative path or file within the project
     * 
     * @return the resource object representing <code>path</code>
     *         or <b>null</b> if the element dies not exist
     *         
     * @since 2.00
     */
    public IResource getResource(String path) {
        return project.findMember(new Path(path));
    }

    /**
     * Returns the root resource of this project.
     * 
     * @return the root resource of this project
     * 
     * @since 2.00
     */
    public IResource getResource() {
        return project;
    }

    /**
     * Refreshes, i.e. synchronizes this project with the file system.
     * 
     * @since 2.00
     */
    public void refresh() {
        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
        }
    }
    
    /**
     * Refreshes, i.e. synchronizes the top-level path of this project with 
     * the file system.
     * 
     * @since 2.00
     */
    public void refreshTopLevel() {
        try {
            project.refreshLocal(IResource.DEPTH_ONE, null);
        } catch (CoreException e) {
        }
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
    public Object getPreference(String qualifiedName) {
        return getPreference(project, qualifiedName);
    }

}