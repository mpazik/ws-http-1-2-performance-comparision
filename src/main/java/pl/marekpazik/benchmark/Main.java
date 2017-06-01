package pl.marekpazik.benchmark;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class Main {
    static public void main(String[] args) {
        Http2Server http2Server = new Http2Server();
        Http1Server http1Server = new Http1Server();
        WebsocketServer websocketServer = new WebsocketServer();

        try {
            http2Server.start();
            http1Server.start();
            websocketServer.start();
            http2Server.waitUntilClosed();
        } catch (SSLException | InterruptedException | CertificateException e) {
            e.printStackTrace();
        } finally {
            websocketServer.shootDown();
            http1Server.shootDown();
            http2Server.shootDown();
        }
    }
}
