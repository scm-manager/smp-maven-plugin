package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.project.MavenProject;
import sonia.scm.maven.doctor.Result;

public class MissingPostInstallRule extends PackageJsonRule {

  private static final String FIELD_SCRIPTS = "scripts";
  private static final String FIELD_POSTINSTALL = "postinstall";
  private static final String SCRIPT_POSTINSTALL = "ui-plugins postinstall";

  @Override
  protected Result validate(MavenProject project, ObjectNode packageJson) {
    JsonNode scripts = packageJson.get(FIELD_SCRIPTS);
    if (scripts != null) {
      JsonNode postinstall = scripts.get(FIELD_POSTINSTALL);
      if (postinstall != null) {
        String script = postinstall.asText();
        if (script.equals(SCRIPT_POSTINSTALL)) {
          return Result.ok("package.json uses post install of ui-scripts");
        }
      }
    }
    return Result.warn("plugin should use '" + SCRIPT_POSTINSTALL + "' as postinstall script")
      .withFix(() -> {
        ObjectNode scriptNode = (ObjectNode) packageJson.get(FIELD_SCRIPTS);
        if (scriptNode == null) {
          scriptNode = packageJson.with(FIELD_SCRIPTS);
        }
        scriptNode.put(FIELD_POSTINSTALL, "SCRIPT_POSTINSTALL");
      }).build();
  }
}
