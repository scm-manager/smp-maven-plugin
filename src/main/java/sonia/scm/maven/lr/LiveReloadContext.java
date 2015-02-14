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

import java.nio.file.Path;

/**
 * Context for live reload.
 *
 * @author Sebastian Sdorra
 */
public class LiveReloadContext
{

  /** instance */
  private static LiveReloadContext INSTANCE = null;

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @param webappDirectory watched directory
   */
  private LiveReloadContext(Path webappDirectory)
  {
    this.handler = new LiveReloadHandler();
    this.webappDirectory = webappDirectory;
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Initialize the context.
   *
   *
   * @param webappDirectory watched directory
   */
  public static void init(Path webappDirectory)
  {
    INSTANCE = new LiveReloadContext(webappDirectory);
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Returns the LiveReloadContext instance.
   *
   *
   * @return instance
   */
  public static LiveReloadContext getInstance()
  {
    return INSTANCE;
  }

  /**
   * Returns the {@link LiveReloadHandler}.
   *
   *
   * @return livereload handler
   */
  public LiveReloadHandler getHandler()
  {
    return handler;
  }

  /**
   * Returns the path of the watched directory.
   *
   *
   * @return watched directory
   */
  public Path getWebappDirectory()
  {
    return webappDirectory;
  }

  //~--- fields ---------------------------------------------------------------

  /** liverelaod handler */
  private final LiveReloadHandler handler;

  /** watched directory */
  private final Path webappDirectory;
}
