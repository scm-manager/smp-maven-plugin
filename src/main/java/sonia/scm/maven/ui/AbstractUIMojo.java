package sonia.scm.maven.ui;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

  @Parameter(defaultValue = "${basedir}")
  private File workingDirectory;


  protected void executeScript(String script) throws MojoExecutionException {
    executeScript(script, false);
  }

  protected void executeScript(String script, boolean background) throws MojoExecutionException {
    try {
      File packageJson = new File(workingDirectory, "package.json");
      if (packageJson.exists() && hasScript(packageJson, script)) {
        execute("run", createRunConfiguration(script), createBackgroundConfiguration(background));
      } else {
        LOG.warn("skip execution of script {}, because package.json or script entry is missing", script);
      }
    } catch (IOException ex) {
      throw new MojoExecutionException("failed to execute script", ex);
    }
  }

  private boolean hasScript(File packageJson, String script) throws IOException {
    try (JsonReader reader = Json.createReader(new FileReader(packageJson))) {
      JsonObject root = reader.readObject();
      if (root.containsKey("scripts")) {
        JsonObject scripts = root.getJsonObject("scripts");
        return scripts.containsKey(script);
      }
    }
    return false;
  }

  private Plugin findOrCreatePlugin() {
    Plugin plugin = project.getPlugin("com.github.sdorra:buildfrontend-maven-plugin");
    if (plugin == null) {
      plugin = createDefaultPlugin();
    }
    return plugin;
  }

  private Plugin createDefaultPlugin() {
    Plugin plugin = new Plugin();
    plugin.setArtifactId("buildfrontend-maven-plugin");
    plugin.setGroupId("com.github.sdorra");
    plugin.setVersion("2.5.0");
    return plugin;
  }

  private String getProperty(String key, String defaultValue) {
    return project.getProperties().getProperty(key, defaultValue);
  }

  protected void execute(String goal, MojoExecutor.Element ...configuration) throws MojoExecutionException {
    MojoExecutor.ExecutionEnvironment environment = executionEnvironment(project, session, pluginManager);

    Plugin plugin = findOrCreatePlugin();

    List<MojoExecutor.Element> elements = new ArrayList<>();
    elements.add(createNodeConfiguration());
    elements.add(createPackageManagerConfiguration());
    elements.add(createFailOnMissingPackageJsonConfiguration());
    Collections.addAll(elements, configuration);

    Xpp3Dom cfg = configuration(elements.toArray(new MojoExecutor.Element[0]));

    if (LOG.isDebugEnabled()) {
      StringWriter writer = new StringWriter();
      Xpp3DomWriter.write(writer, cfg);
      LOG.debug("execute {} with the following configuration:\n{}", goal, writer);
    }

    MojoExecutor.executeMojo(plugin, goal, cfg, environment);
  }

  protected MojoExecutor.Element createRunConfiguration(String script) {
    return element("script", script);
  }

  private MojoExecutor.Element createBackgroundConfiguration(boolean background) {
    return MojoExecutor.element("background", String.valueOf(background));
  }

  private MojoExecutor.Element createNodeConfiguration() {
    return element("nodeConfiguration",
      element("version", getProperty("nodejs.version", "12.16.1"))
    );
  }

  private MojoExecutor.Element createPackageManagerConfiguration() {
    return element("packageManagerConfiguration",
      element("type", "YARN"),
      element("version", getProperty("yarn.version", "1.22.0"))
    );
  }

  private MojoExecutor.Element createFailOnMissingPackageJsonConfiguration() {
    return element("failOnMissingPackageJson", String.valueOf(false));
  }

}
