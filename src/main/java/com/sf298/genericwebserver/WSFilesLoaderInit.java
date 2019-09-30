/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.freeutils.httpserver.HTTPServer;
import static net.freeutils.httpserver.HTTPServer.serveFile;
import sauds.toolbox.DeepFileIterator;

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
	 * @param um the UserManager to use when checking the accessibility of files
	 * @param resourcePath the folder to scan in the resources path
	 * @param isBlackList if the context filters are a white or black list
	 * @param contextFilter the contexts to filter
	 * are logged in.
	 * @throws java.io.FileNotFoundException
	 */
	public static void addToServer(SHTMLServer server, UserManager um,
			String resourcePath, boolean isBlackList, String... contextFilter) throws FileNotFoundException {
		if(contextFilter == null) contextFilter = new String[0];
		HashSet<String> filteredContexts = new HashSet<>(Arrays.asList(contextFilter));
		
		File root;
		try {
			root = new File(WSFilesLoaderInit.class.getClassLoader().getResource(resourcePath).getFile().replace("%20", " "));
		} catch(NullPointerException ex) {
			throw new FileNotFoundException("Could not find resources file: "+resourcePath);
		}
		DeepFileIterator dfi = new DeepFileIterator(root);
		while(dfi.hasNext()) {
			File file = dfi.next();
			String context = file.toString().substring(root.toString().length()).replace("\\", "/");
			
			server.addContext(context, (HTTPServer.Request req, HTTPServer.Response resp) -> {
				if(WSLoginInit.reqLoginValid(req, resp, um, filteredContexts, isBlackList)) {
					return 0;
				} else {
					if (context.endsWith(".html")) {
						String page = readWholeFile(file);
						resp.getHeaders().add("Content-Type", "text/html");
						resp.send(200, page);
					} else {
						HTTPServer.serveFile(file.getCanonicalFile(), req.getContext().getPath(), req, resp);
					}
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
