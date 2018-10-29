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

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.util.Set;

//~--- JDK imports ------------------------------------------------------------

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "package",
  defaultPhase = LifecyclePhase.PACKAGE,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class PackageMojo extends AbstractPackagingMojo
{

  /**
   * Method description
   *
   *
   * @param archiver
   */
  public void setArchiver(JarArchiver archiver)
  {
    this.archiver = archiver;
  }

  /**
   * Method description
   *
   *
   * @param buildDirectory
   */
  public void setBuildDirectory(File buildDirectory)
  {
    this.buildDirectory = buildDirectory;
  }

  /**
   * Method description
   *
   *
   * @param helper
   */
  public void setHelper(MavenProjectHelper helper)
  {
    this.helper = helper;
  }

  /**
   * Method description
   *
   *
   * @param outputClassesPackage
   */
  public void setOutputClassesPackage(File outputClassesPackage)
  {
    this.outputClassesPackage = outputClassesPackage;
  }

  /**
   * Method description
   *
   *
   * @param outputPackage
   */
  public void setOutputPackage(File outputPackage)
  {
    this.outputPackage = outputPackage;
  }

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
  protected void execute(File descriptor)
    throws MojoExecutionException, MojoFailureException
  {
    if (!descriptor.exists())
    {
      throw new MojoFailureException(
        "could not find plugin descriptor at ".concat(descriptor.getPath()));
    }

    try
    {
      packageWar(descriptor);
      packageJar(descriptor);
    }
    catch (ArchiverException | IOException ex)
    {
      throw new MojoExecutionException("could not create archive", ex);
    }
    catch (ManifestException | DependencyResolutionRequiredException ex)
    {
      throw new MojoExecutionException(
        "could not attach maven metadata to archive", ex);
    }
  }

  private void packageJar(File descriptor) throws ManifestException, IOException, DependencyResolutionRequiredException {
    if (isDirectory(classesDirectory)) {
      archiver.addDirectory(classesDirectory);
    }

    archiver.addFile(descriptor, PLUGIN_DESCRIPTOR);
    archiver.setDestFile(outputClassesPackage);

    MavenArchiver mavenArchiver = new MavenArchiver();

    mavenArchiver.setArchiver(archiver);
    mavenArchiver.setOutputFile(outputClassesPackage);
    mavenArchiver.createArchive(project, new MavenArchiveConfiguration());

    helper.attachArtifact(project, "jar", outputClassesPackage);
  }

  private void packageWar(File descriptor) throws ManifestException, IOException,
    DependencyResolutionRequiredException, MojoExecutionException
  {
    Set<ArtifactItem> items = SmpDependencyCollector.collect(project);

    createExploded(buildDirectory, descriptor, items);
    archiver.addDirectory(buildDirectory);

    MavenArchiver mavenArchiver = new MavenArchiver();

    mavenArchiver.setArchiver(archiver);
    mavenArchiver.setOutputFile(outputPackage);
    mavenArchiver.createArchive(project, new MavenArchiveConfiguration());

    project.getArtifact().setFile(outputPackage);
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Component(role = Archiver.class, hint = PACKAGE_JAR)
  private JarArchiver archiver;

  /** Field description */
  @Parameter(
    defaultValue = "${project.build.directory}/${project.name}-${project.version}")
  private File buildDirectory;

  /** Field description */
  @Component
  private MavenProjectHelper helper;

  /** Field description */
  @Parameter(
    defaultValue = "${project.build.directory}/${project.name}-${project.version}.jar")
  private File outputClassesPackage;

  /** Field description */
  @Parameter(
    defaultValue = "${project.build.directory}/${project.name}-${project.version}.smp")
  private File outputPackage;
}
