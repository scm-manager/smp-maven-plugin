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

import com.google.common.base.Throwables;

import org.eclipse.jetty.server.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Sebastian Sdorra
 */
public class StopMonitorThread extends Thread
{

  /** Field description */
  public static final String ADDRESS_LOCALHOST = "127.0.0.1";

  /** Field description */
  public static final String NAME = "ScmStopMonitor";

  /**
   * the logger for StopMonitorThread
   */
  private static final Logger logger =
    LoggerFactory.getLogger(StopMonitorThread.class);

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @param server
   * @param stopPort
   * @param stopKey
   */
  public StopMonitorThread(Server server, int stopPort, String stopKey)
  {
    this.server = server;
    this.stopKey = stopKey;
    setDaemon(true);
    setName(NAME);

    try
    {
      socket = new ServerSocket(stopPort, 1,
        InetAddress.getByName(ADDRESS_LOCALHOST));
    }
    catch (IOException e)
    {
      logger.warn("could not start server");

      throw Throwables.propagate(e);
    }
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   */
  @Override
  public void run()
  {
    try (Socket accept = socket.accept();
      BufferedReader reader = createReader(accept))
    {
      String line = reader.readLine();

      if (stopKey.equals(line))
      {
        server.stop();
        socket.close();
      }
    }
    catch (Exception ex)
    {
      throw Throwables.propagate(ex);
    }
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
  private BufferedReader createReader(Socket socket) throws IOException
  {
    return new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private final Server server;

  /** Field description */
  private final ServerSocket socket;

  /** Field description */
  private final String stopKey;
}
