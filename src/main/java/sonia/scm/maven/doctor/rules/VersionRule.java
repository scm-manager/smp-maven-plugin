package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import sonia.scm.maven.doctor.Result;

public class VersionRule extends PackageJsonRule {

  private static final String FIELD_VERSION = "version";

  @Override
  protected Result validate(MavenProject project, ObjectNode packageJson) {
    String mavenVersion = project.getVersion();
    if (!packageJson.has(FIELD_VERSION)) {
      return Result.ok("package.json has no version, which is fine");
    }

    String jsVersion = packageJson.get(FIELD_VERSION).asText();
    if (mavenVersion.equals(jsVersion)) {
      return Result.ok("version of pom.xml and package.json are equal");
    } else {
      return Result.error("version of pom.xml and package.json are not equal")
        .withFix(() -> packageJson.put(FIELD_VERSION, mavenVersion))
        .build();
    }
  }


}
