package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import sonia.scm.maven.doctor.Result;

public class NameRule extends PackageJsonRule {

  @Override
  protected Result validate(MavenProject project, ObjectNode packageJson) {
    String mavenName = project.getName();
    String jsName = packageJson.get("name").asText();

    int slash = jsName.indexOf('/');
    if (slash > 0) {
      jsName = jsName.substring(slash + 1);
    }

    if (mavenName.equals(jsName)) {
      return Result.ok("pom.xml and package.json names are equal");
    }

    return Result.error("pom.xml and package.json names differ")
      .withFix(() -> {
        packageJson.put("name", "@scm-manager/" + mavenName);
      })
      .build();
  }
}
