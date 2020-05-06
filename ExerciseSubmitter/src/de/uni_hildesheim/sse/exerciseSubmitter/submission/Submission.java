package de.uni_hildesheim.sse.exerciseSubmitter.submission;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.uni_hildesheim.sse.exerciseSubmitter.configuration.IConfiguration;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.IMessage.MessageType;

/**
 * Implements {@link ISubmission} with a concrete XML protocol.
 * 
 * <pre>
 * &lt;submitResults&gt;
 *   &lt;message tool=&quot;&quot; type=&quot;&quot; file=&quot;&quot; 
 *     line=&quot;&quot; message=&quot;&quot;&gt;
 *     &lt;example position=&quot;&quot;&gt; &lt;!-- optional--&gt;
 *     &lt;!-- arbitrary text --&gt;
 *     &lt;/example&gt;
 *   &lt;/message&gt;
 * &lt;/submitResults&gt;
 * </pre>
 * 
 * @author Alexander Schmehl
 * @since 1.00
 * @version 2.00
 */
public class Submission implements ISubmission {

    private static final String START_OF_MSG = "<submitResults>";
    private static final String END_OF_MSG = "</submitResults>";
    
    /**
     * Defines the unique identification for serialization.
     * 
     * @since 2.00
     */
    private static final long serialVersionUID = 3197295927066102093L;

    /**
     * Stores the message after execution.
     * 
     * @since 2.00
     */
    private String message = "";

    /**
     * Stores the result of the execution.
     * 
     * @since 2.00
     */
    private Result result = Result.FAILED;

    /**
     * Stores the path to be submitted or on which an operation should be
     * executed.
     * 
     * @since 2.00
     */
    private File path;

    /**
     * Sets the path of the directory to be submitted or where to replay a
     * submission to.
     * 
     * @param path
     *            the path to be considered
     * 
     * @since 2.00
     */
    public void setPath(File path) {
        this.path = path;
    }

    /**
     * Returns the path of the directory to be submitted or where to replay a
     * submission to.
     * 
     * @return the path to be considered/submitted
     * 
     * @since 2.00
     */
    public File getPath() {
        return path;
    }

    /**
     * Returns the result of an operation on this instance.
     * 
     * @return the result of the operation
     * 
     * @since 2.00
     */
    public Result getResult() {
        return result;
    }

    /**
     * Returns the entire message of the last execution of an operation on this
     * instance. An operation must not return a message.
     * 
     * @return the message(s) of the last operation
     * 
     * @since 2.00
     */
    public String getMessage() {
        return message;
    }

    /**
     * Stores if the result of the execution.
     * 
     * @param result the result of the execution
     * 
     * @since 2.00
     */
    public void setResult(Result result) {
        this.result = result;
    }

    /**
     * Stores the message describing the results of the submission.
     * 
     * @param message
     *            the results of the submission
     * 
     * @since 2.00
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Unparses the message of the last execution returned by
     * {@link #getMessage()}. Unparsing may deliver individual messages if the
     * messages is in an internal XML format.
     * 
     * @param listener
     *            a listener to be informed about each individual (sub)message
     * @return the message itself unparsed from XML or the same result as
     *         {@link #getMessage()} if no XML is in the message
     * 
     * @since 2.00
     */
    public String getUnparsedMessage(IMessageListener listener) {
        return getUnparsedMessage(message, listener);
    }

