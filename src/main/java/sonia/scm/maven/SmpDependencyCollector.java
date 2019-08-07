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

import com.google.common.io.Closeables;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 *
 * @author Sebastian Sdorra
 */
public final class SmpDependencyCollector
{

  /** Field description */
  protected static final String PLUGIN_DESCRIPTOR = "META-INF/scm/plugin.xml";

  /** Field description */
  private static final String ELEMENT_artifactId = "artifactId";

  /** Field description */
  private static final String ELEMENT_INFORMATION = "information";

  /** Field description */
  private static final String ELEMENT_VERSION = "version";

  /** Field description */
  private static final String TYPE = "smp";

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @param project
   */
  private SmpDependencyCollector(MavenProject project)
  {
    this.project = project;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param project
   *
   * @return
   *
   * @throws MojoExecutionException
   */
  public static Set<ArtifactItem> collect(MavenProject project)
    throws MojoExecutionException
  {
    return new SmpDependencyCollector(project).collectSmpDependencies();
  }

  /**
   * Method description
   *
   *
   * @param dependencies
   * @param url
   *
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  private void appendDependency(Set<ArtifactItem> dependencies, URL url)
    throws ParserConfigurationException, SAXException, IOException
  {
    DocumentBuilder builder =
      DocumentBuilderFactory.newInstance().newDocumentBuilder();
    InputStream is = null;

    try
    {
      is = url.openStream();

      Document doc = builder.parse(is);

      dependencies.add(parseDescriptor(doc));
    }
    finally
    {
      Closeables.close(is, true);
    }
  }

  /**
   * Method description
   *
   *
   * @return
   *
   * @throws MojoExecutionException
   */
  private Set<ArtifactItem> collectSmpDependencies()
    throws MojoExecutionException
  {
    Set<ArtifactItem> items = new HashSet<>();

    try
    {
      ClassLoader classLoader = ClassLoaders.createRuntimeClassLoader(project);

      collectSmpDependencies(classLoader, items);
    }
    catch (ParserConfigurationException | SAXException ex)
    {
      throw new MojoExecutionException("could not parse plugin descriptor", ex);
    }
    catch (DependencyResolutionRequiredException | IOException ex)
    {
      throw new MojoExecutionException("could not setup classloader", ex);
    }

    return items;
  }

  /**
   * Method description
   *
   *
   * @param classLoader
   * @param dependencies
   *
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  private void collectSmpDependencies(ClassLoader classLoader,
    Set<ArtifactItem> dependencies)
    throws IOException, ParserConfigurationException, SAXException
  {
    Enumeration<URL> descriptors = classLoader.getResources(PLUGIN_DESCRIPTOR);

    while (descriptors.hasMoreElements())
    {
      URL url = descriptors.nextElement();

      appendDependency(dependencies, url);
    }
  }

  /**
   * Method description
   *
   *
   * @param doc
   *
   * @return
   */
  private ArtifactItem parseDescriptor(Document doc)
  {
    ArtifactItem item = null;
    NodeList nodeList = doc.getElementsByTagName(ELEMENT_INFORMATION);

    for (int i = 0; i < nodeList.getLength(); i++)
    {
      Node node = nodeList.item(i);

      if (isElement(node, ELEMENT_INFORMATION))
      {
        item = parseInformationNode(node);

        break;
      }
    }

    return item;
  }

  /**
   * Method description
   *
   *
   * @param informationNode
   *
   * @return
   */
  private ArtifactItem parseInformationNode(Node informationNode)
  {
    String artifactId = null;
    String version = null;

    NodeList nodeList = informationNode.getChildNodes();

    for (int i = 0; i < nodeList.getLength(); i++)
    {
      Node node = nodeList.item(i);

      if (isElement(node, ELEMENT_artifactId))
      {
        artifactId = node.getTextContent();
      }
      else if (isElement(node, ELEMENT_VERSION))
      {
        version = node.getTextContent();
      }
    }

    if (artifactId == null)
    {
      throw new RuntimeException(
        "descriptor does not contain artifactId");
    }

    return new ArtifactItem("", artifactId, version, TYPE);
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param node
   * @param name
   *
   * @return
   */
  private boolean isElement(Node node, String name)
  {
    return (node.getNodeType() == Node.ELEMENT_NODE)
      && name.equals(node.getNodeName());
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private final MavenProject project;
}
