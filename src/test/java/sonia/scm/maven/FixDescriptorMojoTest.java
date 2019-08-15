package sonia.scm.maven;

import org.apache.maven.model.Developer;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
  private Element information;

  @BeforeEach
  void setUpNodes() throws Exception {
    document = createDocument();
    information = document.createElement("information");
    document.appendChild(information);
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
  void shouldAppendElements()  {
    setUpProject();

    mojo.fixPluginInformation(document, information);

    verify( "name", "scm-cas-plugin");
    verify( "version", "1.0.0");
    verify( "displayName", "CAS");
    verify( "description", "CAS Authentication");
    verify( "author", "Sebastian Sdorra");
  }

  @Test
  void shouldNotOverrideElements() {
    appendNode("name", "scm-ldap-plugin");
    appendNode("version", "2.0.0");
    appendNode("displayName", "LDAP");
    appendNode("description", "LDAP Authentication");
    appendNode( "author", "Thorsten Ludewig");

    mojo.fixPluginInformation(document, information);

    verify("name", "scm-ldap-plugin");
    verify("version", "2.0.0");
    verify("displayName", "LDAP");
    verify("description", "LDAP Authentication");
    verify( "author", "Thorsten Ludewig");
  }

  private void verify(String name, String expectedValue) {
    NodeList elements = information.getElementsByTagName(name);
    String value = null;
    for (int i=0; i<elements.getLength(); i++) {
      Node element = elements.item(i);
      value = element.getTextContent();
    }

    assertThat(expectedValue).isEqualTo(value);
  }



  private void appendNode(String name, String value) {
    Element element = document.createElement(name);
    element.setTextContent(value);
    information.appendChild(element);
  }

  private Document createDocument() throws ParserConfigurationException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
  }

}