    /**
     * Unparses the given message. Unparsing may deliver individual messages if 
     * the messages is in an internal XML format.
     * 
     * @param message the message to (un)parse
     * @param listener
     *            a listener to be informed about each individual (sub)message
     * @return the message itself unparsed from XML or the same result as
     *         {@link #getMessage()} if no XML is in the message
     * 
     * @since 2.00
     */
    public static String getUnparsedMessage(String message, 
        IMessageListener listener) {
        if (null == message) {
            return "";
        }
        String tmp = message.trim();
        int startPos = tmp.indexOf(START_OF_MSG);
        int endPos = tmp.lastIndexOf(END_OF_MSG);
        if (startPos >= 0 && endPos > 0) {
            tmp = tmp.substring(0, endPos + END_OF_MSG.length());
            // some illegal reflective access warnings?
            tmp = tmp.substring(startPos);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            try {
                SAXParser parser = factory.newSAXParser();
                SubmitResultsHandler handler 
                    = new SubmitResultsHandler(listener);
                parser.parse(new ByteArrayInputStream(tmp.getBytes()), handler);
                return handler.toString();
            } catch (Exception e) {
                return message;
            }
        } else {
            if (IConfiguration.INSTANCE.isXmlExpected()) {
                return "Server or server communication failure. Please inform "
                    + "the supervisors for a server inspection. Message was: " 
                    + message;            
            } else {
                return message;
            }
        }
    }

    /**
     * Implements the message interface.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private static class Message implements IMessage {

        /**
         * Stores the example text.
         * 
         * @since 2.00
         */
        private String example = "";

        /**
         * Stores the file in which the message occurred.
         * 
         * @since 2.00
         */
        private String file = "";

        /**
         * Stores the message itself.
         * 
         * @since 2.00
         */
        private String message = "";

        /**
         * Stores the issuing tool.
         * 
         * @since 2.00
         */
        private String tool = "";

        /**
         * Stores the type of this message.
         * 
         * @since 2.00
         */
        private MessageType type = MessageType.UNKNOWN;

        /**
         * Stores a marked position within the {@link #example}.
         * 
         * @since 2.00
         */
        private int position = -1;

        /**
         * Stores the line within {@link #file} in which the message occurred.
         * 
         * @since 2.00
         */
        private int line = -1;

        /**
         * Returns an optional example text.
         * 
         * @return the example text
         * 
         * @since 2.00
         */
        public String getExample() {
            return example;
        }

        /**
         * Returns the character position in the {@link #getExample() example}
         * to be highlighted.
         * 
         * @return the character position or <code>-1</code> if the position
         *         is not valid or irrelevant
         * 
         * @since 2.00
         */
        public int getPositionInExample() {
            return position;
        }

        /**
         * Returns the name of the file in which the message occurred.
         * 
         * @return the name of the file
         * 
         * @since 2.00
         */
        public String getFile() {
            return file;
        }

        /**
         * Returns the message text itself.
         * 
         * @return the message text itself
         * 
         * @since 2.00
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns the issuing tool.
         * 
         * @return the issuing tool (optional)
         * 
         * @since 2.00
         */
        public String getTool() {
            return tool;
        }

        /**
         * Returns the type of the message.
         * 
         * @return the type of the message
         * 
         * @since 2.00
         */
        public MessageType getType() {
            return type;
        }

