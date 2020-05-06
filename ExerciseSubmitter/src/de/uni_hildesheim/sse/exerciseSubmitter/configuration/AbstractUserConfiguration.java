package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

/**
 * A default implementation for handling user name and password according to the
 * {@link IConfiguration#store()} conventions.
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.10
 */
public abstract class AbstractUserConfiguration extends IConfiguration {

    /**
     * Stores the user name.
     * 
     * @since 1.00
     */
    protected String userName = "";

    /**
     * Stores the (plain text) password of the user.
     * 
     * @since 1.00
     */
    protected String password = "";
    
    /**
     * Stores the submission group name.
     * 
     * @since 2.10
     */
    protected String groupName = "";

    /**
     * Returns the stored password of the current user (user local
     * configuration).
     * 
     * @return the stored (plain text) password or an empty string in the case
     *         that no password was stored so far
     * 
     * @since 1.00
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Changes the password of the current user (user local configuration). Call
     * {@link #store()} to make this change permanent.
     * 
     * @param password
     *            the (plain text) password to be stored
     * 
     * @since 1.00
     */
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the current user name (user local configuration).
     * 
     * @return the current user name or an empty string if no user name was
     *         stored so far
     * 
     * @since 1.00
     */
    @Override
    public String getUserName() {
        return userName;
    }

    /**
     * Changes the user name of the current user (user local configuration).
     * Call {@link #store()} to make this change permanent.
     * 
     * @param userName
     *            the user name to be stored
     * 
     * @since 1.00
     */
    @Override
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    /**
     * Returns the current user submission group name (user local 
     * configuration). This value is
     * relevant dependent on {@link #isExplicitGroupNameEnabled()}
     * 
     * @return the current user submission group name or an empty string if no 
     *         name was stored so far.
     * 
     * @since 2.10
     */
    public String getGroupName() {
        return groupName;
    }
    
    /**
     * Changes the user submission group name of the (user local configuration).
     * Call {@link #store()} to make this change permanent. This value is
     * relevant dependent on {@link #isExplicitGroupNameEnabled()}.
     * 
     * @param groupName
     *            the user submission group name to be stored
     * 
     * @since 2.10
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

}
