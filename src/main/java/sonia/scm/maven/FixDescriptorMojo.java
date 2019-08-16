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
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

import static sonia.scm.maven.XmlNodes.appendIfNotExists;

//~--- JDK imports ------------------------------------------------------------

@Mojo(name = "fix-descriptor", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class FixDescriptorMojo extends AbstractDescriptorMojo {

  private static final String SCM_VERSION = "2";
  private static final String ELEMENT_INFORMATION = "information";

  @Component
  private MavenProject project;

  public void setProject(MavenProject project) {
    this.project = project;
  }

  @Override
  protected void execute(File descriptor) throws MojoExecutionException {
    if (descriptor.exists() && descriptor.isFile()) {
      Document document = XmlNodes.createDocument(descriptor);

      fixDescriptor(document);
      XmlNodes.writeDocument(descriptor, document);
    } else {
      getLog().warn("no plugin descriptor found, skipping fix-descriptor goal");
    }
  }


  @VisibleForTesting
  void fixDescriptor(Document document) {
    Element rootElement = document.getDocumentElement();
    fixRootElement(document, rootElement);

    Node informationNode = XmlNodes.getChild(rootElement, ELEMENT_INFORMATION);
    if (informationNode == null) {
      informationNode = document.createElement(ELEMENT_INFORMATION);
      rootElement.appendChild(informationNode);
    }

    fixPluginInformation(document, informationNode);
  }

  private void fixPluginInformation(Document document, Node informationNode) {
    // Map artifactId to name
    appendIfNotExists(document, informationNode, "name", project.getArtifactId());
    appendIfNotExists(document, informationNode, "version", project.getVersion());
    // Map name to displayName
    appendIfNotExists(document, informationNode, "displayName", project.getName());
    appendIfNotExists(document, informationNode, "description", project.getDescription());
    appendIfNotExists(document, informationNode, "author", getFirstDeveloper());
  }

  private String getFirstDeveloper() {
    List<Developer> developers = project.getDevelopers();
    if (developers != null && !developers.isEmpty()) {
      return developers.get(0).getName();
    }
    return null;
  }

  private void fixRootElement(Document document, Element rootElement) {
    appendIfNotExists(document, rootElement, "scm-version", SCM_VERSION);
  }

}