        /**
         * Returns the line within {@link #getFile()}.
         * 
         * @return the line in {@link #getFile()} or <code>-1</code> if
         *         unspecified
         * 
         * @since 2.00
         */
        public int getLine() {
            return line;
        }

    }

    /**
     * Implements a SAX XML reading handler.
     * 
     * @author Holger Eichelberger
     * @since 2.00
     * @version 2.00
     */
    private static class SubmitResultsHandler extends DefaultHandler {

        /**
         * Stores a string builder used to combine the individual information to
         * an unparsed XML text (as if there was no XML data at all).
         * 
         * @since 2.00
         */
        private StringBuilder builder = new StringBuilder();

        /**
         * Stores the message listener.
         * 
         * @since 2.00
         */
        private IMessageListener listener;

        /**
         * Stores the currently processed message.
         * 
         * @since 2.00
         */
        private Message curMessage;

        /**
         * Returns if the current parsing stage is in example.
         * 
         * @since 2.00
         */
        private boolean inExample = false;

        /**
         * Creates a submit results handler.
         * 
         * @param listener
         *            the listener to be informed if a message was found in the
         *            XML data
         * 
         * @since 2.00
         */
        public SubmitResultsHandler(IMessageListener listener) {
            this.listener = listener;
        }

        /**
         * Returns the unparsed contents as textual representation.
         * 
         * @return the unparsed contents
         * 
         * @since 2.00
         */
        public String toString() {
            return builder.toString();
        }

        /**
         * Reads arbitrary text data.
         * 
         * @param ch
         *            the character buffer filled with text data
         * @param start
         *            the valid start position in <code>ch</code>
         * @param length
         *            the length of valid data in <code>ch</code>
         * 
         * @throws SAXException
         *             if any exception occurs
         * 
         * @since 2.00
         */
        public void characters(char[] ch, int start, int length)
            throws SAXException {
            if (inExample) {
                String s = new String(ch, start, length);
                int len;
                do {
                    len = s.length();
                    while (s.startsWith("\n")) {
                        s = s.substring(1);
                    }
                    while (s.startsWith("\r")) {
                        s = s.substring(1);
                    }
                } while (len != s.length());
                do {
                    len = s.length();
                    while (s.endsWith("\n")) {
                        s = s.substring(0, s.length() - 1);
                    }
                    while (s.startsWith("\r")) {
                        s = s.substring(0, s.length() - 1);
                    }
                } while (len != s.length());
                s = s.trim();
                if (s.length() > 0) {
                    builder.append(s);
                    curMessage.message += " " + s;
                }
            }
        }

        /**
         * Is called when an element ends.
         * 
         * @param uri
         *            the namespace uri
         * @param localName
         *            the local name of the element
         * @param qName
         *            the qualified name of the element
         * 
         * @throws SAXException
         *             if any exception occurs
         * 
         * @since 2.00
         */
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
            if (qName.equals("message")) {
                if (null != listener) {
                    listener.notifyMessage(curMessage);
                }
                curMessage = null;
            } else if (qName.equals("example")) {
                if (curMessage.position >= 0) {
                    builder.append("\n");
                    for (int i = 1; i <= curMessage.position; i++) {
                        builder.append(" ");
                    }
                    builder.append("^\n");
                    inExample = false;
                }
            }
        }

        /**
         * Reads ignorable whitespace data.
         * 
         * @param ch
         *            the character buffer filled with text data
         * @param start
         *            the valid start position in <code>ch</code>
         * @param length
         *            the length of valid data in <code>ch</code>
         * @throws SAXException
         *             if any exception occurs
         * 
         * @since 2.00
         */
        public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
        }

        /**
         * Is called when an element is opened.
         * 
         * @param uri
         *            the namespace uri
         * @param localName
         *            the local name of the element
         * @param qName
         *            the qualified name of the element
         * @param atts
         *            the attributes of this element
         * 
         * @throws SAXException
         *             if any exception occurs
         * 
         * @since 2.00
         */
        public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
            if (qName.equals("message")) {
                if (null != curMessage) {
                    throw new SAXException();
                }
                Message message = new Message();
                message.tool = atts.getValue("tool");
                message.type = MessageType.valueOfIgnoreCase(atts
                    .getValue("type"));
                message.file = atts.getValue("file");
                try {
                    message.line = Integer.parseInt(atts.getValue("line"));
                } catch (NumberFormatException e) {
                    message.line = -1;
                }
                message.message = atts.getValue("message");
                if (null != message.file) {
                    builder.append(message.file);
                    builder.append(":");
                }
                if (-1 != message.line) {
                    builder.append(message.line);
                    builder.append(":");
                }
                if (message.tool.equals("javadoc")) {
                    builder.append(atts.getValue("type"));
                    builder.append(" - ");
                }
                builder.append(message);
                builder.append("\n");
                curMessage = message;

            } else if (qName.equals("example")) {
                if (null == curMessage) {
                    throw new SAXException();
                }
                try {
                    curMessage.position = Integer.parseInt(atts
                        .getValue("position"));
                    inExample = true;
                } catch (NumberFormatException nfe) {
                    throw new SAXException();
                }
            }
        }

    }

}
