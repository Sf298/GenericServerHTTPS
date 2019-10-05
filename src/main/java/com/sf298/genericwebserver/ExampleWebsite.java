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
			public boolean allowed(String context, int token) {
				// access is allowed if
				return um.checkToken(token)
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
				// ensure token is valid and return error if not
				if(WSLoginInit.checkTokenAndReplyError(req, resp, um.getPAC())) {
					return 0;
				}
				
				int token = WSLoginInit.getToken(req);
				String uname = um.getUserID(token);
				
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
