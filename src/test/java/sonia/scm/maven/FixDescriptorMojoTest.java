package sonia.scm.maven;

import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixDescriptorMojoTest {

  @Mock
  private MavenProject project;

  @InjectMocks
  private FixDescriptorMojo mojo;

  private Document document;
  private Element plugin;

  @BeforeEach
  void setUpNodes() throws Exception {
    document = createDocument();
    plugin = document.createElement("plugin");
    document.appendChild(plugin);
  }

  private void setUpProject() {
    when(project.getArtifactId()).thenReturn("scm-cas-plugin");
    when(project.getVersion()).thenReturn("1.0.0");
    when(project.getName()).thenReturn("CAS");
    when(project.getDescription()).thenReturn("CAS Authentication");

    Developer developer = new Developer();
    developer.setName("Sebastian Sdorra");
    when(project.getDevelopers()).thenReturn(Collections.singletonList(developer));
  }

  @Test
  void shouldAppendScmVersion() {
    mojo.fixDescriptor(document);

    verify(document.getDocumentElement(), "scm-version", "2");
  }

  @Test
  void shouldAppendInformationIfNotExists() {
    setUpProject();

    mojo.fixDescriptor(document);

    Node information = XmlNodes.getChild(document.getDocumentElement(), "information");
    verify(information, "name", "scm-cas-plugin");
    verify(information, "version", "1.0.0");
    verify(information, "displayName", "CAS");
    verify(information, "description", "CAS Authentication");
    verify(information, "author", "Sebastian Sdorra");
  }

  @Nested
  class WithInformationNode {

    private Element information;

    @BeforeEach
    void setUpNodes() {
      information = document.createElement("information");
      plugin.appendChild(information);
    }

    @Test
    void shouldAppendElements() {
      setUpProject();

      mojo.fixDescriptor(document);

      verify(information, "name", "scm-cas-plugin");
      verify(information, "version", "1.0.0");
      verify(information, "displayName", "CAS");
      verify(information, "description", "CAS Authentication");
      verify(information, "author", "Sebastian Sdorra");
    }

    @Test
    void shouldNotOverrideElements() {
      appendNode(information, "name", "scm-ldap-plugin");
      appendNode(information, "version", "2.0.0");
      appendNode(information, "displayName", "LDAP");
      appendNode(information, "description", "LDAP Authentication");
      appendNode(information, "author", "Thorsten Ludewig");

      mojo.fixDescriptor(document);

      verify(information, "name", "scm-ldap-plugin");
      verify(information, "version", "2.0.0");
      verify(information, "displayName", "LDAP");
      verify(information, "description", "LDAP Authentication");
      verify(information, "author", "Thorsten Ludewig");
    }

  }

  private void verify(Node parent, String name, String expectedValue) {
    Node child = XmlNodes.getChild(parent, name);
    assertThat(child).isNotNull();
    assertThat(child.getTextContent()).isEqualTo(expectedValue);
  }

  private void appendNode(Node parent, String name, String value) {
    Element element = document.createElement(name);
    element.setTextContent(value);
    parent.appendChild(element);
  }

  private Document createDocument() throws ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
  }

}
