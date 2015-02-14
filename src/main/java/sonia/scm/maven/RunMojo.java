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
import com.google.common.collect.Lists;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.archiver.UnArchiver;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy
  .WebSocketServerContainerInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sonia.scm.maven.lr.LiveReloadContext;
import sonia.scm.maven.lr.LiveReloadEndPoint;
import sonia.scm.maven.lr.LiveReloadWatcher;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Desktop;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "run",
  defaultPhase = LifecyclePhase.COMPILE,
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class RunMojo extends AbstractPackagingMojo
{

  /** Field description */
  private static final String DIRECTORY_PLUGINS = "plugins";

  /** Field description */
  private static final int HEADERBUFFERSIZE = 16384;

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
   * @throws MojoFailureException
   */
  @Override
  protected void doExecute(File descriptor)
    throws MojoExecutionException, MojoFailureException
  {
    File pluginDirectory = new File(scmHome, DIRECTORY_PLUGINS);
    File exploded = new File(pluginDirectory,
                      createPluginPath(project.getArtifact()));

    // TODO check for version updates
    // TODO transitive smp dependencies
    Set<ArtifactItem> smps = SmpDependencyCollector.collect(project);

    for (ArtifactItem smp : smps)
    {
      install(pluginDirectory, smp);
    }

    try
    {
      if (link)
      {
        createExplodedLinked(exploded, descriptor, smps);
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
    runServletContainer(warFile);
  }

  /**
   * Method description
   *
   *
   * @param target
   * @param descriptor
   * @param smpDeps
   *
   * @throws IOException
   * @throws MojoExecutionException
   */
  private void createExplodedLinked(File target, File descriptor,
    Set<ArtifactItem> smpDeps)
    throws IOException, MojoExecutionException
  {
    logger.info("create exploded linked smp at {}", target);
    copyDescriptor(target, descriptor);

    if (classesDirectory.exists())
    {
      Path classesLink = new File(target, DIRECTORY_CLASSES).toPath();

      if (!Files.isSymbolicLink(classesLink))
      {

        if (Files.exists(classesLink))
        {
          delete(classesLink);
        }

        logger.debug("link classes directory {} to {}", classesDirectory,
          classesLink);

        Files.createSymbolicLink(classesLink, classesDirectory.toPath());
      }
    }

    if (webappDirectory.exists())
    {
      Path webappLink = new File(target, DIRECTORY_WEBAPP).toPath();

      if (Files.exists(webappLink))
      {
        delete(webappLink);
      }

      if (!Files.isSymbolicLink(webappLink))
      {
        logger.debug("link webapp directory {} to {}", webappDirectory,
          webappLink);

        Files.createSymbolicLink(webappLink, webappDirectory.toPath());
      }
    }

    copyDependencies(new File(target, DIRECTORY_LIB), smpDeps);
  }

  /**
   * Method description
   *
   *
   * @return
   *
   * @throws DeploymentException
   * @throws IOException
   * @throws ServletException
   */
  private ServletContextHandler createLiveReloadContext(Server server)
    throws IOException, ServletException, DeploymentException
  {

    // enable livereload
    LiveReloadContext.init(webappDirectory.toPath());

    logger.info("start LiveReloadFSWatcher thread");

    Thread thread = new Thread(new LiveReloadWatcher(), "LiveReloadFSWatcher");

    thread.start();

    ServletContextHandler context =
      new ServletContextHandler(ServletContextHandler.SESSIONS);

    context.setContextPath("/");
    context.setServer(server);

    ServerContainer wsc =
      WebSocketServerContainerInitializer.configureContext(context);

    wsc.addEndpoint(LiveReloadEndPoint.class);

    return context;
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
   * @param warFile
   *
   * @return
   */
  private WebAppContext createScmContext(File warFile)
  {
    WebAppContext warContext = new WebAppContext();

    warContext.setContextPath(contextPath);
    warContext.setExtractWAR(true);
    warContext.setWar(warFile.getAbsolutePath());

    return warContext;
  }

  /**
   * Method description
   *
   *
   * @param server
   *
   * @return
   */
  private ServerConnector createServerConnector(Server server)
  {
    ServerConnector connector = new ServerConnector(server);
    HttpConfiguration cfg = new HttpConfiguration();

    cfg.setRequestHeaderSize(HEADERBUFFERSIZE);
    cfg.setResponseHeaderSize(HEADERBUFFERSIZE);

    List<ConnectionFactory> factories = Lists.newArrayList();

    factories.add(new HttpConnectionFactory(cfg));
    connector.setConnectionFactories(factories);

    if (openBrowser && Desktop.isDesktopSupported())
    {
      connector.addLifeCycleListener(new OpenBrowserListener(port,
        contextPath));
    }

    connector.setPort(port);

    return connector;
  }

  /**
   * Method description
   *
   *
   * @param path
   *
   * @throws IOException
   */
  private void delete(Path path) throws IOException
  {
    logger.debug("delete {}", path);

    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
    {
      FileUtils.deleteDirectory(path.toFile());
    }
    else
    {
      Files.delete(path);
    }
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
  private void runServletContainer(File warFile) throws MojoExecutionException
  {
    logger.info("start servletcontainer at port " + port);

    try
    {
      System.setProperty("scm.home", scmHome.getPath());
      System.setProperty("scm.stage", stage);
      logger.info("set stage {}", stage);

      // enable debug logging
      System.setProperty("logback.configurationFile", loggingConfiguration);

      Server server = new Server();

      server.addConnector(createServerConnector(server));

      ContextHandlerCollection col = new ContextHandlerCollection();

      //J-
      col.setHandlers(new Handler[]{
        createScmContext(warFile), 
        createLiveReloadContext(server)
      });
      //J+
      server.setHandler(col);

      // server.setHandler(warContext);
      new StopMonitorThread(server, stopPort, stopKey).start();
      server.start();

      logger.info("scm-server is now accessible at http://localhost:{}{}",
        port, contextPath);
      logger.info("livereload is available at ws://localhost:{}/livereload",
        port);

      if (!backgroud)
      {
        server.join();
      }
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("could not start servletcontainer", ex);
    }
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

    File warFile = checkAndResolve(convertToArtifact(webApplication));

    return warFile;
  }

  //~--- fields ---------------------------------------------------------------

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
