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

import com.google.common.base.Strings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.maven.lr.LiveReloadLifeCycleListener;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

//~--- JDK imports ------------------------------------------------------------

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "run",
  defaultPhase = LifecyclePhase.PACKAGE,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
@Execute(phase = LifecyclePhase.PACKAGE)
public class RunMojo extends AbstractPackagingMojo
{

  /** Field description */
  private static final String DIRECTORY_PLUGINS = "plugins";

  /**
   * the logger for RunMojo
   */
  private static final Logger logger = LoggerFactory.getLogger(RunMojo.class);

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param backgroud
   */
  public void setBackgroud(boolean backgroud)
  {
    this.backgroud = backgroud;
  }

  /**
   * Method description
   *
   *
   * @param contextPath
   */
  public void setContextPath(String contextPath)
  {
    this.contextPath = contextPath;
  }

  /**
   * Method description
   *
   *
   * @param link
   */
  public void setLink(boolean link)
  {
    this.link = link;
  }

  /**
   * Method description
   *
   *
   * @param loggingConfiguration
   */
  public void setLoggingConfiguration(String loggingConfiguration)
  {
    this.loggingConfiguration = loggingConfiguration;
  }

  /**
   * Method description
   *
   *
   * @param openBrowser
   */
  public void setOpenBrowser(boolean openBrowser)
  {
    this.openBrowser = openBrowser;
  }

  /**
   * Method description
   *
   *
   * @param port
   */
  public void setPort(int port)
  {
    this.port = port;
  }

  /**
   * Method description
   *
   *
   * @param scmHome
   */
  public void setScmHome(File scmHome)
  {
    this.scmHome = scmHome;
  }

  /**
   * Method description
   *
   *
   * @param stage
   */
  public void setStage(String stage)
  {
    this.stage = stage;
  }

  /**
   * Method description
   *
   *
   * @param stopKey
   */
  public void setStopKey(String stopKey)
  {
    this.stopKey = stopKey;
  }

