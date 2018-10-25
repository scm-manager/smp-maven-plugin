package sonia.scm.maven.lr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;

public class LiveReloadClient extends LiveReloadHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LiveReloadClient.class);


    /** registered client sessions */
    private final Iterable<Session> sessions;

    public LiveReloadClient(Iterable<Session> sessions) {
        this.sessions = sessions;
    }

    /**
     * Inform all client session about the modified file.
     *
     * @param path path to modified file
     */
    public void reload(String path) {
        LOG.info("send reload for path {}", path);

        for (Session session : sessions) {
            LOG.debug("send reload for path {} to {}", path, session.getId());
            sendMessage(session, LiveReloadProtocol.reload(path));
        }
    }
}
