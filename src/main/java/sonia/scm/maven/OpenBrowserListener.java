/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



package sonia.scm.maven;

//~--- non-JDK imports --------------------------------------------------------

import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.awt.Desktop;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 * @author Sebastian Sdorra
 */
public class OpenBrowserListener implements Listener
{

  /**
   * the logger for OpenBrowserListener
   */
  private static final Logger logger =
    LoggerFactory.getLogger(OpenBrowserListener.class);

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @param port
   * @param contextPath
   */
  public OpenBrowserListener(int port, String contextPath)
  {
    this.port = port;
    this.contextPath = contextPath;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param event
   * @param cause
   */
  @Override
  public void lifeCycleFailure(LifeCycle event, Throwable cause)
  {

    // do nothing
  }

  /**
   * Method description
   *
   *
   * @param event
   */
  @Override
  public void lifeCycleStarted(LifeCycle event)
  {
    new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          Desktop desktop = Desktop.getDesktop();

          desktop.browse(new URI("http://localhost:" + port + contextPath));
        }
        catch (IOException | URISyntaxException ex)
        {
          logger.warn("could not open browser", ex);
        }
      }
    }).start();
  }

  /**
   * Method description
   *
   *
   * @param event
   */
  @Override
  public void lifeCycleStarting(LifeCycle event)
  {

    // do nothing
  }

  /**
   * Method description
   *
   *
   * @param event
   */
  @Override
  public void lifeCycleStopped(LifeCycle event)
  {

    // do nothing
  }

  /**
   * Method description
   *
   *
   * @param event
   */
  @Override
  public void lifeCycleStopping(LifeCycle event)
  {

    // do nothing
  }

  //~--- fields ---------------------------------------------------------------

  /** Field description */
  private final String contextPath;

  /** Field description */
  private final int port;
}
