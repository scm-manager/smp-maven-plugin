/**
 * Copyright (c) 2014, Sebastian Sdorra All rights reserved.
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

import com.google.common.io.Files;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal
  .CollectingDependencyNodeVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Sebastian Sdorra
 */
public abstract class AbstractPackagingMojo extends AbstractDescriptorMojo
{

  /** Field description */
  protected static final String PACKAGE_JAR = "jar";

  /** Field description */
  private static final String DIRECTORY_CLASSES =
    "WEB-INF".concat(File.separator).concat("classes");

  /** Field description */
  private static final String DIRECTORY_LIB =
    "WEB-INF".concat(File.separator).concat("lib");

  /**
   * the logger for Packaging
   */
  private static final Logger logger =
    LoggerFactory.getLogger(AbstractPackagingMojo.class);

  //~--- set methods ----------------------------------------------------------

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
   * @param graphBuilder
   */
  public void setGraphBuilder(DependencyGraphBuilder graphBuilder)
  {
    this.graphBuilder = graphBuilder;
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
   * @param system
   */
  public void setSystem(RepositorySystem system)
  {
    this.system = system;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param artifact
   *
   * @return
   *
   * @throws MojoExecutionException
   */
  protected File checkAndResolve(Artifact artifact)
    throws MojoExecutionException
  {
    File file = artifact.getFile();

    if ((file == null) ||!file.exists())
    {
      logger.debug("start resolving artifact {}", artifact.getId());
      file = resolve(artifact);
    }

    if ((file == null) ||!file.exists())
    {
      throw new MojoExecutionException(
        "could not resolve artifact ".concat(artifact.getId()));
    }

    return file;
  }

  /**
   * Method description
   *
   *
   * @param item
   *
   * @return
   */
  protected Artifact convertToArtifact(ArtifactItem item)
  {
    return system.createArtifact(item.getGroupId(), item.getArtifactId(),
      item.getVersion(), "", item.getType());
  }

  /**
   * Method description
   *
   *
   * @param source
   * @param target
   *
   * @throws IOException
   */
  protected void copy(File source, File target) throws IOException
  {
    if (source.isDirectory())
    {
      mkdirs(target);

      String[] children = source.list();

      for (String child : children)
      {
        copy(new File(source, child), new File(target, child));
      }
    }
    else if (target.exists()
      && (source.lastModified() == target.lastModified()))
    {
      logger.trace("source {} has not changed we do not need to copy it",
        source);
    }
    else
    {
      Files.createParentDirs(target);
      Files.copy(source, target);

      // preserve last modified date
      target.setLastModified(source.lastModified());
    }
  }

  /**
   * Method description
   *
   *
   *
   * @param descriptor
   * @param target
   * @param smpDeps
   *
   * @throws IOException
   *
   * @throws MojoExecutionException
   */
  protected void createExploded(File target, File descriptor,
    Set<ArtifactItem> smpDeps)
    throws IOException, MojoExecutionException
  {
    logger.info("create exploded smp at {}", target);

    if (!descriptor.exists())
    {
      throw new IOException("could not find descriptor");
    }

    Files.createParentDirs(target);
    copy(descriptor, new File(target, PLUGIN_DESCRIPTOR));

    if (isDirectory(webappDirectory))
    {
      copy(webappDirectory, target);
    }
    else
    {
      logger.info("no webapp directory found");
    }

    if (isDirectory(classesDirectory))
    {
      copy(classesDirectory, new File(target, DIRECTORY_CLASSES));
    }
    else
    {
      logger.info("no classes directory found");
    }

    copyDependencies(new File(target, DIRECTORY_LIB), smpDeps);
  }

  /**
   * Method description
   *
   *
   * @param file
   *
   * @throws IOException
   */
  protected void mkdirs(File file) throws IOException
  {
    if (!file.exists() &&!file.mkdirs())
    {
      throw new IOException(
        "could not create directory ".concat(file.getPath()));
    }
  }

  /**
   * Method description
   *
   *
   * @param artifact
   *
   * @return
   */
  protected File resolve(Artifact artifact)
  {
    ArtifactResolutionRequest request = new ArtifactResolutionRequest();

    request.setArtifact(artifact);
    request.setRemoteRepositories(project.getRemoteArtifactRepositories());
    request.setLocalRepository(localRepository);

    artifactResolver.resolve(request);

    File file = artifact.getFile();

    logger.trace("resolved artifact {} to file {}", artifact.getId(), file);

    return file;
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param dir
   *
   * @return
   */
  protected boolean isDirectory(File dir)
  {
    return dir.exists() && dir.isDirectory();
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param libDirectory
   * @param items
   *
   * @throws MojoExecutionException
   */
  private void copyDependencies(File libDirectory, Set<ArtifactItem> items)
    throws MojoExecutionException
  {
    for (DependencyNode node : getDependencies(items))
    {
      Artifact artifact = node.getArtifact();

      if (PACKAGE_JAR.equals(artifact.getType()))
      {
        try
        {
          copyDependency(libDirectory, node.getArtifact());
        }
        catch (IOException ex)
        {
          throw new MojoExecutionException(
            "failed to copy dependency ".concat(artifact.getId()));
        }
      }
    }
  }

  /**
   * Method description
   *
   *
   * @param libDirectory
   * @param dependency
   *
   * @throws IOException
   */
  private void copyDependency(File libDirectory, Artifact dependency)
    throws IOException
  {
    File file = dependency.getFile();

    if ((file == null) ||!file.exists())
    {
      logger.debug("artifact {} is not resolved", dependency.getId());
      file = resolve(dependency);
    }

    copy(file, new File(libDirectory, file.getName()));
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param smpDeps
   *
   * @return
   *
   * @throws MojoExecutionException
   */
  private List<DependencyNode> getDependencies(Set<ArtifactItem> smpDeps)
    throws MojoExecutionException
  {
    //J-
    ArtifactFilter filter = new AndArtifactFilter(
      Arrays.asList(
        new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME), 
        new SmpArtifactFilter(smpDeps)
      )
    );
    //J+

    List<DependencyNode> result;

    try
    {

      CollectingDependencyNodeVisitor visitor =
        new CollectingDependencyNodeVisitor();
      DependencyNode rootNode = graphBuilder.buildDependencyGraph(project,
                                  filter);

      rootNode.accept(visitor);
      result = visitor.getNodes();
    }
    catch (DependencyGraphBuilderException ex)
    {
      throw new MojoExecutionException("could not build dependency graph", ex);
    }

    return result;
  }

  //~--- inner classes --------------------------------------------------------

  /**
   * Class description
   *
   *
   * @version        Enter version here..., 14/08/21
   * @author         Enter your name here...
   */
  private static class SmpArtifactFilter implements ArtifactFilter
  {

    /**
     * Constructs ...
     *
     *
     * @param items
     */
    public SmpArtifactFilter(Set<ArtifactItem> items)
    {
      this.items = items;
    }

    //~--- methods ------------------------------------------------------------

    /**
     * Method description
     *
     *
     * @param artifact
     *
     * @return
     */
    @Override
    public boolean include(Artifact artifact)
    {
      boolean result = true;

      for (ArtifactItem item : items)
      {
        if (artifact.getGroupId().equals(item.getGroupId())
          && artifact.getArtifactId().equals(item.getArtifactId()))
        {
          result = false;

          break;
        }
      }

      return result;
    }

    //~--- fields -------------------------------------------------------------

    /** Field description */
    private final Set<ArtifactItem> items;
  }


  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Component
  protected ArtifactResolver artifactResolver;

  /** Field description */
  @Parameter(defaultValue = "${project.build.outputDirectory}")
  protected File classesDirectory;

  /** Field description */
  @Component
  protected DependencyGraphBuilder graphBuilder;

  /** Field description */
  @Parameter(defaultValue = "${localRepository}")
  protected ArtifactRepository localRepository;

  /** Field description */
  @Component
  protected MavenProject project;

  /** Field description */
  @Component
  private RepositorySystem system;
}
