package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util;

import org.eclipse.ui.PlatformUI;

/**
 * An utility class for switching the current workspace.
 * 
 * @author eichelberger
 * @since 2.00
 * @version 2.00
 */
public class SwitchWorkspace {

    // CODE taken from org.eclipse.ui.internal.ide.actions.OpenWorkspaceAction

    /**
     * Defines the system property key for holding the executing 
     * virtual machine. In particular, this is not true if Eclipse
     * was started as platform debugging instance from within eclipse.
     * 
     * @since 2.00
     */
    private static final String PROP_VM = "eclipse.vm";

    /**
     * Defines the system property key for holding the virtual machine 
     * parameters with which the current Eclipse instance was started.
     * 
     * @since 2.00
     */
    private static final String PROP_VMARGS = "eclipse.vmargs";

    /**
     * Defines the system property key for holding the parameters
     * with which the current Eclipse instance was started.
     * 
     * @since 2.00
     */
    private static final String PROP_COMMANDS = "eclipse.commands";

    /**
     * Defines the system property key for holding the eclipse exitcode
     * values (in our case to execute the command line associated 
     * to {@link #PROP_EXIT_DATA}).
     * 
     * @since 2.00
     */
    private static final String PROP_EXIT_CODE = "eclipse.exitcode";

    /**
     * Defines the system property key for holding eclipse exitdata
     * values (in our case the command line to be executed).
     * 
     * @since 2.00
     */
    private static final String PROP_EXIT_DATA = "eclipse.exitdata";

    /**
     * Defines the command line argument for specifying a workspace.
     * 
     * @since 2.00
     */
    private static final String CMD_DATA = "-data";

    /**
     * Defines the command line argument for specifying a Java virtual 
     * machine arguments.
     * 
     * @since 2.00
     */
    private static final String CMD_VMARGS = "-vmargs";

    /**
     * Defines a string containing the newline character(s).
     * 
     * @since 2.00
     */
    private static final String NEW_LINE = "\n";

    /**
     * Prevents this class from being instantiated from outside.
     * 
     * @since 2.00
     */
    private SwitchWorkspace() {
    }
    
    /**
     * Switches the current workspace by restarting Eclipse.
     * 
     * @param path the path of the new workspace
     * @return <code>true</code> if the workspace will be
     *         switched, <code>false</code> else
     * 
     * @since 2.00
     */
    public static boolean switchWorkspace(String path) {
        String commandLine = buildCommandLine(path);
        if (commandLine == null) {
            return false;
        }
        System.setProperty(PROP_EXIT_CODE, Integer.toString(24));
        System.setProperty(PROP_EXIT_DATA, commandLine);
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getWorkbench().
            restart();
        return true;
    }

    /**
     * Create and return a string with command line options for eclipse.exe that
     * will launch a new workbench that is the same as the currently running
     * one, but using the argument directory as its workspace.
     * 
     * @param workspace
     *            the directory to use as the new workspace
     * @return a string of command line options or null on error
     * 
     * @since 2.00
     */
    private static String buildCommandLine(String workspace) {
        String property = System.getProperty(PROP_VM);
        if (property == null) {
            return null;
        }

        StringBuffer result = new StringBuffer(512);
        result.append(property);
        result.append(NEW_LINE);

        // append the vmargs and commands. Assume that these already end in \n
        String vmargs = System.getProperty(PROP_VMARGS);
        if (vmargs != null) {
            result.append(vmargs);
        }

        // append the rest of the args, replacing or adding -data as required
        property = System.getProperty(PROP_COMMANDS);
        if (property == null) {
            result.append(CMD_DATA);
            result.append(NEW_LINE);
            result.append(workspace);
            result.append(NEW_LINE);
        } else {
            // find the index of the arg to replace its value
            int cmdDataPos = property.lastIndexOf(CMD_DATA);
            if (cmdDataPos != -1) {
                cmdDataPos += CMD_DATA.length() + 1;
                result.append(property.substring(0, cmdDataPos));
                result.append(workspace);
                result.append(property.substring(property.indexOf('\n',
                    cmdDataPos)));
            } else {
                result.append(CMD_DATA);
                result.append(NEW_LINE);
                result.append(workspace);
                result.append(NEW_LINE);
                result.append(property);
            }
        }

        // put the vmargs back at the very end (the eclipse.commands property
        // already contains the -vm arg)
        if (vmargs != null) {
            result.append(CMD_VMARGS);
            result.append(NEW_LINE);
            result.append(vmargs);
        }
        return result.toString();
    }
    
}
