package de.uni_hildesheim.sse.exerciseSubmitter.submission;

/**
 * Defines the information to be issued by a submission server
 * in the case of an error/a warning...
 * 
 * @author Holger Eichelberger
 * @since 2.00
 * @version 2.00
 */
public interface IMessage {

    /**
     * Defines the possible types of the message.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    public static enum MessageType {
        
        /**
         * An error.
         * 
         * @since 2.00
         */
        ERROR, 

        /**
         * A warning.
         * 
         * @since 2.00
         */
        WARNING, 
        
        /**
         * An unknown message.
         * 
         * @since 2.00
         */
        UNKNOWN;

        /**
         * Returns the appropriate message type for 
         * the given string. Lower/Upper cases are ignored.
         * 
         * @param name the string to search the constant for
         * @return one of the constants in this enumeration
         * 
         * @since 2.00
         */
        public static MessageType valueOfIgnoreCase(String name) {
            MessageType result;
            try {
                result = (null == name || name.isEmpty()) ? UNKNOWN
                    : valueOf(MessageType.class, name.toUpperCase());
            } catch (IllegalArgumentException e) {
                result = UNKNOWN;
            }
            return result;
        }
    }

    /**
     * Returns the issuing tool.
     * 
     * @return the issuing tool (optional)
     * 
     * @since 2.00
     */
    public String getTool();

    /**
     * Returns the name of the file in which the message 
     * occurred.
     * 
     * @return the name of the file
     * 
     * @since 2.00
     */
    public String getFile();

    /**
     * Returns an optional example text.
     * 
     * @return the example text
     * 
     * @since 2.00
     */
    public String getExample();

    /**
     * Returns the character position in the 
     * {@link #getExample() example} to be highlighted.
     * 
     * @return the character position or <code>-1</code>
     *         if the position is not valid or irrelevant
     * 
     * @since 2.00
     */
    public int getPositionInExample();

    /**
     * Returns the message text itself.
     * 
     * @return the message text itself
     * 
     * @since 2.00
     */
    public String getMessage();

    /**
     * Returns the type of the message.
     * 
     * @return the type of the message
     * 
     * @since 2.00
     */
    public MessageType getType();

    /**
     * Returns the line within {@link #getFile()}.
     * 
     * @return the line in {@link #getFile()} or 
     *         <code>-1</code> if unspecified
     *         
     * @since 2.00
     */
    public int getLine();

}
