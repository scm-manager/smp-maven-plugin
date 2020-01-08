/**
 * Copyright (c) 2014, Sebastian Sdorra
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
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
 *
 * https://bitbucket.org/sdorra/smp-maven-plugin
 */



package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Set;

//~--- JDK imports ------------------------------------------------------------

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "append-dependencies",
  defaultPhase = LifecyclePhase.PROCESS_CLASSES,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class AppendDependenciesMojo extends AbstractDescriptorMojo
{

  /** Field description */
  private static final String ELEMENT_DEPENDENCIES = "dependencies";

  /** Field description */
  private static final String ELEMENT_OPTIONAL_DEPENDENCIES = "optional-dependencies";

  /** Field description */
  private static final String ELEMENT_DEPENDENCY = "dependency";

  /**
   * the logger for AppendDependenciesMojo
   */
  private static final Logger LOG = LoggerFactory.getLogger(AppendDependenciesMojo.class);

  @Component
  private MavenProject project;


  public void setProject(MavenProject project)
  {
    this.project = project;
  }

  @Override
  protected void execute(File descriptor) throws MojoExecutionException {
    if (descriptor.exists()) {
      Set<SmpArtifact> dependencies = SmpDependencyCollector.collect(project);

      if (dependencies.isEmpty()) {
        LOG.info("no plugin dependencies found");
      } else {
        LOG.info("update plugin descriptor");
        rewritePluginDescriptor(descriptor, dependencies);
      }
    } else {
      LOG.warn("no plugin descriptor found, skipping append-dependencies goal");
    }
  }

  private void rewritePluginDescriptor(File descriptor, Set<SmpArtifact> dependencies) throws MojoExecutionException {
    Document doc = XmlNodes.createDocument(descriptor);

    Element root = doc.getDocumentElement();
    // drop existing dependencies node
    XmlNodes.removeNode(root, ELEMENT_DEPENDENCIES);
    XmlNodes.removeNode(root, ELEMENT_OPTIONAL_DEPENDENCIES);

    Element dependenciesEl = doc.createElement(ELEMENT_DEPENDENCIES);
    root.appendChild(dependenciesEl);

    Element optionalDependenciesEl = doc.createElement(ELEMENT_OPTIONAL_DEPENDENCIES);
    root.appendChild(optionalDependenciesEl);

    dependencies.forEach(smp -> {
      Element dependencyEl = doc.createElement(ELEMENT_DEPENDENCY);
      dependencyEl.setTextContent(smp.getPluginName());
      if (smp.isOptional()) {
        optionalDependenciesEl.appendChild(dependencyEl);
      } else {
        dependenciesEl.appendChild(dependencyEl);
      }
    });

    XmlNodes.writeDocument(descriptor, doc);
  }

}
