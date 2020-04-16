package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import sonia.scm.maven.doctor.Context;
import sonia.scm.maven.doctor.Result;
import sonia.scm.maven.doctor.Rule;

import java.util.Optional;

public abstract class PackageJsonRule implements Rule {
  @Override
  public Result validate(Context context) {
    Optional<ObjectNode> packageJson = context.getPackageJson();
    if (packageJson.isPresent()) {
      return validate(context.getProject(), packageJson.get());
    }
    return Result.ok("plugin has no package.json to validate");
  }

  protected abstract Result validate(MavenProject project, ObjectNode jsonNodes);
}
