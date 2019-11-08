/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import net.freeutils.httpserver.HTTPServer;
import sauds.toolbox.deepfileiterator.DeepFileIterator;

/**
 * Adds 
 * @author saud
 */
public class WSFilesLoaderInit {
	
	/**
	 * Adds files found in the project resources folder as accessible web
	 * resources. Specified context paths may be white or black listed. If
	 * blacklisted, only logged in users can access resources in the filter.
	 * @param server the server to which the resources are added
	 * @param um the DefaultUserManager to use when checking the accessibility of files
	 * @param resourcesPath the folder to scan in the resources path
	 * @throws java.io.FileNotFoundException
	 */
	public static void addToServer(SHTMLServer server, UserManager um,
			String resourcesPath) throws FileNotFoundException {
		
		File root;
		try {
			root = new File(WSFilesLoaderInit.class.getClassLoader().getResource(resourcesPath).getFile().replace("%20", " "));
		} catch(NullPointerException ex) {
			throw new FileNotFoundException("Could not find resources file: "+resourcesPath);
		}
		DeepFileIterator dfi = new DeepFileIterator(root);
		while(dfi.hasNext()) {
			File file = dfi.next();
			String context = file.toString().substring(root.toString().length()).replace("\\", "/");
			
			printMessage("Added: "+context);
			server.addContext(context, (HTTPServer.Request req, HTTPServer.Response resp) -> {
				if(WSLoginInit.checkSessionIDAndReplyError(req, resp, um.getPAC()))
					return 0;
				
				if (context.endsWith(".html")) {
					String page = readWholeFile(file);
					if(um.getPAC()!=null) page = WSLoginInit.addSessionIDCode(page);
					resp.getHeaders().add("Content-Type", "text/html");
					resp.send(200, page);
				} else {
					HTTPServer.serveFile(file.getCanonicalFile(), req.getContext().getPath(), req, resp);
				}
				resp.close();
				return 0;
			}, "GET");
		}
	}
	
	public static String readWholeFile(File f) {
		StringBuilder sb = new StringBuilder(512);
		try {
			Scanner in = new Scanner(f);
			while (in.hasNext()) {
				sb.append(in.nextLine()).append("\n");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb.toString();
	}
    
    public static void printMessage(String line) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + "   " + line);
    }

}
