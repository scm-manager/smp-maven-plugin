package sonia.scm.maven.doctor.rules;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.maven.PluginDescriptor;
import sonia.scm.maven.SmpArtifact;
import sonia.scm.maven.SmpDependencyCollector;
import sonia.scm.maven.doctor.Context;
import sonia.scm.maven.doctor.Result;
import sonia.scm.maven.doctor.Rule;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public class MinCoreVersionRule implements Rule {

  private static final Logger LOG = LoggerFactory.getLogger(MinCoreVersionRule.class);

  @VisibleForTesting
  static final String PLUGIN_XML_PATH = "src/main/resources/META-INF/scm/plugin.xml";
  @VisibleForTesting
  static final String NO_PLUGIN = "no min version for plugin defined";
  @VisibleForTesting
  static final String NO_DEPENDENCY = "no plugin dependencies found";
  @VisibleForTesting
  static final String ERROR_PLUGIN = "failed to find min version of plugin";
  @VisibleForTesting
  static final String ERROR_DEPENDENCY = "failed to compute min version of plugin dependencies";

  private static final String PLUGIN_OLDER = "The specified min version of %s is older than %s, which is required by one of the depending plugins";
  private static final String PLUGIN_NEWER = "Current min version %s is newer or equal than %s, which is requested by depending plugins";

  private final DependencyResolver dependencyResolver;

  public MinCoreVersionRule() {
    this(SmpDependencyCollector::collect);
  }

  MinCoreVersionRule(DependencyResolver dependencyResolver) {
    this.dependencyResolver = dependencyResolver;
  }

  @Override
  public Result validate(Context context) {
    MavenProject project = context.getProject();

    Optional<ComparableVersion> currentMinVersion;
    try {
      currentMinVersion = findCurrentMinVersion(project);
      if (!currentMinVersion.isPresent()) {
        return Result.error(NO_PLUGIN).build();
      }
    } catch (MojoExecutionException e) {
      LOG.error(ERROR_PLUGIN, e);
      return Result.error(ERROR_PLUGIN).build();
    }

    Optional<ComparableVersion> dependencyMinVersion;
    try {
      dependencyMinVersion = findDependencyMinVersion(project);
      if (!dependencyMinVersion.isPresent()) {
        return Result.ok(NO_DEPENDENCY);
      }
    } catch (MojoExecutionException e) {
      LOG.error(ERROR_DEPENDENCY, e);
      return Result.error(ERROR_DEPENDENCY).build();
    }

    return compare(currentMinVersion.get(), dependencyMinVersion.get());
  }

  private Result compare(ComparableVersion currentMinVersion, ComparableVersion dependencyMinVersion) {
    if (currentMinVersion.compareTo(dependencyMinVersion) < 0) {
      String message = String.format(PLUGIN_OLDER, currentMinVersion, dependencyMinVersion);
      return Result.error(message).build();
    }
    String message = String.format(PLUGIN_NEWER, currentMinVersion, dependencyMinVersion);
    return Result.ok(message);
  }

  private Optional<ComparableVersion> findDependencyMinVersion(MavenProject project) throws MojoExecutionException {
    return dependencyResolver.resolve(project)
      .stream().map(smp -> smp.getDescriptor().findMinVersion())
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(ComparableVersion::new)
      .max(ComparableVersion::compareTo);
  }

  private Optional<ComparableVersion> findCurrentMinVersion(MavenProject project) throws MojoExecutionException {
    Path basedir = project.getBasedir().toPath();
    Path pluginXmlPath = basedir.resolve(PLUGIN_XML_PATH);

    try {
      Optional<String> optionalMinVersion = findPluginMinVersion(pluginXmlPath);

      if (!optionalMinVersion.isPresent()) {
        return Optional.empty();
      }

      String minVersion = optionalMinVersion.get();

      if (minVersion.equals("${project.parent.version}")) {
        minVersion = project.getParent().getVersion();
      }

      return Optional.of(new ComparableVersion(minVersion));
    } catch (Exception e) {
      throw new MojoExecutionException("failed to parse " + pluginXmlPath, e);
    }
  }

  private Optional<String> findPluginMinVersion(Path pluginXmlPath) throws MojoExecutionException {
    return PluginDescriptor.from(pluginXmlPath.toFile()).findMinVersion();
  }

  @FunctionalInterface
  interface DependencyResolver {
    Collection<SmpArtifact> resolve(MavenProject project) throws MojoExecutionException;
  }

}
