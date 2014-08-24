/**
 * Copyright (c) 2014, Sebastian Sdorra All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. Neither the name of SCM-Manager;
 * nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://bitbucket.org/sdorra/scm-manager
 *
 */



package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.Socket;

/**
 *
 * @author Sebastian Sdorra
 */
@Mojo(name = "stop")
public class StopMojo extends AbstractSmpMojo
{

  /** Field description */
  public static final String ADDRESS_LOCALHOST = "127.0.0.1";

  /**
   * the logger for StopMojo
   */
  private static final Logger logger = LoggerFactory.getLogger(StopMojo.class);

  //~--- get methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @return
   */
  public String getStopKey()
  {
    return stopKey;
  }

  /**
   * Method description
   *
   *
   * @return
   */
  public int getStopPort()
  {
    return stopPort;
  }

  //~--- set methods ----------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param stopKey
   */
  public void setStopKey(String stopKey)
  {
    this.stopKey = stopKey;
  }

  /**
   * Method description
   *
   *
   * @param stopPort
   */
  public void setStopPort(int stopPort)
  {
    this.stopPort = stopPort;
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
    try (Socket socket = createSocket();
      PrintWriter writer = createWriter(socket))
    {
      writer.println(stopKey);
      writer.flush();
    }
    catch (IOException ex)
    {
      logger.warn("could not stop jetty. Perhaps not running?");

      if (logger.isTraceEnabled())
      {
        logger.trace("could not stop jetty", ex);
      }
    }
  }

  /**
   * Method description
   *
   *
   * @return
   *
   * @throws IOException
   */
  private Socket createSocket() throws IOException
  {
    return new Socket(InetAddress.getByName(ADDRESS_LOCALHOST), stopPort);
  }

  /**
   * Method description
   *
   *
   * @param socket
   *
   * @return
   *
   * @throws IOException
   */
  private PrintWriter createWriter(Socket socket) throws IOException
  {
    return new PrintWriter(socket.getOutputStream());
  }

  //~--- fields ---------------------------------------------------------------

  /**
   * @parameter
   */
  private String stopKey = "stop";

  /**
   * @parameter
   */
  private int stopPort = 8085;
}
