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

import com.github.sdorra.buildfrontend.AbstractNodeMojo;
import com.github.sdorra.buildfrontend.GulpMojo;
import com.github.sdorra.buildfrontend.NpmInstallMojo;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import org.codehaus.plexus.archiver.manager.ArchiverManager;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.net.URL;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(name = "compress-webapp-resources",
  defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class CompressWebAppResourcesMojo extends AbstractWebappMojo
{

  /** Field description */
  private static final String FILE_GULPFILE = "gulpfile.js";

  /** Field description */
  private static final String FILE_PACKAGE_JSON = "package.json";

  /** Field description */
  private static final String RESOURCE_GULPFILE = "sonia/scm/maven/gulpfile.js";

  /** Field description */
  private static final String RESOURCE_PACKAGE_JSON =
    "sonia/scm/maven/package.json";

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param archiverManager
   */
  public void setArchiverManager(ArchiverManager archiverManager)
  {
    this.archiverManager = archiverManager;
  }

  /**
   * Method description
   *
   *
   * @param buildDirectory
   */
  public void setBuildDirectory(String buildDirectory)
  {
    this.buildDirectory = buildDirectory;
  }

  /**
   * Method description
   *
   *
   * @param installer
   */
  public void setInstaller(ArtifactInstaller installer)
  {
    this.installer = installer;
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

  /**
   * Method description
   *
   *
   * @param repositorySystem
   */
  public void setRepositorySystem(RepositorySystem repositorySystem)
  {
    this.repositorySystem = repositorySystem;
  }

  /**
   * Method description
   *
   *
   * @param workDirectory
   */
  public void setWorkDirectory(File workDirectory)
  {
    this.workDirectory = workDirectory;
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
    copyAndFilterResource(RESOURCE_PACKAGE_JSON, FILE_PACKAGE_JSON);

    NpmInstallMojo npm = new NpmInstallMojo();

    setMojoDependencies(npm);
    npm.execute();

    copyAndFilterResource(RESOURCE_GULPFILE, FILE_GULPFILE);

    GulpMojo gulp = new GulpMojo();

    setMojoDependencies(gulp);
    gulp.execute();
  }

  /**
   * Method description
   *
   *
   * @param resource
   * @param file
   *
   * @throws MojoFailureException
   */
  private void copyAndFilterResource(String resource, String file)
    throws MojoFailureException
  {
    try
    {
      URL packageJson = Resources.getResource(resource);
      //J-
      String pkg = Resources.toString(packageJson,Charsets.UTF_8)
        .replaceAll("\\$\\{groupId\\}", project.getGroupId())
        .replaceAll("\\$\\{artifactId\\}", project.getArtifactId())
        .replaceAll("\\$\\{version\\}", project.getVersion())
        .replaceAll("\\$\\{webappDirectory\\}", webappDirectory.getPath());
      //J+
      File packageFile = new File(workDirectory, file);

      Files.write(pkg, packageFile, Charsets.UTF_8);
    }
    catch (IOException ex)
    {
      throw new MojoFailureException("could not create ".concat(file), ex);
    }
  }

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param mojo
   */
  private void setMojoDependencies(AbstractNodeMojo mojo)
  {
    mojo.setLog(getLog());
    mojo.setPluginContext(getPluginContext());
    mojo.setBuildDirectory(buildDirectory);
    mojo.setWorkDirectory(workDirectory.getPath());
    mojo.setArchiverManager(archiverManager);
    mojo.setInstaller(installer);
    mojo.setRepositorySystem(repositorySystem);
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Component
  private ArchiverManager archiverManager;

  /** Field description */
  @Parameter(defaultValue = "${project.build.directory}/frontend")
  private String buildDirectory;

  /** Field description */
  @Component
  private ArtifactInstaller installer;

  /** Field description */
  @Component
  private MavenProject project;

  /** Field description */
  @Component
  private RepositorySystem repositorySystem;

  /** Field description */
  @Parameter(defaultValue = "${project.build.directory}")
  private File workDirectory;
}
