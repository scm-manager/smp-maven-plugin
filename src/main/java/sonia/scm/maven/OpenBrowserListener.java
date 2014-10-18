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
