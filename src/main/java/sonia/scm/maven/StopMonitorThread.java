/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
