/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Desktop;

import java.io.File;
import java.io.IOException;

import java.util.List;
import java.util.Set;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "run",
  defaultPhase = LifecyclePhase.PACKAGE,
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
    File exploded = new File(pluginDirectory, createPluginPath());
    Set<ArtifactItem> smps = SmpDependencyCollector.collect(project);

    for (ArtifactItem smp : smps)
    {
      install(pluginDirectory, smp);
    }

    try
    {
      createExploded(exploded, descriptor, smps);
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
   * @return
   */
  private String createPluginPath()
  {
    StringBuilder path = new StringBuilder();

    path.append(File.separator).append(project.getGroupId());
    path.append(File.separator).append(project.getArtifactId());

    return path.toString();
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
      throw new MojoExecutionException(
        "smp dependency installation is not yet implement");
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
      server.addConnector(connector);

      WebAppContext warContext = new WebAppContext();

      warContext.setContextPath(contextPath);
      warContext.setExtractWAR(true);
      warContext.setWar(warFile.getAbsolutePath());
      server.setHandler(warContext);
      new StopMonitorThread(server, stopPort, stopKey).start();
      server.start();

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

    Artifact artifact = system.createArtifact(webApplication.getGroupId(),
                          webApplication.getArtifactId(),
                          webApplication.getVersion(), "",
                          webApplication.getType());

    File warFile = resolve(artifact);

    if ((warFile == null) ||!warFile.exists())
    {
      throw new MojoExecutionException("could not find webapp artifact file");
    }

    return warFile;
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Parameter
  private boolean backgroud = false;

  /** Field description */
  @Parameter
  private String contextPath = "/scm";

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
  @Component
  private RepositorySystem system;
}
