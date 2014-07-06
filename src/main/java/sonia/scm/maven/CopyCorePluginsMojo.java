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

import com.google.common.base.Charsets;
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
import java.io.IOException;

import java.util.List;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(name = "copy-core-plugins", defaultPhase = LifecyclePhase.COMPILE)
public class CopyCorePluginsMojo extends AbstractSmpMojo
{

  /** Field description */
  private static final String FILE_PLUGININDEX = "plugin.idx";

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

    StringBuilder plugins = new StringBuilder();

    for (ArtifactItem artifactItem : artifactItems)
    {
      File artifactFile = resolve(artifactItem);

      if ((artifactFile != null) && artifactFile.exists())
      {
        plugins.append(artifactFile.getName()).append('\n');
        copy(artifactFile);
      }
      else
      {
        logger.warn("could not resolve file for {}",
          artifactItem.getArtifactId());
      }
    }

    try
    {
      //J-
      Files.write(
        plugins.toString(), 
        new File(outputDirectory, FILE_PLUGININDEX),
        Charsets.UTF_8
      );
      //J+
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("could not create plugin index", ex);
    }
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
