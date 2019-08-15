/**
 * Copyright (c) 2014, Sebastian Sdorra
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * https://bitbucket.org/sdorra/smp-maven-plugin
 */


package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

@Mojo(name = "fix-descriptor", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class FixDescriptorMojo extends AbstractDescriptorMojo {

  private static final String SCM_VERSION = "2";

  @Component
  private MavenProject project;

  @Override
  protected void execute(File descriptor)
    throws MojoExecutionException {
    if (descriptor.exists() && descriptor.isFile()) {
      Document document = createDocument(descriptor);

      fixDescriptor(document);
      writeDocument(descriptor, document);
    } else {
      getLog().warn("no plugin descriptor found, skipping fix-descriptor goal");
    }
  }

  public void setProject(MavenProject project) {
    this.project = project;
  }

  private void appendNode(Document document, Node parent, String name,
                          String value) {
    if (value != null) {
      Element node = document.createElement(name);

      node.setTextContent(value);
      parent.appendChild(node);
    }
  }

  private Document createDocument(File descriptor) throws MojoExecutionException {
    Document document = null;

    try {
      document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
          descriptor);
    } catch (IOException | ParserConfigurationException | SAXException ex) {
      throw new MojoExecutionException("could not parse plugin descriptor", ex);
    }

    return document;
  }

  private void fixDescriptor(Document document) {
    Element rootElement = document.getDocumentElement();

    fixRootElement(document, rootElement);

    NodeList informationNodeList = rootElement.getElementsByTagName("information");
    Node informationNode = null;

    for (int i = 0; i < informationNodeList.getLength(); i++) {
      Node node = informationNodeList.item(i);

      if ("information".equals(node.getNodeName())) {
        informationNode = node;

        break;
      }
    }

    if (informationNode == null) {
      informationNode = document.createElement("information");
      rootElement.appendChild(informationNode);
    }

    fixPluginInformation(document, informationNode);
  }

  @VisibleForTesting
  void fixPluginInformation(Document document, Node informationNode) {
    // Map artifactId to name
    appendIfNotExists(document, informationNode, "name", project.getArtifactId());
    appendIfNotExists(document, informationNode, "version", project.getVersion());
    // Map name to displayName
    appendIfNotExists(document, informationNode, "displayName", project.getName());
    appendIfNotExists(document, informationNode, "description", project.getDescription());
    appendIfNotExists(document, informationNode, "author", getFirstDeveloper());
  }

  private void appendIfNotExists(Document document, Node informationNode, String name, String artifactId) {
    if (!hasChild(informationNode, name)) {
      appendNode(document, informationNode, name, artifactId);
    }
  }

  private String getFirstDeveloper() {
    List<Developer> developers = project.getDevelopers();
    if (developers != null && !developers.isEmpty()) {
      return developers.get(0).getName();
    }
    return null;
  }

  private boolean hasChild(Node parent, String name) {
    NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);

      if (name.equals(child.getNodeName())) {
        return true;
      }
    }
    return false;
  }

  private void fixRootElement(Document document, Element rootElement) {
    NodeList children = rootElement.getChildNodes();
    boolean scmVersion = false;

    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      String nodeName = node.getNodeName();

      if (!Strings.isNullOrEmpty(nodeName)) {
        switch (nodeName) {
          case "scm-version":
            scmVersion = true;

            break;
        }
      }
    }

    if (!scmVersion) {
      Element scmVersionEl = document.createElement("scm-version");

      scmVersionEl.setTextContent(SCM_VERSION);
      rootElement.appendChild(scmVersionEl);
    }
  }

  private void writeDocument(File descriptor, Document document)
    throws MojoExecutionException {
    try {
      Transformer transformer =
        TransformerFactory.newInstance().newTransformer();

      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(new DOMSource(document),
        new StreamResult(descriptor));
    } catch (IllegalArgumentException | TransformerException ex) {
      throw new MojoExecutionException("could not write plugin descriptor", ex);
    }
  }
}