  /**
   * Method description
   *
   *
   * @param stopPort
   */
  public void setStopPort(int stopPort)
  {
    this.stopPort = stopPort;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param descriptor
   *
   * @throws MojoExecutionException
   */
  @Override
  protected void execute(File descriptor)
    throws MojoExecutionException {
    File pluginDirectory = new File(scmHome, DIRECTORY_PLUGINS);
    File exploded = new File(pluginDirectory, createPluginPath(project.getArtifact()));

    // TODO check for version updates
    // TODO transitive smp dependencies
    Set<ArtifactItem> smps = SmpDependencyCollector.collect(project);

    for (ArtifactItem smp : smps)
    {
      install(pluginDirectory, smp);
    }

    PluginPathResolver pathResolver = new PluginPathResolver(
            classesDirectory.toPath(), packageDirectory.toPath(), exploded.toPath()
    );

    try
    {
      if (link)
      {
        createExplodedLinked(pathResolver);
      }
      else
      {
        createExploded(exploded, descriptor, smps);
      }
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("could not create exploded smp", ex);
    }

    File warFile = getWebApplicationArchive();

    logger.info("start scm-maven-server with war {}", warFile);
    runScmServer(pathResolver, warFile);
  }


  private void createExplodedLinked(PluginPathResolver pathResolver) throws IOException {
    logger.info("create exploded linked smp at {}", pathResolver.getInstallationDirectory());
    PluginLinker linker = new PluginLinker(pathResolver);
    linker.link();
  }

  /**
   * Method description
   *
   * @param artifact
   * @return
   */
  private String createPluginPath(Artifact artifact)
  {
    StringBuilder path = new StringBuilder();

    path.append(File.separator).append(artifact.getGroupId());
    path.append(File.separator).append(artifact.getArtifactId());

    return path.toString();
  }

  /**
   * Method description
   *
   *
   * @param directory
   * @param archive
   *
   * @throws MojoExecutionException
   */
  private void extractSmp(File directory, File archive)
    throws MojoExecutionException
  {
    logger.info("extract smp dependency {} to {}", archive, directory);

    try
    {
      mkdirs(directory);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException(
        "could not create plugin directory ".concat(directory.getPath()), ex);
    }

    unArchiver.setSourceFile(archive);
    unArchiver.setDestDirectory(directory);
    unArchiver.extract();
  }

  /**
   * Method description
   *
   *
   * @param pluginDirectory
   * @param smp
   *
   * @throws MojoExecutionException
   */
  private void install(File pluginDirectory, ArtifactItem smp)
    throws MojoExecutionException
  {
    if (!smp.isSelf(project))
    {
      Artifact artifact = convertToArtifact(smp);
      File directory = new File(pluginDirectory, createPluginPath(artifact));

      if (!directory.exists())
      {
        logger.info("install smp dependency {}", artifact.getId());

        File archive = checkAndResolve(artifact);

        extractSmp(new File(pluginDirectory, createPluginPath(artifact)),
          archive);
      }
      else
      {
        logger.info("smp dependency {}, is already installed",
          artifact.getId());
      }
    }
  }

  /**
   * Method description
   *
   *
   * @param warFile
   *
   * @throws MojoExecutionException
   */
  private void runScmServer(PluginPathResolver pathResolver, File warFile) throws MojoExecutionException
  {
    ScmServer.ScmServerBuilder builder = ScmServer.builder(warFile.toPath(), scmHome.toPath())
            .withPort(port)
            .withContextPath(contextPath)
            .withBackground(backgroud)
            .withDisableCorePlugins(corePlugin)
            .withLoggingConfiguration(loggingConfiguration)
            .withStopPort(stopPort)
            .withStopKey(stopKey)
            .withStage(stage);

    if (isOpenBrowserListenerEnabled()) {
      logger.info("install open browser listener");
      builder.withListener(new OpenBrowserListener());
    }

    if (restartNotifier) {
      logger.info("install restart notifier");
      long timeout = TimeUnit.SECONDS.toMillis(restartWaitTimeout);
      builder.withListener(
          new RestartNotificationLifeCycleListener(pathResolver, contextPath + restartPath, timeout)
      );
    }

    builder.withListener(new LiveReloadLifeCycleListener(pathResolver));

    ScmServer server = builder.build();
    server.start();
  }

  private boolean isOpenBrowserListenerEnabled() {
    return openBrowser && Desktop.isDesktopSupported();
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   *
   * @throws MojoExecutionException
   */
  private File getWebApplicationArchive() throws MojoExecutionException
  {
    if (Strings.isNullOrEmpty(webApplication.getVersion()))
    {
      String version;
      MavenProject parent = project.getParent();

      if (parent != null)
      {
        version = parent.getVersion();
      }
      else
      {
        version = project.getVersion();
      }

      webApplication.setVersion(version);
    }

    return checkAndResolve(convertToArtifact(webApplication));
  }

  //~--- fields ---------------------------------------------------------------

  @Parameter
  private boolean restartNotifier = true;

  @Parameter
  private String restartPath = "/restart";

  @Parameter(property = "smp.restart.timeout", defaultValue = "1")
  private long restartWaitTimeout = 1;

  @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}")
  private File packageDirectory;

  @Parameter
  private boolean corePlugin = false;

  /** Field description */
  @Parameter
  private String contextPath = "/scm";

  /** Field description */
  @Parameter
  private boolean link = true;

  /** Field description */
  @Parameter
  private boolean backgroud = false;

  /** Field description */
  @Parameter(property = "loggingConfiguration",
    defaultValue = "/logback.default.xml")
  private String loggingConfiguration;

  /** Field description */
  @Parameter
  private int port = 8081;

  /** Field description */
  @Parameter(
    property = "scm.home",
    alias = "scmHome",
    defaultValue = "${project.build.directory}/scm-home"
  )
  private File scmHome;

  /** Field description */
  @Parameter(property = "scm.stage", defaultValue = "DEVELOPMENT")
  private String stage = "DEVELOPMENT";

  /** Field description */
  @Parameter
  private String stopKey = "stop";

  /** Field description */
  @Parameter
  private int stopPort = 8085;

  /** Field description */
  @Parameter
  private boolean openBrowser = true;

  /** Field description */
  @Parameter
  private final WebApplication webApplication = new WebApplication();

  /** Field description */
  @Component(role = UnArchiver.class, hint = PACKAGE_JAR)
  private UnArchiver unArchiver;
}
