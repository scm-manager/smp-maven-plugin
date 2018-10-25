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

//~--- JDK imports ------------------------------------------------------------

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint for livereload events.
 *
 * @author Sebastian Sdorra
 */
@ServerEndpoint("/livereload")
public final class LiveReloadEndPoint
{

  /** live reload server */
  private final LiveReloadServer server = LiveReloadContext.getServer();

  /**
   * Registers the client session at the {@link LiveReloadServer}.
   *
   * @param session client session
   */
  @OnOpen
  public void onOpen(Session session) {
    server.addSession(session);
  }

  /**
   * Delegates the received message to the {@link LiveReloadServer}.
   *
   * @param session client session
   * @param message received message
   */
  @OnMessage
  public void onMessage(Session session, String message) {
    server.receiveMessage(session, message);
  }

  /**
   * Removes the closed session from the {@link LiveReloadServer}.
   *
   * @param session closed session
   */
  @OnClose
  public void onClose(Session session) {
    server.removeSession(session);
  }
}
