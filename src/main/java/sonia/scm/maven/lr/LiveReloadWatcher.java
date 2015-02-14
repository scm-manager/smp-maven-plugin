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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * The LiveReloadWatcher watches a directory for changes and notifies the
 * {@link LiveReloadHandler} on change.
 *
 * @author Sebastian Sdorra
 */
public class LiveReloadWatcher implements Runnable
{

  /**
   * the logger for LiveReloadWatcher
   */
  private static final Logger logger =
    LoggerFactory.getLogger(LiveReloadWatcher.class);

  //~--- constructors ---------------------------------------------------------

  /**
   * Constructs ...
   *
   *
   * @throws IOException
   */
  public LiveReloadWatcher() throws IOException
  {
    this(LiveReloadContext.getInstance());
  }

  /**
   * Constructs ...
   *
   *
   * @param ctx
   *
   * @throws IOException
   */
  public LiveReloadWatcher(LiveReloadContext ctx) throws IOException
  {
    this(ctx.getHandler(), ctx.getWebappDirectory());
  }

  /**
   * Creates a WatchService and registers the given directory
   *
   *
   * @param handler
   * @param webappDirectory
   *
   * @throws IOException
   */
  LiveReloadWatcher(LiveReloadHandler handler, Path webappDirectory)
    throws IOException
  {
    this.handler = handler;
    this.webappDirectory = webappDirectory;
    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<>();
    logger.debug("Scanning directory {}", webappDirectory);
    registerAll(webappDirectory);
  }

  //~--- methods --------------------------------------------------------------

  /**
   * Cast watch event.
   *
   *
   * @param event event
   * @param <T> type
   *
   * @return casted event
   */
  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event)
  {
    return (WatchEvent<T>) event;
  }

  /**
   * Process all events for keys queued to the watcher
   */
  @Override
  public void run()
  {
    for (;;)
    {

      // wait for key to be signalled
      WatchKey key;

      try
      {
        key = watcher.take();
      }
      catch (InterruptedException x)
      {
        return;
      }

      Path dir = keys.get(key);

      if (dir == null)
      {
        logger.warn("WatchKey not recognized");

        continue;
      }

      for (WatchEvent<?> event : key.pollEvents())
      {
        WatchEvent.Kind kind = event.kind();

        // TBD - provide example of how OVERFLOW event is handled
        if (kind == OVERFLOW)
        {
          continue;
        }

        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        Path child = dir.resolve(name);

        handleEvent(event, kind, child);
      }

      // reset key and remove from set if directory no longer accessible
      boolean valid = key.reset();

      if (!valid)
      {
        keys.remove(key);

        // all directories are inaccessible
        if (keys.isEmpty())
        {
          break;
        }
      }
    }
  }

  /**
   * Handle fs event.
   *
   *
   * @param event fs event
   * @param kind kind of event
   * @param child changed path
   */
  private void handleEvent(WatchEvent<?> event, WatchEvent.Kind kind,
    Path child)
  {

    // log event
    logger.debug("received fs event {} for {}", event.kind().name(), child);

    // if directory is created, register it and its sub-directories
    if ((kind == ENTRY_CREATE) && Files.isDirectory(child, NOFOLLOW_LINKS))
    {
      try
      {
        registerAll(child);
      }
      catch (IOException ex)
      {

        logger.warn("failed to register new directory", ex);
      }
    }
    else if ((kind == ENTRY_MODIFY)
      && Files.isRegularFile(child, NOFOLLOW_LINKS))
    {
      notifyHandler(child);
    }
  }

  /**
   * Notify livereload handler.
   *
   *
   * @param file changed file
   */
  private void notifyHandler(Path file)
  {
    String path = webappDirectory.relativize(file).toString();

    logger.debug("notify handler about changed path {}", path);
    handler.reaload(path);
  }

  /**
   * Register the given directory with the WatchService
   *
   * @param dir
   *
   * @throws IOException
   */
  private void register(Path dir) throws IOException
  {
    logger.debug("register directory {}", dir);

    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE,
                     ENTRY_MODIFY);

    keys.put(key, dir);
  }

  /**
   * Register the given directory, and all its sub-directories, with the
   * WatchService.
   *
   * @param start
   *
   * @throws IOException
   */
  private void registerAll(final Path start) throws IOException
  {

    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult preVisitDirectory(Path dir,
        BasicFileAttributes attrs)
        throws IOException
      {
        register(dir);

        return FileVisitResult.CONTINUE;
      }
    });
  }

  //~--- fields ---------------------------------------------------------------

  /** livereload handler */
  private final LiveReloadHandler handler;

  /** watched keys */
  private final Map<WatchKey, Path> keys;

  /** the watcher */
  private final WatchService watcher;

  /** watched directory */
  private final Path webappDirectory;
}
