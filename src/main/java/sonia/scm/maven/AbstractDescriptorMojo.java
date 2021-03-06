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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

/**
 *
 * @author Sebastian Sdorra
 */
public abstract class AbstractDescriptorMojo extends AbstractWebappMojo
{

  /** Field description */
  protected static final String PLUGIN_DESCRIPTOR = "META-INF/scm/plugin.xml";

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param descriptor
   * @throws MojoExecutionException
   * @throws MojoFailureException
   */
  protected abstract void execute(File descriptor)
    throws MojoExecutionException, MojoFailureException;

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param descriptorPath
   */
  public void setDescriptorPath(String descriptorPath)
  {
    this.descriptorPath = descriptorPath;
  }

  /**
   * Method description
   *
   *
   * @param outputDirectory
   */
  public void setOutputDirectory(File outputDirectory)
  {
    this.outputDirectory = outputDirectory;
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
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    execute(new File(outputDirectory, descriptorPath));
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  @Parameter(defaultValue = PLUGIN_DESCRIPTOR)
  protected String descriptorPath;

  /** Field description */
  @Parameter(defaultValue = "${project.build.outputDirectory}")
  protected File outputDirectory;
}
