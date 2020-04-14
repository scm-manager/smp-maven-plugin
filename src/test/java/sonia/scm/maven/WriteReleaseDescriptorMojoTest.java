package sonia.scm.maven;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static edu.emory.mathcs.backport.java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(TempDirectory.class)
class WriteReleaseDescriptorMojoTest {

  Path pluginFile;
  Path artifactFile;

  @BeforeEach
  void writePluginFile(@TempDirectory.TempDir Path temp) throws IOException {
    pluginFile = temp.resolve("plugin.xml");
    ByteSource byteSource = Resources.asByteSource(Resources.getResource("plugin.xml"));
    byteSource.copyTo(Files.newOutputStream(pluginFile));
  }

  @BeforeEach
  void writeArtifactFile(@TempDirectory.TempDir Path temp) throws IOException {
    artifactFile = temp.resolve("scm-review-plugin.smp");
    Files.write(artifactFile, singleton("this is some plugin file"));
  }

  @Test
  void shouldWriteDescriptorFile(@TempDirectory.TempDir Path temp) throws MojoFailureException, IOException {
    WriteReleaseDescriptorMojo mojo = new WriteReleaseDescriptorMojo();
    mojo.setDescriptorPath(pluginFile.toString());
    Path resultFile = temp.resolve("plugin.yaml");
    mojo.setReleaseDescriptorFile(resultFile.toString());
    MavenProject project = new MavenProject();
    project.setArtifactId("scm-review-plugin");
    project.setGroupId("sonia.scm.plugins");
    project.setVersion("2.1.0");
    Artifact artifact = mock(Artifact.class);
    when(artifact.getFile()).thenReturn(artifactFile.toFile());
    project.setArtifact(artifact);
    mojo.setProject(project);
    mojo.execute(pluginFile.toFile());

    List<String> resultContent = Files.readAllLines(resultFile);

    assertThat(resultContent).contains(
      "plugin: \"scm-review-plugin\"",
      "tag: \"2.1.0\"",
      "checksum: \"ccc88166f98b68935a605ed9b595e94cfed4c9047df2e0feacc4776aaadeab4c\"",
      "dependencies:",
      "- \"scm-mail-plugin\"",
      "- \"scm-other-plugin\"",
      "conditions:",
      "  minVersion: \"2.2.0\"",
      "  os: \"Linux\"",
      "  arch: \"arm\"",
      "url: \"https://maven.scm-manager.org/nexus/content/repositories/plugin-releases/sonia/scm/plugins/scm-review-plugin/2.1.0/scm-review-plugin-2.1.0.smp\""
    );
    assertThat(resultContent.stream())
      .anyMatch(s -> s.matches("date: \".*\""));
  }
}
