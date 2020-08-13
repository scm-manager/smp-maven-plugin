package sonia.scm.maven.doctor.rules;

import com.google.common.io.Resources;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sonia.scm.maven.PluginDescriptor;
import sonia.scm.maven.SmpArtifact;
import sonia.scm.maven.XmlNodes;
import sonia.scm.maven.doctor.Result;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static sonia.scm.maven.doctor.rules.MinCoreVersionRule.*;

@ExtendWith(MockitoExtension.class)
class MinCoreVersionRuleTest extends RuleTestBase {

  private Path directory;

  @BeforeEach
  void setUpProject(@TempDir Path directory) {
    this.directory = directory;
    when(project.getBasedir()).thenReturn(directory.toFile());
  }

  @Test
  void shouldReturnOkWithoutDependencies() throws IOException, MojoExecutionException {
    Result result = validate("2.0.0");
    assertResult(result, Result.Type.OK);
  }

  @Test
  void shouldReturnOkIfPluginIsNewerThanDependency() throws IOException, MojoExecutionException {
    Result result = validate("2.1.0", "2.0.0");
    assertResult(result, Result.Type.OK);
  }

  @Test
  void shouldFailIfNewerVersionIsRequired() throws IOException, MojoExecutionException {
    Result result = validate("2.1.0", "2.0.0", "2.0.1", "2.2.0");
    assertResult(result, Result.Type.ERROR, "2.1.0", "2.2.0");
  }

  @Test
  void shouldResolveParentVersionExpression() throws IOException, MojoExecutionException {
    when(project.getParent().getVersion()).thenReturn("2.2.1");
    Result result = validate("${project.parent.version}", "2.0.1");
    assertResult(result, Result.Type.OK, "2.0.1", "2.2.1");
  }

  @Test
  void shouldFailIfDependencyVersionCouldNotBeResolved() throws IOException, MojoExecutionException {
    createDescriptor("2.0.0");

    MinCoreVersionRule rule = new MinCoreVersionRule(project -> {
      throw new MojoExecutionException("failed");
    });

    Result result = rule.validate(context);
    assertResult(result, Result.Type.ERROR, ERROR_DEPENDENCY);
  }

  @Test
  void shouldFailWithPluginMinVersion() throws IOException, MojoExecutionException {
    Result result = validate("");
    assertResult(result, Result.Type.ERROR, NO_PLUGIN);
  }

  @Test
  void shouldFailPluginVersionCouldNotBeResolved() throws IOException, MojoExecutionException {
    MinCoreVersionRule rule = new MinCoreVersionRule(project -> Collections.emptyList());
    Result result = rule.validate(context);
    assertResult(result, Result.Type.ERROR, ERROR_PLUGIN);
  }

  private void assertResult(Result result, Result.Type expectedType, String... expectedMessageParts) {
    assertThat(result.getType()).isSameAs(expectedType);
    if (expectedMessageParts.length > 0) {
      assertThat(result.getMessage()).contains(expectedMessageParts);
    }
  }

  private Result validate(String pluginMinVersion, String... dependencyMinVersions) throws IOException, MojoExecutionException {
    createDescriptor(pluginMinVersion);
    List<SmpArtifact> smps = Arrays.stream(dependencyMinVersions)
      .map(this::smp)
      .collect(Collectors.toList());
    return new MinCoreVersionRule(project -> smps).validate(context);
  }

  private SmpArtifact smp(String minVersion) {
    PluginDescriptor descriptor = mock(PluginDescriptor.class);
    when(descriptor.findMinVersion()).thenReturn(Optional.ofNullable(minVersion));
    return new SmpArtifact(
      "sonia.scm.plugins",
      "scm-test-plugin",
      "1.0.0",
      false,
      descriptor
    );
  }

  @SuppressWarnings("UnstableApiUsage")
  private void createDescriptor(String minVersion) throws IOException, MojoExecutionException {
    URL resource = Resources.getResource("sonia/scm/maven/doctor/rules/scm-review-plugin.xml");
    byte[] bytes = Resources.toByteArray(resource);

    Document document = XmlNodes.createDocument(bytes);
    Node conditionsNode = XmlNodes.getChild(document, "conditions");
    Node minVersionNode = XmlNodes.getChild(conditionsNode, "min-version");
    minVersionNode.setTextContent(minVersion);

    Path pluginXmlPath = directory.resolve(MinCoreVersionRule.PLUGIN_XML_PATH);
    Files.createDirectories(pluginXmlPath.getParent());

    XmlNodes.writeDocument(pluginXmlPath.toFile(), document);
  }

}
