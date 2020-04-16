package sonia.scm.maven;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Copies plugin.xml only if necessary and forces recompile if plugin.xml has changed.
 */
@Setter(AccessLevel.PACKAGE) // for testing purposes
@Mojo(name = "copy-plugin-xml", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class CopyPluginXmlMojo extends AbstractMojo {

  @Getter(AccessLevel.PACKAGE)
  @Parameter(defaultValue = "src/main/resources/META-INF/scm/plugin.xml", required = true)
  private File source;

  @Getter(AccessLevel.PACKAGE)
  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File outputDirectory;

  @Getter(AccessLevel.PACKAGE)
  @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/scm/plugin.xml", required = true)
  private File target;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(defaultValue = "${project.build.filters}", readonly = true)
  private List<String> buildFilters;

  @Component(role = MavenFileFilter.class, hint = "default")
  private MavenFileFilter fileFilter;

  @Override
  public void execute() throws MojoExecutionException {
    if (target.exists()) {
      if (isSourceModified()) {
        getLog().info("source plugin.xml is newer than target, force complete rebuild");
        clean();
        copy();
      } else {
        getLog().info("skip copy of plugin.xml, because target is newer");
      }
    } else {
      getLog().info("copy plugin.xml, because target does not exist");
      copy();
    }
  }

  private boolean isSourceModified() {
    return source.lastModified() > target.lastModified();
  }

  private void clean() throws MojoExecutionException {
    try {
      FileUtils.deleteDirectory(outputDirectory);
    } catch (IOException e) {
      throw new MojoExecutionException("failed to remove output directory " + outputDirectory, e);
    }
  }

  private void copy() throws MojoExecutionException {
    File parent = target.getParentFile();
    if (!parent.exists() && !parent.mkdirs()) {
      throw new MojoExecutionException("failed to create plugin.xml parent directory: " + parent);
    }
    try {
      fileFilter.copyFile(
        source,
        target,
        true,
        project,
        buildFilters,
        true,
        "UTF-8",
        session);
    } catch (MavenFilteringException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}
