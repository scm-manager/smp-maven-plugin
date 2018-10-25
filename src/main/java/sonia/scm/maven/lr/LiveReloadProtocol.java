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

import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

/**
 * LiveReload protocol implementation.
 *
 * @author Sebastian Sdorra
 */
public final class LiveReloadProtocol
{

  /** supported protocol */
  private static final String PROTOCOL = "http://livereload.com/protocols/official-7";

  /** server name */
  private static final String SERVER = "smp-lr";

  private LiveReloadProtocol() {
  }

  /**
   * Creates an alert message.
   *
   *
   * @param message alert message
   *
   * @return json object for alert message
   */
  public static String alert(String message) {
    JsonObject object = Json.createObjectBuilder()
      .add("command", "alert")
      .add("message", message)
      .build();

    return toString(object);
  }

  /**
   * Creates an hello message.
   *
   * @return json object for hello message
   */
  public static String hello() {
    JsonObject object = Json.createObjectBuilder()
      .add("command", "hello")
      .add("protocols", Json.createArrayBuilder()
        .add(PROTOCOL)
        .build())
      .add("serverName", SERVER)
      .build();

    return toString(object);
  }

  /**
   * Creates an info message.
   *
   * @param url server url
   *
   * @return json object for info message
   */
  public static String info(String url) {
    JsonObject object = Json.createObjectBuilder()
            .add("command", "info")
            .add("plugins", Json.createArrayBuilder().build())
            .add("url", url)
            .build();

    return toString(object);
  }

  /**
   * Create an reload message for the modified path.
   *
   * @param path path which was modified
   *
   * @return json object for alert message
   */
  public static String reload(String path) {
    JsonObject object = Json.createObjectBuilder()
      .add("command", "reload")
      .add("path", path)
      .add("liveCSS", true)
      .build();

    return toString(object);
  }

  //~--- get methods ----------------------------------------------------------

  /**
   * Returns {@code true} if the received message is a hello command.
   *
   * @param message received message
   *
   * @return {@code true} if the message is a hello command
   */
  public static boolean isHello(String message) {
    return isCommand(message, "hello");
  }

  /**
   * Returns {@code true} if the received message is a info command.
   *
   * @param message received message
   *
   * @return {@code true} if the message is a info command
   */
  public static boolean isInfo(String message) {
    return isCommand(message, "info");
  }

  private static boolean isCommand(String message, String expectedCommand) {
    try (JsonReader reader = Json.createReader(new StringReader(message))) {
      JsonObject object = reader.readObject();
      String command = object.getString("command");

      return expectedCommand.equals(command);
    }
  }

  /**
   * Converts a JsonObject to string.
   *
   * @param object json object
   *
   * @return string representation of json object
   */
  private static String toString(JsonObject object) {
    StringWriter writer = new StringWriter();

    try (JsonWriter jw = Json.createWriter(writer)) {
      jw.writeObject(object);
    }

    return writer.toString();
  }
}
