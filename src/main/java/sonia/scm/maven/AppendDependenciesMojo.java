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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.xml.sax.SAXException;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
  private static final String ELEMENT_DEPENDENCY = "dependency";

  /**
   * the logger for AppendDependenciesMojo
   */
  private static final Logger logger =
    LoggerFactory.getLogger(AppendDependenciesMojo.class);

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   *
   * @param descriptor
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  public void doExecute(File descriptor)
    throws MojoExecutionException, MojoFailureException
  {
    if (descriptor.exists())
    {
      Set<ArtifactItem> dependencies = SmpDependencyCollector.collect(project);

      if (dependencies.isEmpty())
      {
        logger.info("no plugin dependencies found");
      }
      else
      {
        logger.info("update plugin descriptor");

        try
        {
          rewritePluginDescriptor(descriptor, dependencies);
        }
        catch (SAXException | IOException | ParserConfigurationException
          | TransformerException ex)
        {
          logger.error("could not rewrite plugin descriptor with dependencies",
            ex);
        }
      }
    }
    else
    {
      logger.warn(
        "no plugin descriptor found, skipping append-dependencies goal");
    }
  }

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param project
   */
  public void setProject(MavenProject project)
  {
    this.project = project;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   *
   * @param descriptor
   * @param dependencies
   *
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws TransformerConfigurationException
   * @throws TransformerException
   */
  private void rewritePluginDescriptor(File descriptor,
    Set<ArtifactItem> dependencies)
    throws SAXException, IOException, ParserConfigurationException,
    TransformerConfigurationException, TransformerException
  {
    DocumentBuilder builder =
      DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.parse(descriptor);
    Element dependenciesEl = doc.createElement(ELEMENT_DEPENDENCIES);

    doc.getDocumentElement().appendChild(dependenciesEl);

    for (ArtifactItem item : dependencies)
    {
      if (!item.isSelf(project))
      {
        Element dependencyEl = doc.createElement(ELEMENT_DEPENDENCY);

        dependencyEl.setTextContent(item.getId());
        dependenciesEl.appendChild(dependencyEl);
      }
    }

    Transformer transformer = TransformerFactory.newInstance().newTransformer();

    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.transform(new DOMSource(doc), new StreamResult(descriptor));
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Component
  private MavenProject project;
}
