/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.realm;

import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import winstone.Logger;
import winstone.WinstoneException;
import winstone.WinstoneResourceBundle;
import winstone.cmdline.Option;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static org.eclipse.jetty.util.security.Credential.*;

/**
 * @author rickk
 * @version $Id: FileRealm.java,v 1.4 2006/08/30 04:07:52 rickknowles Exp $
 */
public class FileRealm extends HashLoginService {

    private static final WinstoneResourceBundle REALM_RESOURCES = new WinstoneResourceBundle("winstone.realm.LocalStrings");

    static final String DEFAULT_FILE_NAME = "users.xml";
    static final String ELEM_USER = "user";
    static final String ATT_USERNAME = "username";
    static final String ATT_PASSWORD = "password";
    static final String ATT_ROLELIST = "roles";

    /**
     * Constructor - this sets up an authentication realm, using the file
     * supplied on the command line as a source of userNames/passwords/roles.
     */
    public FileRealm(Map args) {

        UserStore userStore = new UserStore();
        setUserStore(userStore);

        // Get the filename and parse the xml doc
        File realmFile = getRealmFile(args);

        try (InputStream inFile = new FileInputStream(realmFile)){
            int count=0;
            Document doc = this.parseStreamToXML(inFile);
            Node rootElm = doc.getDocumentElement();
            for (int n = 0; n < rootElm.getChildNodes().getLength(); n++) {
                Node child = rootElm.getChildNodes().item(n);

                if ((child.getNodeType() == Node.ELEMENT_NODE)
                        && (child.getNodeName().equals(ELEM_USER))) {
                    String userName = null;
                    String password = null;
                    String roleList = null;
                    // Loop through for attributes
                    for (int j = 0; j < child.getAttributes().getLength(); j++) {
                        Node thisAtt = child.getAttributes().item(j);
                        if (thisAtt.getNodeName().equals(ATT_USERNAME))
                            userName = thisAtt.getNodeValue();
                        else if (thisAtt.getNodeName().equals(ATT_PASSWORD))
                            password = thisAtt.getNodeValue();
                        else if (thisAtt.getNodeName().equals(ATT_ROLELIST))
                            roleList = thisAtt.getNodeValue();
                    }

                    if ((userName == null) || (password == null)
                            || (roleList == null))
                        Logger.log(Logger.FULL_DEBUG, REALM_RESOURCES,
                                "FileRealm.SkippingUser", userName);
                    else {
                        userStore.addUser(userName, getCredential(password), getRoleList(roleList));
                        count++;
                    }
                }
            }
            Logger.log(Logger.DEBUG, REALM_RESOURCES, "FileRealm.Initialised",
                    "" + count);
        } catch (IOException err) {
            throw new WinstoneException(REALM_RESOURCES
                    .getString("FileRealm.ErrorLoading"), err);
        }
    }

    /**
     * Get a parsed XML DOM from the given inputstream. Used to process the
     * web.xml application deployment descriptors.
     */
    private Document parseStreamToXML(InputStream in) {
        try {
            // Use JAXP to create a document builder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setCoalescing(true);
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(in);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new WinstoneException(REALM_RESOURCES
                    .getString("FileRealm.XMLParseError"), e);
        }
    }

    private File getRealmFile(Map args) {
        File realmFile = Option.FILEREALM_CONFIGFILE.get(args);
        if (realmFile==null){
            realmFile = new File(DEFAULT_FILE_NAME);
        }
        if (!realmFile.exists()) {
            throw new WinstoneException(REALM_RESOURCES.getString(
                    "FileRealm.FileNotFound", realmFile.getPath()));
        }
        return realmFile;
    }

    /***
     * Parse the role list into an array and sort it
     * @param roleList
     * @return
     */
    private String[] getRoleList(String roleList) {
        StringTokenizer st = new StringTokenizer(roleList, ",");
        List<String> rl = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String currentRole = st.nextToken();
            rl.add(currentRole);
        }
        String[] roleArray = rl.toArray(new String[rl.size()]);
        Arrays.sort(roleArray);
        return roleArray;
    }

}
