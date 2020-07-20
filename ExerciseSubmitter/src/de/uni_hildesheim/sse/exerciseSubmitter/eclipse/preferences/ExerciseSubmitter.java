package de.uni_hildesheim.sse.exerciseSubmitter.eclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.configuration.PreferenceConstants;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.SubmissionCommunication;

/**
 * This class represents the preference page of the exercise submitter eclipse
 * plugin that is contributed to the Preferences dialog.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 * 
 * @version 2.2
 * @since 2.0
 * @author Holger Eichelberger
 * @author El-Sharkawy
 */
public class ExerciseSubmitter extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    /**
     * Stores the user editor field.
     * 
     * @since 2.0
     */
    private StringFieldEditor userName;
   
    /**
     * Stores the password editor field.
     * 
     * @since 2.0
     */
    private StringFieldEditor password;

    /**
     * Stores the user name before editing.
     * 
     * @since 2.0
     */
    private String userNameBefore = "";

    /**
     * Stores the password before editing.
     * 
     * @since 2.0
     */
    private String passwordBefore = "";

    /**
     * Creates a new exercise submitter.
     * 
     * @since 2.0
     */
    public ExerciseSubmitter() {
        super(GRID);
        setDescription("Preferences of the SSE Stiftung University of "
            + "Hildesheim exercise submitter "
            + "(values will be validated during the first server access). "
            + "Warning: Passwords and user names are case sensitive.");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     * 
     * @since 2.0
     */
    public void createFieldEditors() {
        userName = new StringFieldEditor(PreferenceConstants.USERNAME, "user name:", getFieldEditorParent());
        addField(userName);
        userName.setStringValue(IConfiguration.INSTANCE.getUserName());
        password = new StringFieldEditor(PreferenceConstants.PASSWORD, "password:", getFieldEditorParent());
        password.getTextControl(getFieldEditorParent()).setEchoChar('*');
        password.setStringValue(IConfiguration.INSTANCE.getPassword());
        addField(password);
        userNameBefore = userName.getStringValue();
        passwordBefore = password.getStringValue();
    }

    /**
     * Method declared on IPreferencePage. Subclasses should override
     * 
     * @return if the preferences input is ok
     * 
     * @since 2.0
     */
    public boolean performOk() {
        boolean ok = true;
        if (!userName.getStringValue().equals(userNameBefore) || !password.getStringValue().equals(passwordBefore)) {
            SubmissionCommunication.clearInstances();
            userNameBefore = userName.getStringValue();
            passwordBefore = password.getStringValue();
            IConfiguration.INSTANCE.setUserName(userNameBefore);
            IConfiguration.INSTANCE.setPassword(passwordBefore);
            IConfiguration.INSTANCE.store();
        }
        return ok;
    }

    /**
     * Performs special processing when this page's Apply button has been
     * pressed.
     * <p>
     * This is a framework hook method for subclasses to do special things when
     * the Apply button has been pressed. The default implementation of this
     * framework method simply calls <code>performOk</code> to simulate the
     * pressing of the page's OK button.
     * </p>
     * 
     * @see #performOk
     * 
     * @since 2.0
     */
    protected void performApply() {
        performOk();
    }

    /**
     * Initializes this preference page for the given workbench.
     * <p>
     * This method is called automatically as the preference page is being
     * created and initialized. Clients must not call this method.
     * 
     * @param workbench
     *            the workbench
     * 
     * @since 2.0
     */
    public void init(IWorkbench workbench) {
    }

}