/**
 * Copyright (c) 2014, Sebastian Sdorra
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * <p>
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
 * <p>
 * https://bitbucket.org/sdorra/smp-maven-plugin
 */


package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.base.Strings;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sonia.scm.maven.lr.LiveReloadDirectoryListener;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.TimeUnit;

//~--- JDK imports ------------------------------------------------------------

/**
 * @author Sebastian Sdorra
 */
@Mojo(
  name = "run",
  requiresDependencyResolution = ResolutionScope.RUNTIME
)
public class RunMojo extends AbstractPackagingMojo {

  /**
   * Field description
   */
  private static final String DIRECTORY_PLUGINS = "plugins";

  /**
   * the logger for RunMojo
   */
  private static final Logger logger = LoggerFactory.getLogger(RunMojo.class);

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   * @param backgroud
   */
  public void setBackgroud(boolean backgroud) {
    this.backgroud = backgroud;
  }

  /**
   * Method description
   *
   * @param contextPath
   */
  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  /**
   * Method description
   *
   * @param link
   */
  public void setLink(boolean link) {
    this.link = link;
  }

  /**
   * Method description
   *
   * @param loggingConfiguration
   */
  public void setLoggingConfiguration(String loggingConfiguration) {
    this.loggingConfiguration = loggingConfiguration;
  }

  /**
   * Method description
   *
   * @param openBrowser
   */
  public void setOpenBrowser(boolean openBrowser) {
    this.openBrowser = openBrowser;
  }

  /**
   * Method description
   *
   * @param port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Method description
   *
   * @param scmHome
   */
  public void setScmHome(File scmHome) {
    this.scmHome = scmHome;
  }

  /**
   * Method description
   *
   * @param stage
   */
  public void setStage(String stage) {
    this.stage = stage;
  }

  /**
   * Method description
   *
   * @param stopKey
   */
  public void setStopKey(String stopKey) {
    this.stopKey = stopKey;
  }

  /**
   * Method description
   *
   * @param stopPort
   */
  public void setStopPort(int stopPort) {
    this.stopPort = stopPort;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   * @param descriptor
   * @throws MojoExecutionException
   */
  @Override
  protected void execute(File descriptor) throws MojoExecutionException {
    File pluginDirectory = new File(scmHome, DIRECTORY_PLUGINS);
    SmpArtifact self = createSmpArtifact(descriptor);
    File exploded = new File(pluginDirectory, createPluginPath(self));

    // TODO check for version updates
    // TODO transitive smp dependencies
    Set<SmpArtifact> smps = SmpDependencyCollector.collect(project);
    for (SmpArtifact smp : smps) {
      install(pluginDirectory, smp);
    }

    PluginPathResolver pathResolver = new PluginPathResolver(
      classesDirectory.toPath(), webappSourceDirectory.toPath(), packageDirectory.toPath(), exploded.toPath()
    );

    try {
      if (link) {
        createExplodedLinked(pathResolver);
      } else {
        createExploded(exploded, descriptor, smps);
      }
    } catch (IOException ex) {
      throw new MojoExecutionException("could not create exploded smp", ex);
    }

    File warFile = getWebApplicationArchive();

    logger.info("start scm-maven-server with war {}", warFile);
    runScmServer(pathResolver, warFile);
  }

  private SmpArtifact createSmpArtifact(File descriptor) throws MojoExecutionException {
    if (descriptor.exists() && descriptor.isFile()) {
      Document document = XmlNodes.createDocument(descriptor);
      Node information = XmlNodes.getChild(document, "information");
      if (information == null) {
        throw new MojoExecutionException("could not find information node");
      }
      Node name = XmlNodes.getChild(information, "name");
      if (name == null) {
        throw new MojoExecutionException("could not find name node");
      }
      return new SmpArtifact(name.getTextContent(), project.getGroupId(), project.getArtifactId(), project.getVersion());
    } else {
      throw new MojoExecutionException("could not find descriptor");
    }
  }


  private void createExplodedLinked(PluginPathResolver pathResolver) throws IOException {
    logger.info("create exploded linked smp at {}", pathResolver.getInstallationDirectory());
    PluginLinker linker = new PluginLinker(pathResolver);
    linker.link();
  }

  private String createPluginPath(SmpArtifact artifact) {
    return artifact.getPluginName();
  }

  private void extractSmp(File directory, File archive) throws MojoExecutionException {
    logger.info("extract smp dependency {} to {}", archive, directory);

    try {
      mkdirs(directory);
    } catch (IOException ex) {
      throw new MojoExecutionException(
        "could not create plugin directory ".concat(directory.getPath()), ex);
    }

    unArchiver.setSourceFile(archive);
    unArchiver.setDestDirectory(directory);
    unArchiver.extract();
  }

  private void install(File pluginDirectory, SmpArtifact smp) throws MojoExecutionException {
    File directory = new File(pluginDirectory, createPluginPath(smp));

    if (!directory.exists()) {
      logger.info("install smp dependency {}", smp.getPluginName());
      File archive = checkAndResolve(convertToArtifact(smp));
      extractSmp(directory, archive);
    } else {
      logger.info("smp dependency {}, is already installed", smp.getPluginName());
    }
  }

  private void runScmServer(PluginPathResolver pathResolver, File warFile) throws MojoExecutionException {
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
      logger.info("install hot reloading components");

      FileWatcher watcher = new FileWatcher();
      registerRestartNotificationListener(watcher, pathResolver);
      registerLiveReloadListener(watcher, pathResolver);
      registerStaticFileListener(watcher, pathResolver);

      builder.withListener(watcher);
    }

    ScmServer server = builder.build();
    server.start();
  }

