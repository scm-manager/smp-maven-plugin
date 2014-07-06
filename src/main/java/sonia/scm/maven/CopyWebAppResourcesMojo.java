/**
 * Copyright (c) 2014, Sebastian Sdorra
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * https://bitbucket.org/sdorra/smp-maven-plugin
 */



package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.io.Files;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Sebastian Sdorra
 *
 */
@Mojo(
  name = "copy-webapp-resources",
  defaultPhase = LifecyclePhase.PROCESS_RESOURCES
)
public class CopyWebAppResourcesMojo extends AbstractWebappMojo
{

  /**
   * Method description
   *
   *
   * @param source
   * @param target
   *
   * @throws IOException
   */
  public void copyDirectory(File source, File target) throws IOException
  {
    if (source.isDirectory())
    {
      if (!target.exists() &&!target.mkdirs())
      {
        throw new IOException(
          "could not create directory ".concat(target.getPath()));
      }

      for (String child : source.list())
      {
        copyDirectory(new File(source, child), new File(target, child));
      }
    }
    else
    {
      Files.copy(source, target);
    }
  }

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param webappSourceDirectory
   */
  public void setWebappSourceDirectory(File webappSourceDirectory)
  {
    this.webappSourceDirectory = webappSourceDirectory;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  @Override
  protected void doExecute() throws MojoExecutionException, MojoFailureException
  {
    try
    {
      if (webappSourceDirectory.exists() && webappSourceDirectory.isDirectory())
      {
        copyDirectory(webappSourceDirectory, webappDirectory);
      }
    }
    catch (IOException ex)
    {
      throw new MojoFailureException("could not copy webapp files", ex);
    }
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Parameter(defaultValue = "${basedir}/src/main/webapp")
  private File webappSourceDirectory;
}
