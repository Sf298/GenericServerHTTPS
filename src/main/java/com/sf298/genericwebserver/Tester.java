/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

import java.io.IOException;
import net.freeutils.httpserver.HTTPServer;
import sauds.toolbox.Encryptor;

/**
 *
 * @author saud
 */
public class Tester {
	
	public static void main(String[] args) {
		// create a new server
		SHTMLServer server = new SHTMLServer(8888, "", "", "");
		
		// create and populate a new usermanager
		UserManager um = new UserManager(true, "suhbgfogv", "lsjfnrjn");
		um.addUser("Sf298", Encryptor.hashSHA256("password", ""));
		
		// add login pages and files found in the WebPages resource
		WSLoginInit.addToServer(server, um, "Test Website", "/home.html");
		WSFilesLoaderInit.addToServer(server, um, "WebPages", false,
				"login.html",
				"logo.png");
		
		// add custom (dynamic) context handler
		server.addContext("/home.html", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				int token = WSLoginInit.getToken(req);
				String uname = um.getUser(token);
				resp.getHeaders().add("Content-Type", "text/html");
				resp.send(200, "Hi "+uname+"! you are loggin in!");
				resp.close();
				return 0;
			}
		}, "GET");
		
		// start server
		server.start();
		
		// stop server
		server.stop();
	}
	
}
