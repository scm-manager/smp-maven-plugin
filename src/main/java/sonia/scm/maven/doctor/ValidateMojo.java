package sonia.scm.maven.doctor;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateMojo extends AbstractDoctorMojo {

  @Override
  protected void execute(Context context, Results results) throws MojoExecutionException {
    results.forEach(this::log);
    if (results.hasError()) {
      throw new MojoExecutionException("one of the validation rules has failed with error");
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
