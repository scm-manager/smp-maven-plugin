package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import sonia.scm.maven.doctor.Result;

public class UiPluginsVersionRule extends PackageJsonRule {

  @Override
  protected Result validate(MavenProject project, ObjectNode packageJson) {
    String mavenVersion = project.getParent().getVersion();
    ObjectNode dependencies = (ObjectNode) packageJson.get("dependencies");
    if (dependencies != null) {
      JsonNode uiPlugins = dependencies.get("@scm-manager/ui-plugins");
      if (uiPlugins != null) {
        String uiPluginsVersion = uiPlugins.asText();
        if (mavenVersion.equals(uiPluginsVersion)) {
          return Result.ok("parent pom version is equal with @scm-manager/ui-plugins");
        } else if (mavenVersion.endsWith("SNAPSHOT")) {
          return Result.ok("parent pom uses snapshot, no need to match @scm-manager/ui-plugins");
        } else {
          return Result.warn("@scm-manager/ui-plugins is not equal with parent pom")
            .withFix(() -> dependencies.put("@scm-manager/ui-plugins", mavenVersion))
            .build();
        }
      } else {
        return Result.ok("plugin has no dependency to @scm-manager/ui-plugins");
      }
    }
    return Result.ok("plugin has not dependencies");
  }
}
