/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.freeutils.httpserver.HTTPServer;

/**
 * Adds 
 * @author saud
 */
public class WSLoginInit {
	
	/**
	 * A place holder String that gets replaced by the user's session ID on the client.
	 */
	public static String SESSION_ID_PLH = "0twtcht4msessionID0thv303c";
	public static String SESSION_ID_VAR_NAME = "sessionID";
	public static String SESSION_ID_URL_PARAM = SESSION_ID_VAR_NAME+"="+SESSION_ID_PLH;
	
	public static String addSessionIDCode(String page) {
		String injCode1 = "\n<script src=\"/sessionCode.js\"></script>\n";
		int[] pos;
		while(true) {
			if( (pos = firstRgx(page, "<.*head.*>")) != null ) {
				page = insStr(page, injCode1, pos[1]);
				break;
			} else if( (pos = firstRgx(page, "<.*body.*>")) != null ) {
				page = insStr(page, injCode1, pos[1]);
				break;
			} else {
				page = "<html>\n<body>\n"+page+"\n</body>\n</html>";
			}
		}
		
		String injCode2 =	"\n" +
							"<script type=\"text/javascript\"> \n" +
							"    fillPageWithSessionID();\n" +
							"</script>\n";
		if( (pos = firstRgx(page, "<.*\\/.*body.*>")) != null ) {
			page = insStr(page, injCode2, pos[0]);
		} else {
			page = insStr(page, injCode2, -1);
		}
		return page;
	}
	private static int[] firstRgx(String str, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(str);
		if(matcher.find()) {
			return new int[] {matcher.start(), matcher.end()};
		}
		return null;
	}
	private static String insStr(String str, String insStr, int i) { 
		if(i == -1) i = str.length();
        StringBuilder newString = new StringBuilder(str);
        newString.insert(i, insStr);
        return newString.toString(); 
    } 
	
	/**
	 * Gets the user session ID from the provided request.
	 * @param req the request
	 * @return the session ID found in the request or -1 if not found
	 * @throws IOException 
	 */
	public static int getSessionID(HTTPServer.Request req) throws IOException {
		Map<String, String> params = req.getParams();
		return Integer.parseInt(params.getOrDefault(SESSION_ID_VAR_NAME, "-1"));
	}
	
