package sonia.scm.maven;

import com.google.common.base.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.util.Optional;

public class PluginDescriptor {

  private static final String ELEMENT_NAME = "name";
  private static final String ELEMENT_INFORMATION = "information";
  private static final String ELEMENT_CONDITIONS = "conditions";
  private static final String ELEMENT_MIN_VERSION = "min-version";

  private final Document document;

  private PluginDescriptor(Document document) {
    this.document = document;
  }

  public static PluginDescriptor from(File file) throws MojoExecutionException {
    return new PluginDescriptor(XmlNodes.createDocument(file));
  }

  public static PluginDescriptor from(byte[] bytes) throws MojoExecutionException {
    return new PluginDescriptor(XmlNodes.createDocument(bytes));
  }

  public static PluginDescriptor from(Document document) {
    return new PluginDescriptor(document);
  }

  public String findName() throws MojoExecutionException {
    Node information = XmlNodes.getChild(document.getDocumentElement(), ELEMENT_INFORMATION);
    if (information != null) {
      Node name = XmlNodes.getChild(information, ELEMENT_NAME);
      if (name != null) {
        return name.getTextContent();
      }
    }
    throw new MojoExecutionException("found plugin descriptor without name");
  }

  public Optional<String> findMinVersion() {
    Node conditions = XmlNodes.getChild(document.getDocumentElement(), ELEMENT_CONDITIONS);
    if (conditions != null) {
      Node minVersion = XmlNodes.getChild(conditions, ELEMENT_MIN_VERSION);
      if (minVersion != null && !Strings.isNullOrEmpty(minVersion.getTextContent())) {
        return Optional.of(minVersion.getTextContent());
      }
    }
    return Optional.empty();
  }
}
