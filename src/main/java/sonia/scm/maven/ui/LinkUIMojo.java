package sonia.scm.maven.ui;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.twdata.maven.mojoexecutor.MojoExecutor;

@Mojo(name = "link-ui")
public class LinkUIMojo extends AbstractUIMojo {

  @Parameter
  private String[] links;

  @Override
  public void execute() throws MojoExecutionException {
    if (links != null) {
      for (String link : links) {
        execute("install-link", createLinkConfiguration(link));
      }
    }
  }

  private MojoExecutor.Element createLinkConfiguration(String pkg) {
    return MojoExecutor.element("pkg", pkg);
  }
}
