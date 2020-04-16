package sonia.scm.maven.doctor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nullable;
import java.util.Optional;

public class Context {

  private final MavenProject project;
  private final ObjectNode packageJson;

  public Context(MavenProject project, @Nullable ObjectNode packageJson) {
    this.project = project;
    this.packageJson = packageJson;
  }

  public Optional<ObjectNode> getPackageJson() {
    return Optional.ofNullable(packageJson);
  }

  public MavenProject getProject() {
    return project;
  }
}
