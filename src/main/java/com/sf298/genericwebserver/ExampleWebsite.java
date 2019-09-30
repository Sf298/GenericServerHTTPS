/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

import java.io.FileNotFoundException;
import java.io.IOException;
import net.freeutils.httpserver.HTTPServer;
import sauds.toolbox.Encryptor;

/**
 *
 * @author saud
 */
public class ExampleWebsite {
	
	public static void main(String[] args) throws FileNotFoundException {
		String keystoreFilePath = (args.length < 1) ? "mykey.keystore" : args[0];
		String storepass = (args.length < 2) ? "123456" : args[1];
		String keypass = (args.length < 3) ? "456789" : args[2];
		int port = (args.length < 4) ? 443 : Integer.parseInt(args[3]);
		
		// create a new server
		SHTMLServer server = new SHTMLServer(port, keystoreFilePath, storepass, keypass);
		
		// create and populate a new usermanager
		String hashSalt = "suhbgfogv", fileEncryptorKey = "lsjfnrjn";
		UserManager um = new UserManager(true, hashSalt, fileEncryptorKey);
		um.addUser("Sf298", Encryptor.hashSHA256("password", ""));
		
		// add login pages and files found in the WebPages resource
		WSLoginInit.addToServer(server, um, "Test Website", "/home.html");
		/*WSFilesLoaderInit.addToServer(server, um, "WebPages", false,
				"login.html",
				"loginPhoto.png");*/
		
		// add custom (dynamic) context handler
		server.addContext("/home.html", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				if(!WSLoginInit.reqLoginValid(req, resp, um)) {
					return 0;
				}
				
				int token = WSLoginInit.getToken(req);
				String uname = um.getUser(token);
				
				// it is recommended to use the token place holder as to not leak valid tokens
				String page = "Hi "+uname+"! You are loggin in with token \""+WSLoginInit.TOKEN_PLH+"\"\n"
						+ "<a href='/home.html?token="+WSLoginInit.TOKEN_PLH+"'>Test link with token</a>";
				page = WSLoginInit.addTokenCode(page);
				
				resp.getHeaders().add("Content-Type", "text/html");
				resp.send(200, page);
				resp.close();
				return 0;
			}
		}, "GET");
		
		// start server
		server.start();
		
		// stop server
		//server.stop();
		//um.stop();
	}
	
}
