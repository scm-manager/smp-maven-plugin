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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "copy-core-plugins",
  defaultPhase = LifecyclePhase.COMPILE,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME
)
public class CopyCorePluginsMojo extends AbstractSmpMojo
{

  /** Field description */
  private static final String FILE_PLUGININDEX = "plugin.idx";

  /** Field description */
  private static final String TYPE = "smp";

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
    if (!outputDirectory.mkdirs())
    {
      throw new MojoExecutionException("could not create output directory");
    }

    StringBuilder plugins = new StringBuilder();

    for (Artifact artifact : project.getDependencyArtifacts())
    {
      if (TYPE.equals(artifact.getType()))
      {
        logger.info("copy core plugin {}", artifact.getId());

        File file = artifact.getFile();

        plugins.append(file.getName()).append('\n');
        copy(file);
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

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Parameter(
    required = true,
    defaultValue = "${project.build.directory}/${project.build.finalName}/WEB-INF/plugins"
  )
  private File outputDirectory;

  /** Field description */
  @Component
  private MavenProject project;
}
