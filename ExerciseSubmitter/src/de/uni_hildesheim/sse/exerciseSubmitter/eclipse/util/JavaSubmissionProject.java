package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

/**
 * An internal project class for Eclipse Java projects.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
class JavaSubmissionProject extends ISubmissionProject {

    /**
     * Stores the Java project.
     * 
     * @since 2.00
     */
    private IJavaProject project;
    
    /**
     * Stores the associated eclipse project instance.
     * 
     * @since 2.00
     */
    private IProject iProject = null;

    /**
     * Creates a new project wrapper instance.
     * 
     * @param project
     *            the project instance to be considered
     * 
     * @since 2.00
     */
    public JavaSubmissionProject(IJavaProject project) {
        this.project = project;

        IProject[] projects = ResourcesPlugin.
            getWorkspace().getRoot().getProjects();
        for (int i = projects.length - 1; 
            null == iProject && i >= 0; i--) {
            if (projects[i].getName().equals(project.getElementName())) {
                iProject = projects[i];
            }
        }

    }

    /**
     * Returns the name of the project.
     * 
     * @return the name of the project
     * 
     * @since 2.00
     */
    public String getName() {
        return project.getElementName();
    }

    /**
     * Returns the entire OS path to the project.
     * 
     * @return the path to the project
     * 
     * @since 2.00
     */
    public String getPath() {
        IPath path = ResourcesPlugin.getWorkspace().getRoot()
            .getRawLocation();
        return path + "" + project.getPath();
    }
    
    /**
     * Returns the associated eclipse project instance.
     * 
     * @return the associated eclipse project instance
     * 
     * @since 2.00
     */
    public IProject getProject() {
        return iProject;
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
        IResource result = null;
        try {
            IJavaElement element = project.findElement(new Path(path));
            if (null != element && element.getResource() != null) {
                result = element.getResource();
            } else {
                IProject project = ResourcesPlugin.getWorkspace().
                    getRoot().getProject(getName());
                result = project.findMember(path);
            }
        } catch (CoreException e) {
        }
        return result;
    }

    /**
     * Returns the root resource of this project.
     * 
     * @return the root resource of this project
     * 
     * @since 2.00
     */
    public IResource getResource() {
        return project.getResource();
    }

    /**
     * Refreshes, i.e. synchronizes this project with the file system.
     * 
     * @since 2.00
     */
    public void refresh() {
        try {
            project.getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
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
            project.getResource().refreshLocal(IResource.DEPTH_ONE, null);
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
        return getPreference(iProject, qualifiedName);
    }

}