package sonia.scm.maven.doctor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mojo(name = "fix", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class FixMojo extends AbstractDoctorMojo {

  @Override
  protected void execute(Context context, Results results) throws MojoExecutionException {
    List<Result> fixable = findFixable(results);
    if (fixable.isEmpty()) {
      getLog().info("nothing to fix");
    } else {
      fixable.forEach(this::process);
      Optional<ObjectNode> packageJson = context.getPackageJson();
      if (packageJson.isPresent()) {
        writePackageJson(packageJson.get());
      }
    }
  }

  private List<Result> findFixable(Results results) {
    return results.stream().filter(Result::isFixable).collect(Collectors.toList());
  }

  private void process(Result result) {
    if (result.isFixable()) {
      result.fix();
      getLog().info(result.getMessage() + " [fixed]");
    }
  }
}
