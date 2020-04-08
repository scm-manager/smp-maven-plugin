package sonia.scm.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@Mojo(
  name = "fix-package-version",
  requiresDependencyResolution = ResolutionScope.NONE
)
public class FixPackageJsonVersionMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoFailureException {
    File packageJson = new File(project.getBasedir(), "package.json");
    if (packageJson.exists()) {
      JsonNode json = readJson(packageJson);
      fixVersion(packageJson, json);
    }
  }

  private void fixVersion(File packageJson, JsonNode jsonNode) throws MojoFailureException {
    if (replaceVersion(jsonNode)) {
      ((ObjectNode) jsonNode).set("version", new TextNode(project.getVersion()));
      writeCorrectedJson(packageJson, jsonNode);
    }
  }

  private void writeCorrectedJson(File packageJson, JsonNode json) throws MojoFailureException {
    try {
      new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(packageJson, json);
    } catch (IOException e) {
      throw new MojoFailureException("could not write package.json", e);
    }
  }

  private JsonNode readJson(File packageJson) throws MojoFailureException {
    try {
      return new ObjectMapper().readTree(packageJson);
    } catch (IOException e) {
      throw new MojoFailureException("could not read package.json", e);
    }
  }

  private boolean replaceVersion(JsonNode jsonNode) {
    Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      if ("version".equals(entry.getKey())) {
        return !entry.getValue().textValue().equals(project.getVersion());
      }
    }
    return false;
  }

  void setProject(MavenProject project) {
    this.project = project;
  }
}
