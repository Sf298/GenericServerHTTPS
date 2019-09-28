/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ServerSocketFactory;
import net.freeutils.httpserver.HTTPServer;

/**
 *
 * @author saud
 */
public class SHTMLServer {
    
    private final int port;
    private final String keystoreFilename;
    private final String storepass;
    private final String keypass;
    private final HTTPServer server;
    private final HTTPServer.VirtualHost vhost;
    
	/**
	 * Running "keytool -genkey -keyalg RSA -alias alias -keystore mykey.keystore -storepass 01234 -keypass 56789"
	 * in cmd creates the file.
	 * @param port The port to host on.
	 * @param keystoreFilename "mykey.keystore"
	 * @param storepass "01234"
	 * @param keypass "56789"
	 */
    public SHTMLServer(int port, String keystoreFilename, String storepass, String keypass) {
        this.port = port;
        this.keystoreFilename = keystoreFilename;
        this.storepass = storepass;
        this.keypass = keypass;
        
        this.server = new HTTPServer(port);
        this.vhost = server.getVirtualHost(null);
    }
    
	/**
	 * Starts the server.
	 */
    public void start() {
        try {
            System.out.println("starting...");
            
            server.setServerSocketFactory(getServerSocketFactory());
            server.start();
            
            System.out.println("Server can be accessed on address(es): \n"+getIPAdresses());
            System.out.println("Started server on port: "+port);
            System.out.println();
            
        } catch (IOException | CertificateException | UnrecoverableKeyException ex) {
            Logger.getLogger(SHTMLServer.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Server initialisation failed. Is port already in use?");
        }
    }
	
	/**
	 * Stops the server. Cannot be restarted once stopped.
	 */
	public void stop() {
		server.stop();
	}
    
    protected ServerSocketFactory getServerSocketFactory() throws FileNotFoundException, IOException, CertificateException, UnrecoverableKeyException {
        try {
            char[] password = storepass.toCharArray();
            KeyStore ks = KeyStore.getInstance("jks");
            ks.load(new FileInputStream(keystoreFilename), password);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext.getServerSocketFactory();
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException ex) {
            Logger.getLogger(SHTMLServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
	/**
	 * Adds a context and its corresponding context handler to this server.
	 * Paths are normalised by removing trailing slashes (except the root).
	 * @param path the context's path (must start with '/')
	 * @param handler the context handler for the given path
	 * @param methods the HTTP methods supported by the context handler (default is "GET")
	 */
    public void addContext(String path, HTTPServer.ContextHandler handler, String... methods) {
        vhost.addContext(path, handler, methods);
    }
	
    public void addHTTPContext(String path, String http, String... methods) {
        vhost.addContext(path, new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				resp.getHeaders().add("Content-Type", "text/html");
				resp.send(200, http);
				resp.close();
				return 0;
			}
		}, methods);
    }
	
	/**
	 * Adds a redirect to the server.
	 * @param fromContext 
	 * @param toContext
	 * @param methods the HTTP methods supported by the context handler (default is "GET")
	 */
	public void addRedirect(String fromContext, String toContext, String... methods) {
		addContext(fromContext, (HTTPServer.Request req, HTTPServer.Response resp) -> {
			resp.redirect(toContext, true);
			return 0;
		}, methods);
	}
    
    public static String getIPAdresses() {
        try {
            String out = "";
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while(nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while(ias.hasMoreElements()) {
                    InetAddress ia = ias.nextElement();
                    out += "\t" + ni.getDisplayName() + " : " + ia.getHostAddress() + "\n";
                }
            }
            return out.substring(0, out.length()-1);
        } catch (SocketException ex) {
            Logger.getLogger(SHTMLServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
}
