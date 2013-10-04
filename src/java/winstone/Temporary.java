package winstone;

import org.w3c.dom.Node;

/**
 * Meant to be removed by the time Jetty migration is completed.
 *
 * @author Kohsuke Kawaguchi
 * @deprecated
 */
public class Temporary {
    public static String getTextFromNode(Node node) {
        if (node == null) {
            return null;
        }
        Node child = node.getFirstChild();
        if (child == null) {
            return "";
        }
        String textNode = child.getNodeValue();
        if (textNode == null) {
            return "";
        } else {
            return textNode.trim();
        }
    }
}