  @SuppressWarnings("squid:S3725") // Files.exists is to slow, but this is not critical in this case
  private void registerStaticFileListener(FileWatcher watcher, PluginPathResolver pathResolver) {
    if (Files.exists(pathResolver.getWebAppSourceDirectory())) {
      watcher.register(
        new StaticFileListener(pathResolver.getWebAppSourceDirectory(), pathResolver.getWebApp().getSource()),
        pathResolver.getWebAppSourceDirectory()
      );
    } else {
      logger.warn("skip register of static file listener, because webapp directory does not exists");
    }
  }

  @SuppressWarnings("squid:S3725") // Files.exists is to slow, but this is not critical in this case
  private void registerLiveReloadListener(FileWatcher watcher, PluginPathResolver pathResolver) {
    if (Files.exists(pathResolver.getWebApp().getSource())) {
      watcher.register(new LiveReloadDirectoryListener(), pathResolver.getWebApp().getSource());
    } else {
      logger.warn("skip register of live reload listener, because of target web directory does not exists");
    }
  }

  private void registerRestartNotificationListener(FileWatcher watcher, PluginPathResolver pathResolver) throws MojoExecutionException {
    long timeout = TimeUnit.SECONDS.toMillis(restartWaitTimeout);
    URL restartUrl = createRestartURL();
    watcher.register(
      new RestartNotificationDirectoryListener(restartUrl, timeout),
      pathResolver.getClasses().getSource(),
      pathResolver.getLib().getSource()
    );
  }

  private URL createRestartURL() throws MojoExecutionException {
    try {
      return new URL("http://localhost:" + port + contextPath + restartPath);
    } catch (MalformedURLException ex) {
      throw new MojoExecutionException("failed to create restart url", ex);
    }
  }

  private boolean isOpenBrowserListenerEnabled() {
    return openBrowser && Desktop.isDesktopSupported();
  }

  private File getWebApplicationArchive() throws MojoExecutionException {
    if (Strings.isNullOrEmpty(webApplication.getVersion())) {
      String version;
      MavenProject parent = project.getParent();

      if (parent != null) {
        version = parent.getVersion();
      } else {
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

  @Parameter(defaultValue = "src/main/webapp")
  private File webappSourceDirectory;

  @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}")
  private File packageDirectory;

  @Parameter
  private boolean corePlugin = false;

  /**
   * Field description
   */
  @Parameter
  private String contextPath = "/scm";

  /**
   * Field description
   */
  @Parameter
  private boolean link = true;

  /**
   * Field description
   */
  @Parameter
  private boolean backgroud = false;

  /**
   * Field description
   */
  @Parameter(property = "loggingConfiguration",
    defaultValue = "/logback.default.xml")
  private String loggingConfiguration;

  /**
   * Field description
   */
  @Parameter
  private int port = 8081;

  /**
   * Field description
   */
  @Parameter(
    property = "scm.home",
    alias = "scmHome",
    defaultValue = "${project.build.directory}/scm-home"
  )
  private File scmHome;

  /**
   * Field description
   */
  @Parameter(property = "scm.stage", defaultValue = "DEVELOPMENT")
  private String stage = "DEVELOPMENT";

  /**
   * Field description
   */
  @Parameter
  private String stopKey = "stop";

  /**
   * Field description
   */
  @Parameter
  private int stopPort = 8085;

  /**
   * Field description
   */
  @Parameter
  private boolean openBrowser = true;

  /**
   * Field description
   */
  @Parameter
  private final WebApplication webApplication = new WebApplication();

  /**
   * Field description
   */
  @Component(role = UnArchiver.class, hint = PACKAGE_JAR)
  private UnArchiver unArchiver;
}
