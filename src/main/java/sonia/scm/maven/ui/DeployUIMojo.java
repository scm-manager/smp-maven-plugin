package sonia.scm.maven.ui;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.Collections;

@Mojo(name = "deploy-ui")
public class DeployUIMojo extends AbstractUIMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    executeScript("deploy");
  }

  private MojoExecutor.Element createDeployConfiguration() {
    return MojoExecutor.element("args", MojoExecutor.element("arg", project.getVersion()));
  }

  @Override
  protected Iterable<MojoExecutor.Element> createExtraConfiguration() {
    return Collections.singleton(createDeployConfiguration());
  }
}
