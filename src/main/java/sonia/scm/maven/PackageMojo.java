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

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.ManifestException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.war.WarArchiver;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.Locale;
import java.util.Set;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "package",
  defaultPhase = LifecyclePhase.PACKAGE,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class PackageMojo extends AbstractDescriptorMojo
{

  /** Field description */
  private static final Set<String> EXCLUDED_SCOPES =
    ImmutableSet.of("provided", "test");

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
   * @param artifactResolver
   */
  public void setArtifactResolver(ArtifactResolver artifactResolver)
  {
    this.artifactResolver = artifactResolver;
  }

  /**
   * Method description
   *
   *
   * @param classesDirectory
   */
  public void setClassesDirectory(File classesDirectory)
  {
    this.classesDirectory = classesDirectory;
  }

  /**
   * Method description
   *
   *
   * @param localRepository
   */
  public void setLocalRepository(ArtifactRepository localRepository)
  {
    this.localRepository = localRepository;
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
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  protected void doExecute(File descriptor)
    throws MojoExecutionException, MojoFailureException
  {
    if (!descriptor.exists())
    {
      throw new MojoFailureException(
        "could not find plugin descriptor at ".concat(descriptor.getPath()));
    }

    try
    {
      WarArchiver archiver = createWarArchiver();

      resolve(archiver, getRuntimeDependencies());

      if (isDirectory(classesDirectory))
      {
        archiver.addClasses(classesDirectory, new String[0], new String[0]);
      }

      if (isDirectory(webappDirectory))
      {
        archiver.addDirectory(webappDirectory);
      }

      archiver.addFile(descriptor, PLUGIN_DESCRIPTOR);
      archiver.setDestFile(outputPackage);

      MavenArchiver mavenArchiver = new MavenArchiver();

      mavenArchiver.setArchiver(archiver);
      mavenArchiver.setOutputFile(outputPackage);
      mavenArchiver.createArchive(project, new MavenArchiveConfiguration());

      project.getArtifact().setFile(outputPackage);

    }
    catch (NoSuchArchiverException ex)
    {
      throw new MojoExecutionException("unable to find war archiver", ex);
    }
    catch (ArchiverException ex)
    {
      throw new MojoExecutionException("could not create archive", ex);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("could not create archive", ex);
    }
    catch (ManifestException ex)
    {
      throw new MojoExecutionException(
        "could not attach maven metadata to archive", ex);
    }
    catch (DependencyResolutionRequiredException ex)
    {
      throw new MojoExecutionException(
        "could not attach maven metadata to archive", ex);
    }
  }

  /**
   * Method description
   *
   *
   * @return
   *
   * @throws MojoFailureException
   * @throws NoSuchArchiverException
   */
  private WarArchiver createWarArchiver()
    throws NoSuchArchiverException, MojoFailureException
  {
    Archiver a = archiverManager.getArchiver("war");

    if (!(a instanceof WarArchiver))
    {
      throw new MojoFailureException(
        "archiver is not an instance of WarArchiver");
    }

    WarArchiver archiver = (WarArchiver) a;

    // false works ??
    archiver.setIgnoreWebxml(false);

    return archiver;
  }

  /**
   * Method description
   *
   *
   * @param archiver
   * @param artifacts
   */
  private void resolve(WarArchiver archiver, Iterable<Artifact> artifacts)
  {
    for (Artifact artifact : artifacts)
    {
      resolve(archiver, artifact);
    }
  }

  /**
   * Method description
   *
   *
   * @param archiver
   * @param artifact
   */
  private void resolve(WarArchiver archiver, Artifact artifact)
  {
    ArtifactResolutionRequest request = new ArtifactResolutionRequest();

    request.setArtifact(artifact);
    request.setRemoteRepositories(project.getRemoteArtifactRepositories());
    request.setLocalRepository(localRepository);

    artifactResolver.resolve(request);

    archiver.addLib(artifact.getFile());
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   */
  private Iterable<Artifact> getRuntimeDependencies()
  {
    return Iterables.filter(project.getArtifacts(), new Predicate<Artifact>()
    {

      @Override
      public boolean apply(Artifact input)
      {
        //J-
        return ! EXCLUDED_SCOPES.contains(
          Strings.nullToEmpty(input.getScope()).toLowerCase(Locale.ENGLISH)
        );
        //J+
      }
    });
  }

  /**
   * Method description
   *
   *
   * @param dir
   *
   * @return
   */
  private boolean isDirectory(File dir)
  {
    return dir.exists() && dir.isDirectory();
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Component
  private ArchiverManager archiverManager;

  /** Field description */
  @Component
  private ArtifactResolver artifactResolver;

  /** Field description */
  @Parameter(defaultValue = "${project.build.outputDirectory}")
  private File classesDirectory;

  /** Field description */
  @Parameter(defaultValue = "${localRepository}")
  private ArtifactRepository localRepository;

  /** Field description */
  @Parameter(
    defaultValue = "${project.build.directory}/${project.name}-${project.version}.smp")
  private File outputPackage;

  /** Field description */
  @Component
  private MavenProject project;
}
