package sonia.scm.maven.ui;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.twdata.maven.mojoexecutor.MojoExecutor;

@Mojo(name = "watch-ui")
public class WatchUIMojo extends AbstractUIMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    execute("run", createRunConfiguration("watch"), createBackgroundConfiguration(true));
  }

  private MojoExecutor.Element createBackgroundConfiguration(boolean background) {
    return MojoExecutor.element("background", String.valueOf(background));
  }

}
