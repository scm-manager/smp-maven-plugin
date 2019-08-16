package sonia.scm.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

class XmlNodes {

  private XmlNodes() {
  }

  static boolean hasChild(Node parent, String name) {
    return getChild(parent, name) != null;
  }

  static Node getChild(Node parent, String name) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);

      if (name.equals(child.getNodeName())) {
        return child;
      }
    }
    return null;
  }

  static void appendIfNotExists(Document document, Node informationNode, String name, String artifactId) {
    if (!hasChild(informationNode, name)) {
      appendNode(document, informationNode, name, artifactId);
    }
  }

  static void writeDocument(File descriptor, Document document)
    throws MojoExecutionException {
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(new DOMSource(document),
        new StreamResult(descriptor));
    } catch (IllegalArgumentException | TransformerException ex) {
      throw new MojoExecutionException("could not write plugin descriptor", ex);
    }
  }


  static void appendNode(Document document, Node parent, String name, String value) {
    if (value != null) {
      Element node = document.createElement(name);

      node.setTextContent(value);
      parent.appendChild(node);
    }
  }

  static Document createDocument(File descriptor) throws MojoExecutionException {
    try {
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(descriptor);
    } catch (IOException | ParserConfigurationException | SAXException ex) {
      throw new MojoExecutionException("could not parse plugin descriptor", ex);
    }
  }
}
