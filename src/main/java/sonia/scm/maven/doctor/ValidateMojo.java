package sonia.scm.maven.doctor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ValidateMojo extends AbstractDoctorMojo {

  @Override
  protected void execute(Context context, Results results) throws MojoExecutionException {
    results.forEach(this::log);
    if (results.hasError()) {
      throw new MojoExecutionException("one of the validation rules has failed with error");
    }
    if (results.stream().anyMatch(Result::isFixable)) {
      getLog().info("run 'mvn smp:fix' to fix the named problems");
    }
  }

  private void log(Result result) {
    String message = result.getMessage();
    if (result.isFixable()) {
      message += " [fixable]";
    }
    log(result.getType(), message);
  }
}
