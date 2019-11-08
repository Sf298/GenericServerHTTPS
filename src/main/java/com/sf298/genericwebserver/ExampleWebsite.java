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
		// load params from args, or use default values
		String keystoreFilePath = (args.length < 1) ? "mykey.keystore" : args[0];
		String storepass = (args.length < 2) ? "123456" : args[1];
		String keypass = (args.length < 3) ? "456789" : args[2];
		int port = (args.length < 4) ? 443 : Integer.parseInt(args[3]);
		
		// create a new server
		SHTMLServer server = new SHTMLServer(port, keystoreFilePath, storepass, keypass);
		
		// create and populate a new usermanager
		String hashSalt = "suhbgfogv", fileEncryptorKey = "lsjfnrjn";
		DefaultUserManager um = new DefaultUserManager(true, hashSalt, fileEncryptorKey);
		um.addUser("Sf298", Encryptor.hashSHA256("a", ""));
		
		// add a PagesAccessChecker to decide what pages can be access by what user
		um.setPAC(new PagesAccessChecker() {
			@Override
			public boolean allowed(String context, int sessionID) {
				// access is allowed if
				return um.checkSessionID(sessionID)
						//|| context.endsWith("home2.html")
						|| context.endsWith("login.html")
						|| context.endsWith("loginPhoto.png");
			}
		});
		
		// add login pages and files found in the WebPages resource
		WSLoginInit.addToServer(server, "Test Website", "/home.html", um);
		WSFilesLoaderInit.addToServer(server, um, "WebPages");
		
		// add custom server-side context handler
		server.addContext("/home.html", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				// ensure session ID is valid and return error if not
				if(WSLoginInit.checkSessionIDAndReplyError(req, resp, um.getPAC())) {
					return 0;
				}
				
				int sessionID = WSLoginInit.getSessionID(req);
				String uname = um.getUserID(sessionID);
				
				// it is recommended to use the session ID place holder as to not leak valid session IDs
				String page = "Hi "+uname+"! You are loggin in with sessionID \""+WSLoginInit.SESSION_ID_PLH+"\"\n"
						+ "<a href='/home.html?sessionID="+WSLoginInit.SESSION_ID_PLH+"'>Test link with sessionID</a>";
				page = WSLoginInit.addSessionIDCode(page);
				
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
