package sonia.scm.maven.lr;

import com.github.sdorra.webresources.WebResourceSender;
import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Delivers the java script livereload client to the browser.
 *
 * @author Sebastian Sdorra
 */
public class LiveReloadScriptServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(LiveReloadScriptServlet.class);

    private final WebResourceSender.Sender sender;

    public LiveReloadScriptServlet() throws IOException {
        sender = createSender();
    }

    private final WebResourceSender.Sender createSender() throws IOException {
            URL livereloadJS = Resources.getResource("sonia/scm/maven/livereload.js");
            if (livereloadJS == null) {
                throw new FileNotFoundException("could not find livereload script");
            }

            return WebResourceSender.create()
                    .withGZIP()
                    .withExpires(7, TimeUnit.DAYS)
                    .resource(livereloadJS);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
        try {
            sender.head(req, resp);
        } catch (IOException ex) {
            LOG.warn("failed to send head informations to client", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            sender.get(req, resp);
        } catch (IOException ex) {
            LOG.warn("failed to send head livereload script to client", ex);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
