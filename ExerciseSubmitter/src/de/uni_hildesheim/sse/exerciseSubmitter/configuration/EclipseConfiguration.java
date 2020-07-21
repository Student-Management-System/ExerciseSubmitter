package de.uni_hildesheim.sse.exerciseSubmitter.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.uni_hildesheim.sse.exerciseSubmitter.eclipse.util.GuiUtils;
import de.uni_hildesheim.sse.exerciseSubmitter.submission.
    CommunicationException;

/**
 * A special class for configuration access in the execution environment. Use
 * {@link IConfiguration#INSTANCE} to retrieve an instance.
 * 
 * @author eichelberger
 * @since 2.00
 * @version 2.20
 */
class EclipseConfiguration extends AbstractUserConfiguration {

    /**
     * Stores the secure preferences instances where to store passwords to.
     * 
     * @since 2.20
     */
    private ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();

    
    /**
     * Creates a new eclipse configuration.
     * 
     * @since 2.00
     */
    public EclipseConfiguration() {
        try {
            userName = securePreferences.get(PreferenceConstants.USERNAME, "");
        } catch (StorageException se) {
        }
        try {
            password = securePreferences.get(PreferenceConstants.PASSWORD, "");
        } catch (StorageException se) {
        }
    }

    /**
     * Is called to generically handle errors.
     * 
     * @param exception
     *            the (typed) exception denoting the reason
     * 
     * @since 2.00
     */
    @Override
    protected void handleError(CommunicationException exception) {
        GuiUtils.handleThrowable(exception);
    }

    /**
     * Stores the user name in the user local configuration. A local
     * configuration must not exist.
     * 
     * @since 2.00
     */
    @Override
    public void store() {
        try {
            securePreferences.put(PreferenceConstants.USERNAME, userName, false);
        } catch (StorageException se) {
        }
        try {
            securePreferences.put(PreferenceConstants.PASSWORD, password, true); 
        } catch (StorageException se) {
        }
    }
    
    /**
     * Adjusts the Eclipse project file to reflect the appropriate
     * project name.
     * 
     * @param targetDir the entire path to the eclipse directory
     * 
     * @since 2.00
     */
    public void adjustFilesAfterReplay(File targetDir) {
        String intendedProjectName = targetDir.getName();
        File projectFile = new File(targetDir, ".project");
        if (projectFile.exists()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                boolean modified = false;
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(projectFile);
                Node rootNode = document.getDocumentElement();
                NodeList children = rootNode.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeName().equals("name")) {
                        String projectName = child.getTextContent().trim();
                        if (!projectName.equals(intendedProjectName)) {
                            child.setTextContent(intendedProjectName);
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    DOMSource source = new DOMSource(document);
                    FileOutputStream os = new FileOutputStream(projectFile);
                    StreamResult result = new StreamResult( os );
                    transformer.transform(source, result);
                }
            } catch (TransformerConfigurationException e) {
            } catch (TransformerException e) {
            } catch (ParserConfigurationException e) {
            } catch (SAXException e) {
            } catch (IOException e) {
            }
        }
    }

}
