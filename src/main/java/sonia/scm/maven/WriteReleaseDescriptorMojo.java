package sonia.scm.maven;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.common.hash.Hashing;
import lombok.Getter;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.io.Files.asByteSource;

@Mojo(
  name = "write-release-descriptor",
  requiresDependencyResolution = ResolutionScope.NONE
)
public class WriteReleaseDescriptorMojo extends AbstractDescriptorMojo {
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(property = "releaseDescriptor", required = true, readonly = true)
  private String releaseDescriptorFile;

  @Override
  protected void execute(File descriptor) throws MojoFailureException {
    ReleaseDescriptor releaseDescriptor = createReleaseDesciptor(readPluginFile(descriptor));
    try {
      createYamlMapper().writeValue(new File(releaseDescriptorFile), releaseDescriptor);
    } catch (IOException e) {
      throw new MojoFailureException("could not write descriptor", e);
    }
  }

  private ReleaseDescriptor createReleaseDesciptor(Plugin plugin) throws MojoFailureException {
    ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
    releaseDescriptor.plugin = project.getArtifactId();
    releaseDescriptor.tag = project.getVersion();
    releaseDescriptor.date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
    releaseDescriptor.url = createDownloadUrl();
    releaseDescriptor.checksum = computeCheckSum();
    releaseDescriptor.conditions = plugin.conditions;
    releaseDescriptor.dependencies.addAll(plugin.dependencies);
    return releaseDescriptor;
  }

  private ObjectMapper createYamlMapper() {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return mapper;
  }

  private Plugin readPluginFile(File descriptor) throws MojoFailureException {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Plugin.class);

      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      return (Plugin) jaxbUnmarshaller.unmarshal(descriptor);
    } catch (JAXBException e) {
      throw new MojoFailureException("could not read plugin.xml", e);
    }
  }

  String createDownloadUrl() {
    String groupIdPath = project.getGroupId().replaceAll("\\.", "/");
    return "https://maven.scm-manager.org/nexus/content/repositories/plugin-releases/" + project.getGroupId() + "/" + project.getArtifactId() + "/" + project.getVersion() + "/" + project.getArtifactId() + "-" + project.getVersion() + ".smp";
  }

  void setProject(MavenProject project) {
    this.project = project;
  }

  void setReleaseDescriptorFile(String releaseDescriptorFile) {
    this.releaseDescriptorFile = releaseDescriptorFile;
  }

  private String computeCheckSum() throws MojoFailureException {
    try {
      return asByteSource(project.getArtifact().getFile()).hash(Hashing.sha256()).toString();
    } catch (IOException e) {
      throw new MojoFailureException("could not compute checksum of artifact", e);
    }
  }

  @Getter
  private static class ReleaseDescriptor {
    String plugin;
    String tag;
    String date;
    String url;
    String checksum;
    Collection<String> dependencies = new ArrayList<>();
    PluginConditions conditions;
  }

  @XmlRootElement(name = "plugin")
  @XmlAccessorType(XmlAccessType.FIELD)
  static class Plugin {
    @XmlElement(name = "scm-version")
    private String scmVersion;

    private PluginConditions conditions;

    @XmlElementWrapper(name="dependencies")
    @XmlElement(name = "dependency")
    private List<String> dependencies;
  }

  @XmlRootElement(name = "information")
  @XmlAccessorType(XmlAccessType.FIELD)
  @Getter
  static class PluginConditions {
    @XmlElement(name = "min-version")
    private String minVersion;
    private String os;
    private String arch;
  }
}
