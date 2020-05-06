package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Method;

import de.uni_hildesheim.sse.exerciseSubmitter.Activator;

/**
 * Enumeration for denoting operation systems.
 * 
 * @author Alexander Schmehl
 * @version 2.00
 * @since 1.00
 */
public enum OsTypes {

    /**
     * Linux and Unix Systems.
     * 
     * @since 1.00
     */
    LINUX("/etc/", "UTF8", "Cp1252"),

    /**
     * Windows Systems (Windows NT would need further checks!).
     * 
     * @since 1.00
     */
    WINDOWS("c:\\windows\\", "Cp1252", "Cp1252"), 
    // TODO check Windows NT and c:\winnt

    /**
     * Mac OS (at least version X).
     * 
     * @since 1.00
     */
    MACOSX("/etc/", "UTF8", "Cp1252"),

    /**
     * Other, unknown operating systems.
     * 
     * @since 1.00
     */
    UNSUPPORTED("", "UTF8", "Cp1252");

    /**
     * Stores if the program is running in
     * the Eclipse execution environment.
     * 
     * @since 2.00
     */
    private static Boolean isEclipse;

    /**
     * Stores the path prefix for system wide configuration files.
     * 
     * @since 1.00
     */
    private final String globalConfigPrefix;

    /**
     * Stores the encoding assumed to be used for en/decoding UI texts.
     * This might be significantly different from the expected input
     * and, in order to reach compatibility, it might be different 
     * from the system encoding.
     */
    private String uiEncoding;

    /**
     * Stores the encoding assumed to be used for en/decoding files.
     * This might be significantly different from the expected input
     * and, in order to reach compatibility, it might be different 
     * from the system encoding.
     */
    private String fileEncoding;
    
    /**
     * Constructor for enumeration constants in order to 
     * set {@link #globalConfigPrefix}.
     * 
     * @param globalConfigPrefix the path prefix to system wide 
     *        configuration files to be stored in the newly created constant
     * @param uiEncoding the encoding for user interface input
     * @param fileEncoding the encoding for files
     * 
     * @since 1.00
     */
    private OsTypes(String globalConfigPrefix, String uiEncoding, 
        String fileEncoding) {
        this.globalConfigPrefix = globalConfigPrefix;
        this.uiEncoding = uiEncoding;
        this.fileEncoding = fileEncoding;
    }

    /**
     * Returns the explicit file encoding.
     * 
     * @return the file encoding
     * 
     * @since 1.00
     */
    public String getFileEncoding() {
        return fileEncoding;
    }

    /**
     * Returns the explicit UI encoding.
     * 
     * @return the UI encoding
     * 
     * @since 1.00
     */
    public String getUiEncoding() {
        return fileEncoding;
    }

    /**
     * Returns a reader which considers the file encoding.
     * 
     * @param file the file to be read
     * @return the file reader
     * @throws IOException in case of I/O exceptions
     * 
     * @since 1.00
     */
    public Reader getDecodingReader(File file) throws IOException {
        return new InputStreamReader(
            new FileInputStream(file), fileEncoding);
    }

    /**
     * Returns a writer which considers the file encoding.
     * 
     * @param file the file to be read
     * @return the file writer
     * @throws IOException in case of I/O exceptions
     * 
     * @since 1.00
     */
    public Writer getEncodingWriter(File file) throws IOException {
        return new OutputStreamWriter(
            new FileOutputStream("test.txt"), fileEncoding);
    }
   
    /**
     * Decodes a string from the UI.
     * 
     * @param string the string to be decoded
     * @return the decoded string, <code>string</code> in case of an 
     *     illegal encoding
     * 
     * @since 1.00
     */
    public String fromUI(String string) {
        String result;
        try {
            result = new String(string.getBytes(), uiEncoding);
        } catch (UnsupportedEncodingException e) {
            result = string;
            Activator.log(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Encodes a string for the UI.
     * 
     * @param string the string to be encoded
     * @return the encoded string, <code>string</code> in case of an 
     *     illegal encoding
     * 
     * @since 1.00
     */
    public String toUI(String string) {
        String result;
        try {
            result = new String(string.getBytes(uiEncoding));
        } catch (UnsupportedEncodingException e) {
            result = string;
            Activator.log(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Returns the path prefix to system wide configuration files.
     * 
     * @return the global configuration path prefix
     * 
     * @since 1.00
     */
    public String getGlobalConfigPrefix() {
        return globalConfigPrefix;
    }

    /**
     * Returns the path prefix to the user local configuration files.
     * 
     * @return the path prefix to the user local configuration files
     * 
     * @since 1.00
     */
    public String getUserConfigPrefix() {
        return System.getProperty("user.home") + java.io.File.separator;
    }

    /**
     * Returns the type of operating system.
     * 
     * @return {@link #LINUX} for all Linux/Unix systems, {@link #MACOSX} 
     *    for all versions of Mac OS X, {@link #WINDOWS} for all versions of
     *    Windows and {@link #UNSUPPORTED} for other non-supported 
     *    operating systems
     *    
     * @since 1.00
     */
    public static OsTypes getOSType() {
        if (System.getProperty("os.name").toUpperCase().contains("LINUX")) {
            return LINUX;
        } else if (System.getProperty("os.name").toUpperCase().contains(
                "WINDOWS")) {
            return WINDOWS;
        } else if (System.getProperty("os.name").toUpperCase().contains(
                "MAC OS X")) {
            return MACOSX;
        } else {
            return UNSUPPORTED;
        }
    }

    /**
     * Returns if this program is running under Eclipse. Requires that Eclipse
     * is really running!
     * 
     * @return <code>true</code> if the program is running under Eclipse,
     *         <code>false</code> else
     * 
     * @since 2.00
     */
    @SuppressWarnings("unchecked")
    public static boolean isEclipse() {
        // copied from SvnKit
        if (isEclipse == null) {
            try {
                Class platform = OsTypes.class.getClassLoader().loadClass(
                        "org.eclipse.core.runtime.Platform");
                Method isRunning = platform
                        .getMethod("isRunning", new Class[0]);
                Object result = isRunning.invoke(null, new Object[0]);
                if (result != null && Boolean.TRUE.equals(result)) {
                    isEclipse = Boolean.TRUE;
                    return true;
                }
            } catch (Throwable th) {
            }
            isEclipse = Boolean.FALSE;
        }
        return isEclipse.booleanValue();
    }

}
