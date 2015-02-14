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



package sonia.scm.maven.lr;

//~--- non-JDK imports --------------------------------------------------------

import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.Collections;
import java.util.Set;

import javax.websocket.Session;

/**
 * Handler for the LiveReload sessions.
 *
 * @author Sebastian Sdorra
 */
public class LiveReloadHandler
{

  /**
   * the logger for LiveReloadHandler
   */
  private static final Logger logger =
    LoggerFactory.getLogger(LiveReloadHandler.class);

  //~--- methods --------------------------------------------------------------

  /**
   * Registers a new client session.
   *
   *
   * @param session client session
   */
  public void addSession(Session session)
  {
    logger.debug("session {} connected", session.getId());
    sessions.add(session);
  }

  /**
   * Inform all client session about the modified file.
   *
   *
   * @param path path to modified file
   */
  public void reaload(String path)
  {
    logger.info("send reload for path {}", path);

    for (Session session : sessions)
    {
      logger.debug("send reload for path {} to {}", path, session.getId());
      sendMessage(session, protocol.reload(path));
    }
  }

  /**
   * Handle received messages from client sessions.
   *
   *
   * @param session client session
   * @param message received message
   */
  public void receiveMessage(Session session, String message)
  {
    if (protocol.isHello(message))
    {
      logger.debug("received hello from {}", session.getId());
      sendMessage(session, protocol.hello());
    }
    else
    {
      logger.warn("received unknown message {} from {}", message,
        session.getId());
      sendMessage(session, protocol.alert("could not handle message"));
    }
  }

  /**
   * Remove client session.
   *
   *
   * @param session client session
   */
  public void removeSession(Session session)
  {
    logger.debug("session {} disconnected", session.getId());
    sessions.remove(session);
  }

  /**
   * Send message to client session.
   *
   *
   * @param session client session
   * @param message message to send
   */
  private void sendMessage(Session session, String message)
  {
    logger.trace("send message {} to {}", message, session.getId());

    try
    {
      session.getBasicRemote().sendText(message);
    }
    catch (IOException ex)
    {
      logger.warn("could not send message to lr client", ex);
    }
  }

  //~--- fields ---------------------------------------------------------------

  /** live reload protocol */
  private final LiveReloadProtocol protocol = new LiveReloadProtocol();

  /** registered client sessions */
  private final Set<Session> sessions =
    Collections.synchronizedSet(Sets.<Session>newHashSet());
}
