package sonia.scm.maven.ui;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

public abstract class AbstractUIMojo extends AbstractMojo {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractUIMojo.class);

  @Parameter(readonly = true, required = true, defaultValue = "${project}")
  private MavenProject project;

  @Parameter(readonly = true, required = true, defaultValue = "${session}")
  private MavenSession session;

  @Component
  private BuildPluginManager pluginManager;

  protected void execute(String goal, MojoExecutor.Element ...configuration) throws MojoExecutionException {
    MojoExecutor.ExecutionEnvironment environment = executionEnvironment(project, session, pluginManager);
    Plugin plugin = new Plugin();
    plugin.setArtifactId("buildfrontend-maven-plugin");
    plugin.setGroupId("com.github.sdorra");
    plugin.setVersion("2.2.0");


    List<MojoExecutor.Element> elements = new ArrayList<>();
    elements.add(createNodeConfiguration());
    elements.add(createPackageManagerConfiguration());
    elements.add(createFailOnMissingPackageJsonConfiguration());
    Collections.addAll(elements, configuration);

    Xpp3Dom cfg = configuration(elements.toArray(new MojoExecutor.Element[0]));

    if (LOG.isDebugEnabled()) {
      StringWriter writer = new StringWriter();
      Xpp3DomWriter.write(writer, cfg);
      LOG.debug("execute {} with the following configuration:\n{}", goal, writer.toString());
    }

    MojoExecutor.executeMojo(plugin, goal, cfg, environment);
  }

  protected MojoExecutor.Element createRunConfiguration(String script) {
    return element("script", script);
  }

  private MojoExecutor.Element createNodeConfiguration() {
    return element("nodeConfiguration",
      element("version", "8.11.4")
    );
  }

  private MojoExecutor.Element createPackageManagerConfiguration() {
    return element("packageManagerConfiguration",
      element("type", "YARN"),
      element("version", "1.9.4")
    );
  }

  private MojoExecutor.Element createFailOnMissingPackageJsonConfiguration() {
    return element("failOnMissingPackageJson", String.valueOf(false));
  }


}
