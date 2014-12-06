/**
 * Copyright (c) 2010, Sebastian Sdorra All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. Neither the name of SCM-Manager;
 * nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://bitbucket.org/sdorra/scm-manager
 *
 */



package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(name = "copy-core-plugins", defaultPhase = LifecyclePhase.COMPILE)
public class CopyCorePluginsMojo extends AbstractSmpMojo
{

  /** Field description */
  private static final String FILE_PLUGININDEX = "plugin-index.xml";

  /**
   * the logger for CopyCorePluginsMojo
   */
  private static final Logger logger =
    LoggerFactory.getLogger(CopyCorePluginsMojo.class);

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param artifactItems
   */
  public void setArtifactItems(List<ArtifactItem> artifactItems)
  {
    this.artifactItems = artifactItems;
  }

  /**
   * Method description
   *
   *
   * @param outputDirectory
   */
  public void setOutputDirectory(File outputDirectory)
  {
    this.outputDirectory = outputDirectory;
  }

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
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  protected void doExecute() throws MojoExecutionException, MojoFailureException
  {
    if (!outputDirectory.exists() &&!outputDirectory.mkdirs())
    {
      throw new MojoExecutionException("could not create output directory");
    }

    List<Plugin> plugins = Lists.newArrayList();

    for (ArtifactItem artifactItem : artifactItems)
    {
      File artifactFile = resolve(artifactItem);

      if ((artifactFile != null) && artifactFile.exists())
      {
        copy(artifactFile);
        plugins.add(new Plugin(artifactFile.getName(),
          createHash(artifactFile)));
      }
      else
      {
        logger.warn("could not resolve file for {}",
          artifactItem.getArtifactId());
      }
    }

    //J-
    JAXB.marshal(
      new PluginIndex(plugins),
      new File(outputDirectory, FILE_PLUGININDEX)
    );
    //J+
  }

  /**
   * Method description
   *
   *
   * @param file
   *
   * @return
   */
  private ByteSource asByteSource(File file)
  {
    return new FileByteSource(file);
  }

  /**
   * Method description
   *
   *
   * @param artifactItem
   *
   * @return
   */
  private Artifact convert(ArtifactItem artifactItem)
  {
    return repositorySystem.createArtifact(artifactItem.getGroupId(),
      artifactItem.getArtifactId(), artifactItem.getVersion(),
      artifactItem.getType());
  }

  /**
   * Method description
   *
   *
   * @param source
   *
   * @throws MojoExecutionException
   */
  private void copy(File source) throws MojoExecutionException
  {
    try
    {
      Files.copy(source, new File(outputDirectory, source.getName()));
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("could not copy smp dependency", ex);
    }
  }

  /**
   * Method description
   *
   *
   * @param file
   *
   * @return
   *
   * @throws MojoExecutionException
   */
  private String createHash(File file) throws MojoExecutionException
  {
    String hash;

    try
    {
      hash = asByteSource(file).hash(Hashing.sha256()).toString();
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("could not create checksum", ex);
    }

    return hash;
  }

  /**
   * Method description
   *
   *
   * @param artifactItem
   *
   * @return
   */
  private File resolve(ArtifactItem artifactItem)
  {
    Artifact artifact = convert(artifactItem);
    ArtifactResolutionRequest request = new ArtifactResolutionRequest();

    request.setArtifact(convert(artifactItem));
    request.setRemoteRepositories(project.getRemoteArtifactRepositories());
    request.setLocalRepository(localRepository);

    repositorySystem.resolve(request);

    File file = artifact.getFile();

    if (file == null)
    {
      file = new File(localRepository.getBasedir(),
        localRepository.pathOf(artifact));
    }

    return file;
  }

  //~--- inner classes --------------------------------------------------------

  /**
   * Workaround for missing methods in {@link Files} class with maven 3.0.x.
   */
  private static class FileByteSource extends ByteSource
  {

    /**
     * Constructs ...
     *
     *
     * @param file
     */
    public FileByteSource(File file)
    {
      this.file = file;
    }

    //~--- methods ------------------------------------------------------------

    /**
     * Method description
     *
     *
     * @return
     *
     * @throws IOException
     */
    @Override
    public InputStream openStream() throws IOException
    {
      return new FileInputStream(file);
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    private final File file;
  }


  /**
   * Class description
   *
   *
   * @version        Enter version here..., 14/07/06
   * @author         Enter your name here...
   */
  @XmlRootElement(name = "plugin")
  @XmlAccessorType(XmlAccessType.FIELD)
  static class Plugin
  {

    /**
     * Constructs ...
     *
     */
    public Plugin() {}

    /**
     * Constructs ...
     *
     *
     * @param name
     * @param checksum
     */
    public Plugin(String name, String checksum)
    {
      this.name = name;
      this.checksum = checksum;
    }

    //~--- get methods --------------------------------------------------------

    /**
     * Method description
     *
     *
     * @return
     */
    public String getChecksum()
    {
      return checksum;
    }

    /**
     * Method description
     *
     *
     * @return
     */
    public String getName()
    {
      return name;
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    private String checksum;

    /** Field description */
    private String name;
  }


  /**
   * Class description
   *
   *
   * @version        Enter version here..., 14/07/06
   * @author         Enter your name here...
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlRootElement(name = "plugin-index")
  static class PluginIndex
  {

    /**
     * Constructs ...
     *
     */
    public PluginIndex() {}

    /**
     * Constructs ...
     *
     *
     * @param plugins
     */
    public PluginIndex(List<Plugin> plugins)
    {
      this.plugins = plugins;
    }

    //~--- get methods --------------------------------------------------------

    /**
     * Method description
     *
     *
     * @return
     */
    public List<Plugin> getPlugins()
    {
      return plugins;
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    private List<Plugin> plugins;
  }


  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Parameter(required = true)
  private List<ArtifactItem> artifactItems;

  /** Field description */
  @Parameter(defaultValue = "${localRepository}")
  private ArtifactRepository localRepository;

  /** Field description */
  @Parameter(required = true,
    defaultValue = "${project.build.directory}/${project.build.finalName}/WEB-INF/plugins")
  private File outputDirectory;

  /** Field description */
  @Component
  private MavenProject project;

  /** Field description */
  @Component
  private RepositorySystem repositorySystem;
}
