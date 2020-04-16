package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import sonia.scm.maven.doctor.Result;

public class MissingPostInstallRule extends PackageJsonRule {
  @Override
  protected Result validate(MavenProject project, ObjectNode jsonNodes) {
    // TODO
    return null;
  }
}