	/**
	 * Checks if the session ID belongs to a valid user account. Replies with a 403
	 * error, closes the stream and returns true if the session ID is invalid. This
	 * allows it to be easily used in an if statement;
	 * @param req the request
	 * @param resp the response to write to
	 * @param pac the pages access checker used to determine if a user needs to
	 * be logged in to access the page.
	 * @return true if the user exists, otherwise false
	 * @throws IOException 
	 */
	public static boolean checkSessionIDAndReplyError(HTTPServer.Request req,
			HTTPServer.Response resp, PagesAccessChecker pac) throws IOException {
		if(!pac.allowed(req.getContext().getPath(), getSessionID(req))) {
			resp.sendError(403, "Invalid login");
			resp.close();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Adds the relevant pages and services to allow user accounts.
	 * @param server the server to which the resources are added
	 * @param um the UserManager to use
	 * @param websiteTitle the title of the website, it is displayed on the login page
	 * @param homePageAddress the context to forward traffic to after login is
	 * validated. must start with a "/"
	 */
	public static void addToServer(SHTMLServer server, String websiteTitle, String homePageAddress, UserManager um) {
		if(server == null) throw new NullPointerException("server == null");
		if(um == null) throw new NullPointerException("um == null");
		if(homePageAddress == null) throw new NullPointerException("homePageAddress == null");
		addPages(server, websiteTitle, homePageAddress);
		addServices(server, um);
		server.addRedirect("/", "/login.html");
	}
	private static void addPages(SHTMLServer server, String websiteTitle, String homePageAddress) {
		String page = getLoginPage(websiteTitle, homePageAddress);
		server.addHTTPContext("/login.html", page, "GET");
		server.addContext("/sessionCode.js", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				String page = getSessionIDCodeJS();
				resp.getHeaders().add("Content-Type", "text/javascript");
				resp.send(200, page);
				resp.close();
				return 0;
			}
		}, "GET");
	}
	private static void addServices(SHTMLServer server, UserManager umi) {
		server.addContext("/logout", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				int sessionID = getSessionID(req);
				if(sessionID != -1) {
					umi.logout(sessionID);
				}
				resp.send(200, "done");
				resp.close();
				return 0;
			}
		}, "PUT");
		server.addContext("/logoutUser", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				int sessionID = getSessionID(req);
				if(sessionID != -1) {
					umi.logoutUser(sessionID);
				}
				resp.send(200, "done");
				resp.close();
				return 0;
			}
		}, "PUT");
		server.addContext("/loginChecker", new HTTPServer.ContextHandler() { // process session ID and login credentials
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				//System.out.println("got req params "+req.getPath()+req.getParams());
				Map<String, String> params = req.getParams();
				if (params.containsKey(SESSION_ID_VAR_NAME)) {
					int sessionID = Integer.parseInt(params.get(SESSION_ID_VAR_NAME));
					resp.send(200, String.valueOf(umi.checkSessionID(sessionID)));
				} else if (params.containsKey("uname") && params.containsKey("pswHash")) {
					if (umi.tryLogin(params.get("uname"), params.get("pswHash"))) {
						printMessage("User: " + params.get("uname") + " logged in succesfully");
						resp.send(200, String.valueOf(umi.newSessionID(params.get("uname"))));
					} else {
						resp.send(200, "-1");
					}
				} else {
					resp.sendError(403);
				}
				resp.close();
				return 0;
			}
		}, "GET");
	}
	
	/**
	 * requires homepage, login checker, sessionID.js 
	 * @param websiteTitle Title of the website.
	 * @param homePageAddress eg "/home.html".
	 * @return 
	 */
	private static String getLoginPage(String websiteTitle, String homePageAddress) {
		return	"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"    <title>"+websiteTitle+"</title>\n" +
				"    <meta charset=\"UTF-8\">\n" +
				"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
				"    \n" +
				"    <style>\n" +
				"        .container { \n" +
				"          height: 96vh;\n" +
				"          position: relative;\n" +
				"        }\n" +
				"\n" +
				"        .center {\n" +
				"          margin: 0;\n" +
				"          position: absolute;\n" +
				"          top: 50%;\n" +
				"          left: 50%;\n" +
				"          -ms-transform: translate(-50%, -50%);\n" +
				"          transform: translate(-50%, -50%);\n" +
				"        }\n" +
				"    </style>\n" +
				"</head>\n" +
				"\n" +
				"<body style=\"max-width:1600px\">\n" +
				"    \n" +
				"    <script src=\"/sessionCode.js\"></script>\n" +
				"    <script type=\"text/javascript\">\n" +
				"        function hexString(buffer) {\n" +
				"            const byteArray = new Uint8Array(buffer);\n" +
				"            const hexCodes = [...byteArray].map(value => {\n" +
				"                const hexCode = value.toString(16);\n" +
				"                const paddedHexCode = hexCode.padStart(2, '0');\n" +
				"                return paddedHexCode;\n" +
				"            });\n" +
				"            return hexCodes.join('');\n" +
				"        }\n" +
				"        function digestMessage(message) {\n" +
				"            const encoder = new TextEncoder();\n" +
				"            const data = encoder.encode(message);\n" +
				"            return window.crypto.subtle.digest('SHA-256', data);\n" +
				"        }\n" +
				"        \n" +
				"        var sessionID = getSessionID();\n" +
				"        if(sessionID != \"\") {\n" +
				"            var xhr = new XMLHttpRequest();\n" +
				"            xhr.open('GET', \"/loginChecker?"+SESSION_ID_VAR_NAME+"=\"+sessionID, true);\n" +
				"            xhr.send();\n" +
				"            xhr.onreadystatechange = function (e) {\n" +
				"                if (xhr.readyState == 4 && xhr.status == 200) {\n" +
				"                    if(xhr.responseText == \"true\") {\n" +
				"                        window.location.replace(\""+homePageAddress+"?"+SESSION_ID_VAR_NAME+"=\"+sessionID);\n" +
				"                    } else {\n" +
				"                        delSessionID();\n" +
				"                    }\n" +
				"                }\n" +
				"            };\n" +
				"        }\n" +
				"        \n" +
				"        function handleLoginSubmit() {\n" +
				"            var uname = document.getElementsByName(\"uname\")[0].value;\n" +
				"            var pword = document.getElementsByName(\"psw\")[0].value;\n" +
				"            var remember = document.getElementsByName(\"remember\")[0].checked;\n" +
				"            \n" +
				"            digestMessage(pword).then(pwordHash => {\n" +
				"                var xhr = new XMLHttpRequest();\n" +
				"                xhr.open('GET', \"/loginChecker?uname=\"+uname+\"&pswHash=\"+hexString(pwordHash), true);\n" +
				"                xhr.send();\n" +
				"                xhr.onreadystatechange = function (e) {\n" +
				"                    if (xhr.readyState == 4 && xhr.status == 200) {\n" +
				"                        sessionID = parseInt(xhr.responseText);\n" +
				"                        if(sessionID > -1) {\n" +
				"                            if(remember) {\n" +
				"                                setSessionID(sessionID, 5);\n" +
				"							} else {\n" +
				"								setSessionID(sessionID, 0); // session only\n" +
				"							}\n" +
				"                            window.location.assign(\""+homePageAddress+"?"+SESSION_ID_VAR_NAME+"=\"+getSessionID());\n" +
				"                        } else {\n" +
				"                            alert(\"Incorrect username or password!\");\n" +
				"                        }\n" +
				"                    } else if (xhr.readyState == 4) {\n" +
				"						alert(xhr.status + \" \" + xhr.responseText);\n" +
				"					}\n" +
				"                };\n" +
				"            });\n" +
				"        }\n" +
				"    </script>\n" +
				"    \n" +
				"    <div class=\"container\">\n" +
				"        <div class=\"center\" style=\"border:1px solid gray;\">\n" +
				"            <iframe name=\"sinkFrame\" style=\"display:none;\"></iframe>\n" +
				"            <form onSubmit=\"JavaScript:handleLoginSubmit()\" style=\"text-align:center; margin: 10px\" target=\"sinkFrame\">\n" +
				"                <label for=\"uname\"><b>Username</b></label>\n" +
				"                <input type=\"text\" placeholder=\"Enter Username\" name=\"uname\" required>\n" +
				"                <br>\n" +
				"                <label for=\"psw\"><b>Password</b></label>\n" +
				"                <input type=\"password\" placeholder=\"Enter Password\" name=\"psw\" required>\n" +
				"                <br><br>\n" +
				"                <button type=\"submit\">Login</button>\n" +
				"                <br><br>\n" +
				"                <label>\n" +
				"                  <input type=\"checkbox\" name=\"remember\"> Remember me\n" +
				"                </label>\n" +
				"            </form>\n" +
				"        </div>\n" +
				"    </div>\n" +
				"\n" +
				"</body>\n" +
				"</html>";
	}
	
	private static String getSessionIDCodeJS() {
		return	"function getCookie(cname) {\n" +
				"    var name = cname + \"=\";\n" +
				"    var decodedCookie = decodeURIComponent(document.cookie);\n" +
				"    var ca = decodedCookie.split(';');\n" +
				"    for(var i = 0; i <ca.length; i++) {\n" +
				"        var c = ca[i];\n" +
				"        while (c.charAt(0) == ' ') {\n" +
				"            c = c.substring(1);\n" +
				"        }\n" +
				"        if (c.indexOf(name) == 0) {\n" +
				"            return c.substring(name.length, c.length);\n" +
				"        }\n" +
				"    }\n" +
				"    return \"\";\n" +
				"}\n" +
				"function delCookie(cname) {\n" +
				"    document.cookie = cname+\"=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;\";\n" +
				"}\n" +
				"function setCookie(cname, cvalue, expireDays) {\n" +
				"    var d = new Date();\n" +
				"    d.setTime(d.getTime() + (expireDays*24*60*60*1000));\n" +
				"    var expires = \"expires=\"+ d.toUTCString();\n" +
				"    document.cookie = cname + \"=\" + cvalue + \";\" + expires + \";path=/\";\n" +
				"}\n" +
				"function setSessionCookie(cname, cvalue) {\n" +
				"    document.cookie = cname + \"=\" + cvalue + \";path=/\";\n" +
				"}\n" +
				"\n" +
				"function sf298sWalkText(node, oldStr, newStr) {\n" +
				"    if (node.nodeType == 3)\n" +
				"        node.data = node.data.replace(oldStr, newStr);\n" +
				"    else if (node.nodeType == 1) {\n" +
				"        node.innerHTML = node.innerHTML.replace(oldStr, newStr);\n" +
				"        for(var i=0; i<node.attributes.length; i++)\n" +
				"            node.attributes[i].value = node.attributes[i].value.replace(oldStr, newStr);\n" +
				"    }\n" +
				"    if(node.nodeType == 1 && node.nodeName != \"SCRIPT\") {\n" +
				"        for (var i = 0; i < node.childNodes.length; i++) {\n" +
				"            sf298sWalkText(node.childNodes[i], oldStr, newStr);\n" +
				"        }\n" +
				"    }\n" +
				"}\n" +
				"function fillPageWithSessionID() {\n" +
				"    var sessionID = getSessionID();\n" +
				"    var re = new RegExp(\""+SESSION_ID_PLH+"\");\n" +
				"    sf298sWalkText(document.body, re, sessionID);\n" +
				"}\n" +
				"\n" +
				"function getSessionID() {\n" +
				"    var sessionID = getCookie(\"websiteSessionID\");\n" +
				"    if(sessionID.length > 0) return sessionID;\n" +
				"    return getUrlParameter(\""+SESSION_ID_VAR_NAME+"\");\n" +
				"}\n" +
				"function setSessionID(sessionID, expireDays) {\n" +
				"	if(expireDays == 0)\n" +
				"		setSessionCookie(\"websiteSessionID\", sessionID);\n" +
				"	else\n" +
				"		setCookie(\"websiteSessionID\", sessionID, expireDays);\n" +
				"}\n" +
				"function delSessionID() {\n" +
				"    delCookie(\"websiteSessionID\");\n" +
				"}\n" +
				"\n" +
				"function getUrlParameter(name) {\n" +
				"    name = name.replace(/[\\[]/, '\\\\[').replace(/[\\]]/, '\\\\]');\n" +
				"    var regex = new RegExp('[\\\\?&]' + name + '=([^&#]*)');\n" +
				"    var results = regex.exec(window.location.search);\n" +
				"    return results === null ? '' : decodeURIComponent(results[1].replace(/\\+/g, ' '));\n" +
				"};\n" +
				"function logout() {\n" +
				"    var xhr = new XMLHttpRequest();\n" +
				"    xhr.open('PUT', \"/logout?"+SESSION_ID_VAR_NAME+"=\"+getSessionID(), true);\n" +
				"    xhr.send();\n" +
				"    xhr.onreadystatechange = function (e) {\n" +
				"        if (xhr.readyState == 4 && xhr.status == 200) {\n" +
				"            delSessionID();\n" +
				"            window.location.replace(\"/login.html\");\n" +
				"        }\n" +
				"    };\n" +
				"}";
	}
	
    public static void printMessage(String line) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + "   " + line);
    }

}
