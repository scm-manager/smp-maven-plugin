package sonia.scm.maven.doctor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Setter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

@Setter(AccessLevel.PACKAGE)
public abstract class AbstractDoctorMojo extends AbstractMojo {

  private final ObjectMapper mapper = new ObjectMapper();

  @Parameter(defaultValue = "package.json", required = true)
  private File packageJsonFile;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Setter(AccessLevel.PACKAGE)
  private Rules rules = Rules.all();

  @Override
  public void execute() throws MojoExecutionException {
    Context context = new Context(project);
    if (packageJsonFile.exists()) {
      context = context.withPackageJson(readPackageJson());
    }

    Results results = rules.validate(context);
    execute(context, results);
  }

  protected abstract void execute(Context context, Results results) throws MojoExecutionException;

  protected void log(Result.Type type, String message) {
    if (type == Result.Type.OK) {
      getLog().info("Rule passed: " + message);
    } else {
      String messageWithPrefix = "Rule failed: " + message;
      if (type == Result.Type.WARN) {
        getLog().warn(messageWithPrefix);
      } else {
        getLog().error(messageWithPrefix);
      }
    }
  }

  protected void writePackageJson(ObjectNode packageJson) throws MojoExecutionException {
    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(packageJsonFile, packageJson);
    } catch (IOException e) {
      throw new MojoExecutionException("failed to write package.json", e);
    }
  }

  private ObjectNode readPackageJson() throws MojoExecutionException {
    try {
      return (ObjectNode) mapper.readTree(packageJsonFile);
    } catch (IOException e) {
      throw new MojoExecutionException("failed to parse package.json", e);
    }
  }
}
