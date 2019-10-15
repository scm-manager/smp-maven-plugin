package sonia.scm.maven;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.maven.lr.LiveReloadEndPoint;
import sonia.scm.maven.lr.LiveReloadScriptServlet;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public final class ScmServer {

    private static final Logger LOG = LoggerFactory.getLogger(ScmServer.class);

    private final Path warFile;
    private final Path scmHome;

    private int port = 8081;

    private String contextPath = "/scm";
    private boolean disableCorePlugins = false;
    private String loggingConfiguration;
    private String stage = "DEVELOPMENT";
    private String stopKey = "stop";
    private int stopPort = 8005;
    private boolean background = false;
    private int headerSize = 16384;

    private Set<ScmServerListener> listeners = Sets.newHashSet();

    private ScmServer(Path warFile, Path scmHome) {
        this.warFile = warFile;
        this.scmHome = scmHome;
    }

    public void start() throws MojoExecutionException {
        LOG.info("start scm-server at port {}", port);

        try {
            System.setProperty("scm.home", scmHome.toString());
            if (disableCorePlugins) {
                LOG.info("disable core plugin extraction");
                System.setProperty("sonia.scm.boot.disable-core-plugin-extraction", "true");
            }

            LOG.info("set stage {}", stage);
            System.setProperty("scm.stage", stage);

            System.setProperty("livereload.url", "/livereload.js");

            if (Strings.isNullOrEmpty(loggingConfiguration)) {
                System.setProperty("logback.configurationFile", loggingConfiguration);
            }

            Server server = new Server();

            URL baseURL = createBaseURL();
            server.addConnector(createServerConnector(server, baseURL));

            ContextHandlerCollection col = new ContextHandlerCollection();
            col.setHandlers(new Handler[]{
                createScmContext(),
                createLiveReloadContext(server)
            });
            server.setHandler(col);

            startStopMonitor(server);
            startReadyNotifier(baseURL);
            server.start();

            LOG.info("scm-server is now accessible at http://localhost:{}{}", port, contextPath);
            LOG.info("livereload is available at ws://localhost:{}/livereload", port);

            if (!background) {
                server.join();
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("could not start scm-server", ex);
        }
    }

  private void startReadyNotifier(URL baseURL) throws MalformedURLException {
    new Thread(new ReadyNotifier(listeners, baseURL, createReadinessURL())).start();
  }

  private URL createReadinessURL() throws MalformedURLException {
    return new URL("http://localhost:" + port + contextPath + "/api/v2");
  }

  private void startStopMonitor(Server server) {
        new StopMonitorThread(server, stopPort, stopKey).start();
    }

    private WebAppContext createScmContext() {
        WebAppContext warContext = new WebAppContext();

        warContext.setContextPath(contextPath);
        warContext.setExtractWAR(true);
        warContext.setWar(warFile.toString());

        return warContext;
    }

    private ServletContextHandler createLiveReloadContext(Server server) throws ServletException, DeploymentException {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setServer(server);

        ServerContainer wsc = WebSocketServerContainerInitializer.configureContext(context);
        wsc.addEndpoint(LiveReloadEndPoint.class);

        context.addServlet(LiveReloadScriptServlet.class, "/livereload.js");

        return context;
    }

    private ServerConnector createServerConnector(Server server, URL baseURL) throws MalformedURLException {
        ServerConnector connector = new ServerConnector(server);
        HttpConfiguration cfg = new HttpConfiguration();

        cfg.setRequestHeaderSize(headerSize);
        cfg.setResponseHeaderSize(headerSize);

        List<ConnectionFactory> factories = Lists.newArrayList();

        factories.add(new HttpConnectionFactory(cfg));
        connector.setConnectionFactories(factories);



      connector.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
            @Override
            public void lifeCycleStarted(LifeCycle event) {
                for (ScmServerListener listener : listeners) {
                    LOG.info("call started listener {} with url {}", listener.getClass(), baseURL);
                    try {
                        listener.started(baseURL);
                    } catch (IOException ex) {
                        LOG.warn("listener failed to start: " + listener.getClass(), ex);
                    }
                }
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                for (ScmServerListener listener : listeners) {
                    try {
                        listener.stopped(baseURL);
                    } catch (IOException ex) {
                        LOG.warn("listener failed to stop: " + listener.getClass(), ex);
                    }
                }
            }
        });

        connector.setPort(port);

        return connector;
    }

  private URL createBaseURL() throws MalformedURLException {
    return new URL("http://localhost:" + port + contextPath);
  }

  public static ScmServerBuilder builder(Path warFile, Path scmHome) {
        return new ScmServerBuilder(warFile, scmHome);
    }

    public static class ScmServerBuilder {

        private final ScmServer scmServer;

        public ScmServerBuilder(Path warFile, Path scmHome) {
            this.scmServer = new ScmServer(warFile, scmHome);
        }

        public ScmServerBuilder withDisableCorePlugins(boolean disableCorePlugins) {
            scmServer.disableCorePlugins = disableCorePlugins;
            return this;
        }

        public ScmServerBuilder withBackground(boolean background) {
            scmServer.background = background;
            return this;
        }

        public ScmServerBuilder withContextPath(String contextPath) {
            scmServer.contextPath = contextPath;
            return this;
        }

        public ScmServerBuilder withStage(String stage) {
            scmServer.stage = stage;
            return this;
        }

        public ScmServerBuilder withPort(int port) {
            scmServer.port = port;
            return this;
        }

        public ScmServerBuilder withStopPort(int port) {
            scmServer.stopPort = port;
            return this;
        }

        public ScmServerBuilder withLoggingConfiguration(String loggingConfiguration) {
            scmServer.loggingConfiguration = loggingConfiguration;
            return this;
        }

        public ScmServerBuilder withStopKey(String key) {
            scmServer.stopKey = key;
            return this;
        }

        public ScmServerBuilder withHeaderSize(int headerSize) {
            checkArgument(headerSize >= 1024, "header buffer must as least 1024");
            checkArgument(headerSize <= 65536, "header buffer must be smaller than 65536");
            scmServer.headerSize = headerSize;
            return this;
        }

        public ScmServerBuilder withListener(ScmServerListener scmServerListener) {
            scmServer.listeners.add(scmServerListener);
            return this;
        }

        public ScmServer build() {
            return scmServer;
        }
    }

}
