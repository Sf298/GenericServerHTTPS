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
	 * A place holder String that gets replaced by the user's token on the client.
	 */
	public static String TOKEN_PLH = "0twtcht4mtoken0thv303c";
	
	public static String addTokenCode(String page) {
		String injCode1 = "\n<script src=\"/tokenCode.js\"></script>\n";
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
							"    fillPageWithToken();\n" +
							"</script>\n";
		if( (pos = firstRgx(page, "<.*\\/.*body.*>")) != null ) {
			page = insStr(page, injCode2, pos[0]);
		} else {
			page = insStr(page, injCode2, -1);
		}
		return page;
	}
	public static int[] firstRgx(String str, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(str);
		if(matcher.find()) {
			return new int[] {matcher.start(), matcher.end()};
		}
		return null;
	}
	public static String insStr(String str, String insStr, int i) { 
		if(i == -1) i = str.length();
        StringBuilder newString = new StringBuilder(str);
        newString.insert(i, insStr);
        return newString.toString(); 
    } 
	
	/**
	 * Gets the user token from the provided request.
	 * @param req the request
	 * @return the token found in the request or -1 if not found
	 * @throws IOException 
	 */
	public static int getToken(HTTPServer.Request req) throws IOException {
		Map<String, String> params = req.getParams();
		return Integer.parseInt(params.getOrDefault("token", "-1"));
	}
	
	/**
	 * Checks if the token belongs to a valid user account.
	 * @param req the request
	 * @param um the UserManager containing the user database
	 * @return true if the user exists, otherwise false
	 * @throws IOException 
	 */
	public static boolean reqLoginValid(HTTPServer.Request req, UserManager um) throws IOException {
		int token = getToken(req);
		return token!=-1 && um.checkToken(token);
	}
	
	/**
	 * Checks if the token belongs to a valid user account. Replies with a 403
	 * error, closes the stream and returns false if the token is invalid.
	 * @param req the request
	 * @param resp the response to write to
	 * @param um the UserManager containing the user database
	 * @return true if the user exists, otherwise false
	 * @throws IOException 
	 */
	public static boolean reqLoginValid(HTTPServer.Request req, HTTPServer.Response resp, UserManager um) throws IOException {
		if(um != null && !WSLoginInit.reqLoginValid(req, um)) {
			resp.sendError(403, "Invalid login");
			resp.close();
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Checks if the token belongs to a valid user account. Replies with a 403
	 * error, closes the stream and returns false if the token is invalid.
	 * Includes a filter for batch processing. If blacklisted, only logged in
	 * users can access resources in the filter.
	 * @param req the request
	 * @param resp the response to write to
	 * @param um the UserManager containing the user database
	 * @param isBlackList if the context filters are a white or black list
	 * @param contextFilter the contexts to filter
	 * @return true if the user exists, otherwise false
	 * @throws IOException 
	 */
	public static boolean reqLoginValid(HTTPServer.Request req, HTTPServer.Response resp,
			UserManager um, HashSet<String> contextFilter, boolean isBlackList) throws IOException {
		if(contextFilter==null) contextFilter = new HashSet<>();
		if(um != null
				&& isBlackList==contextFilter.contains(req.getContext().getPath())
				&& !WSLoginInit.reqLoginValid(req, um)) {
			resp.sendError(403, "Invalid login");
			resp.close();
			return false;
		} else {
			return true;
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
	public static void addToServer(SHTMLServer server, UserManager um, String websiteTitle, String homePageAddress) {
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
		server.addContext("/tokenCode.js", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				String page = getTokenCodeJS();
				resp.getHeaders().add("Content-Type", "text/javascript");
				resp.send(200, page);
				resp.close();
				return 0;
			}
		}, "GET");
	}
	private static void addServices(SHTMLServer server, UserManager um) {
		server.addContext("/logout", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				int token = getToken(req);
				if(token != -1) {
					um.logout(token);
				}
				resp.send(200, "done");
				resp.close();
				return 0;
			}
		}, "PUT");
		server.addContext("/logoutUser", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				int token = getToken(req);
				if(token != -1) {
					um.logoutUser(token);
				}
				resp.send(200, "done");
				resp.close();
				return 0;
			}
		}, "PUT");
		server.addContext("/loginChecker", new HTTPServer.ContextHandler() { // process token and login credentials
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				Map<String, String> params = req.getParams();
				if (params.containsKey("token")) {
					int token = Integer.parseInt(params.get("token"));
					resp.send(200, String.valueOf(um.checkToken(token)));
				} else if (params.containsKey("uname") && params.containsKey("pswHash")) {
					if (um.checkPasswordHash(params.get("uname"), params.get("pswHash"))) {
						printMessage("User: " + params.get("uname") + " logged in succesfully");
						resp.send(200, String.valueOf(um.newToken(params.get("uname"))));
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
	 * requires homepage, login checker, token.js 
	 * @param websiteTitle Title of the website.
	 * @param homePageAddress eg "/home.html".
	 * @return 
	 */
	private static String getLoginPage(String websiteTitle, String homePageAddress) {
		return	"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"	<title>"+websiteTitle+"</title>\n" +
				"	<meta charset=\"UTF-8\">\n" +
				"	<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
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
				"    <script src=\"/tokenCode.js\"></script>\n" +
				"    <script type=\"text/javascript\">\n" +
				"		function hexString(buffer) {\n" +
				"			const byteArray = new Uint8Array(buffer);\n" +
				"			const hexCodes = [...byteArray].map(value => {\n" +
				"				const hexCode = value.toString(16);\n" +
				"				const paddedHexCode = hexCode.padStart(2, '0');\n" +
				"				return paddedHexCode;\n" +
				"			});\n" +
				"			return hexCodes.join('');\n" +
				"		}\n" +
				"		function digestMessage(message) {\n" +
				"			const encoder = new TextEncoder();\n" +
				"			const data = encoder.encode(message);\n" +
				"			return window.crypto.subtle.digest('SHA-256', data);\n" +
				"		}\n" +
				"		\n" +
				"        var token = getCookie(\"websitetoken\");\n" +
				"        if(token != \"\") {\n" +
				"            var xhr = new XMLHttpRequest();\n" +
				"            xhr.open('GET', \"/loginChecker?token=\"+token, true);\n" +
				"            xhr.send();\n" +
				"            xhr.onreadystatechange = function (e) {\n" +
				"                if (xhr.readyState == 4 && xhr.status == 200) {\n" +
				"                    if(xhr.responseText == \"true\") {\n" +
				"                        window.location.replace(\""+homePageAddress+"?token=\"+token);\n" +
				"                    } else {\n" +
				"                        delCookie(\"websitetoken\");\n" +
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
				"			digestMessage(pword).then(pwordHash => {\n" +
				"				var xhr = new XMLHttpRequest();\n" +
				"				xhr.open('GET', \"/loginChecker?uname=\"+uname+\"&pswHash=\"+hexString(pwordHash), true);\n" +
				"				xhr.send();\n" +
				"				xhr.onreadystatechange = function (e) {\n" +
				"					if (xhr.readyState == 4 && xhr.status == 200) {\n" +
				"						token = parseInt(xhr.responseText);\n" +
				"						if(token > -1) {\n" +
				"							if(remember)\n" +
				"								setCookie(\"websitetoken\", token, 5);\n" +
				//"							alert(\"going to "+homePageAddress+"?token=\"+token);\n" +
				"							window.location.replace(\""+homePageAddress+"?token=\"+token);\n" +
				"						} else {\n" +
				"							alert(\"Incorrect username or password!\");\n" +
				"						}\n" +
				"					}\n" +
				"				};\n" +
				"			});\n" +
				"        }\n" +
				"    </script>\n" +
				"    \n" +
				"    <div class=\"container\">\n" +
				"        <div class=\"center\" style=\"border:1px solid gray;\">\n" +
				"            <form onSubmit=\"JavaScript:handleLoginSubmit()\" style=\"text-align:center; margin: 10px\">\n" +
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
				"</body>";
	}
	
	private static String getTokenCodeJS() {
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
				"      var d = new Date();\n" +
				"      d.setTime(d.getTime() + (expireDays*24*60*60*1000));\n" +
				"      var expires = \"expires=\"+ d.toUTCString();\n" +
				"      document.cookie = cname + \"=\" + cvalue + \";\" + expires + \";path=/\";\n" +
				"}\n" +
				"function sf298sWalkText(node, oldStr, newStr) {\n" +
				"  if (node.nodeType == 3)\n" +
				"	node.data = node.data.replace(oldStr, newStr);\n" +
				"  else if (node.nodeType == 1) {\n" +
				"	for(var i=0; i<node.attributes.length; i++)\n" +
				"	  node.attributes[i].value = node.attributes[i].value.replace(oldStr, newStr);\n" +
				"  }\n" +
				"  if(node.nodeType == 1 && node.nodeName != \"SCRIPT\") {\n" +
				"	for (var i = 0; i < node.childNodes.length; i++) {\n" +
				"	  sf298sWalkText(node.childNodes[i], oldStr, newStr);\n" +
				"	}\n" +
				"  }\n" +
				"}\n" +
				"function fillPageWithToken() {\n" +
				"    var token = getToken();\n" +
				"    var re = new RegExp(\"0twtcht4m\"+\"token\"+\"0thv303c\");\n" +
				"	sf298sWalkText(document.body, re, token);\n" +
				"}\n" +
				"function getToken() {\n" +
				"	var token = getCookie(\"websitetoken\");\n" +
				"	if(token.length > 0) return token;\n" +
				"	return getUrlParameter(\"token\");\n" +
				"}\n" +
				"function getUrlParameter(name) {\n" +
				"    name = name.replace(/[\\[]/, '\\\\[').replace(/[\\]]/, '\\\\]');\n" +
				"    var regex = new RegExp('[\\\\?&]' + name + '=([^&#]*)');\n" +
				"    var results = regex.exec(window.location.search);\n" +
				"    return results === null ? '' : decodeURIComponent(results[1].replace(/\\+/g, ' '));\n" +
				"};\n" +
				"function logout() {\n" +
				"    var xhr = new XMLHttpRequest();\n" +
				"    xhr.open('PUT', \"/logout?token=\"+getCookie(\"websitetoken\"), true);\n" +
				"    xhr.send();\n" +
				"    xhr.onreadystatechange = function (e) {\n" +
				"        if (xhr.readyState == 4 && xhr.status == 200) {\n" +
				"            delCookie(\"websitetoken\");\n" +
				"            window.location.replace(\"/login.html\");\n" +
				"        }\n" +
				"    };\n" +
				"}";
	}

	/*private static void addDynamicPages(SHTMLServer server, UserManager um, DevicesManager dm) {
		server.addContext("/home.html", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				Map<String, String> params = req.getParams();
				String tokenStr = params.get("token");
				if (tokenStr == null || !um.checkToken(Integer.parseInt(tokenStr))) {
					resp.sendError(403);
					return 0;
				}

				StringBuilder contentPartBuilder = new StringBuilder();
				for (Map.Entry<Integer, Device> entry : dm.getDeviceMap().entrySet()) {
					Integer deviceID = entry.getKey();
					Device device = entry.getValue();

					String deviceImgPath = null;

					if (device instanceof LightDevice) {
						deviceImgPath = "/imgs/lightbulb.png";
					}
					contentPartBuilder
							.append("<a href=\"/device.html?token=0twtcht4mtoken0thv303c&deviceid=").append(deviceID).append("\"><div class=\"card\"> \n")
							.append("    <img src=\"").append(deviceImgPath).append("\" alt=\"light bulb img\" width=\"100%\">\n")
							.append("    <p> ").append(device.getLabel()).append(" </p>\n")
							.append("</div></a><!--\n")
							.append("-->");
				}
				String contentPart;
				if (!dm.getDeviceMap().isEmpty()) {
					contentPart = contentPartBuilder.substring(0, contentPartBuilder.length() - 8);
				} else {
					contentPart = "No devices found";
				}

				// <editor-fold defaultstate="collapsed" desc=" Website Body ">
				String body = "<!DOCTYPE html>\n"
						+ "<html>\n"
						+ "<head>\n"
						+ "    <title>Everything Bridge</title>\n"
						+ "    <meta charset=\"UTF-8\">\n"
						+ "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
						+ "    \n"
						+ "    <style>\n"
						+ "        @media only screen and (min-device-width: 600px) {\n"
						+ "            .container {\n"
						+ "                margin: 0 25% 0 25%;\n"
						+ "            }\n"
						+ "            .card {\n"
						+ "                width : calc(25% - 10px - 20px);\n"
						+ "            }\n"
						+ "        }\n"
						+ "        @media only screen and (max-device-width: 600px) {\n"
						+ "            .card {\n"
						+ "                width : calc(50% - 10px - 20px);\n"
						+ "            }\n"
						+ "        }\n"
						+ "        .container {\n"
						+ "            text-align: center;\n"
						+ "        }\n"
						+ "        .card {\n"
						+ "            text-align : center;\n"
						+ "            border: 1px solid gray;\n"
						+ "            margin: 10px;\n"
						+ "            display: inline-block;\n"
						+ "        }\n"
						+ "        body {\n"
						+ "            font-family: Sans-Serif;\n"
						+ "            font-weight: bold;\n"
						+ "        }\n"
						+ "        a {\n"
						+ "            color: #3E3D3D;\n"
						+ "        }\n"
						+ "    </style>\n"
						+ "</head>\n"
						+ "\n"
						+ "<body class=\"w3-light-grey w3-content\" style=\"max-width:1600px\">\n"
						+ "    \n"
						+ "    <script src=\"/tokenCode.js\"></script>\n"
						+ "    \n"
						+ "    <div class=\"container\">\n"
						+ "        <h1> Devices </h1>\n"
						+ "        <div class=\"devicePanel\">\n"
						+ "            " + contentPart + "\n"
						+ "            \n"
						+ "            <!--<a href=\"/custom.html?token=0twtcht4mtoken0thv303c\">Custom Message</a><br>\n"
						+ "            <a href=\"/netflix.html?token=0twtcht4mtoken0thv303c\">Netflix Remote</a><br>\n"
						+ "            <a href=\"/mouse.html?token=0twtcht4mtoken0thv303c\">Mouse Control</a><br>\n"
						+ "            <a href=\"/power.html?token=0twtcht4mtoken0thv303c\">Power Control</a>\n"
						+ "            <br>\n"
						+ "            <button onclick=\"JavaScript:logout()\">Logout</button>-->\n"
						+ "        </div>\n"
						+ "    </div>\n"
						+ "\n"
						+ "\n"
						+ "    <script type=\"text/javascript\"> \n"
						+ "        fillPageWithToken();\n"
						+ "    </script>\n"
						+ "</body>";
				// </editor-fold>
				resp.send(200, body);
				return 0;
			}
		}, "GET");
		server.addContext("/device.html", new HTTPServer.ContextHandler() {
			@Override
			public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
				Map<String, String> params = req.getParams();
				String tokenStr = params.get("token");
				if (tokenStr == null || !um.checkToken(Integer.parseInt(tokenStr))) {
					resp.sendError(403);
					return 0;
				}
				Integer deviceID = Integer.parseInt(params.get("deviceid"));
				Device device = dm.getDevice(deviceID);

				StringBuilder contentPartBuilder = new StringBuilder();
				//<editor-fold defaultstate="collapsed" desc="add device controls">
				if (device instanceof OnOffDevice) {
					OnOffDevice temp = (OnOffDevice) device;
					contentPartBuilder
							.append("<p style=\"text-align:left;\">Power\n")
							.append("   <label class=\"switch\" style=\"float:right;\">\n")
							.append("	    <input name=\"devicePower\" id=\"powerSwitch\" type=\"checkbox\"").append((temp.getPowerState()) ? "checked" : "").append("/>\n")
							.append("	    <span class=\"slider round\"/>\n")
							.append("   </label>\n")
							.append("<p/>");
				}
				if (device instanceof LightDevice) {
					LightDevice temp = (LightDevice) device;
					contentPartBuilder
							.append("<p style=\"text-align:left;\">Brightness\n")
							.append("	<input name=\"lightBrightness\" id=\"brightnessSlider\" style=\"float:right; width:60%;\" type=\"range\" min=\"1\" max=\"100\" value=\"").append((int) (temp.getLightBrightness() * 100)).append("\" class=\"rangeslider\">\n")
							.append("<p/>");
				}
				if (device instanceof RGBLightDevice) {
					RGBLightDevice temp = (RGBLightDevice) device;
					Color c = temp.getLightColor().asColor();
					String hex = String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
					contentPartBuilder
							.append("<p style=\"text-align:left;\">Colour\n")
							.append("   <input name=\"lightColor\" style=\"float:right;\" class=\"jscolor\" value=\"").append(hex).append("\">\n")
							.append("<p/>");
				}

				if (device instanceof BlindsDevice) {
					contentPartBuilder.append("");
				}
				//</editor-fold>

				String contentPart;
				if (contentPartBuilder.length() > 0) {
					contentPart = contentPartBuilder.substring(0, contentPartBuilder.length() - 8);
				} else {
					contentPart = "Devices not found";
				}

				// <editor-fold defaultstate="collapsed" desc=" Website Body ">
				String body = "<!DOCTYPE html>\n"
						+ "<html>\n"
						+ "<head>\n"
						+ "    <title>Everything Bridge</title>\n"
						+ "    <meta charset=\"UTF-8\">\n"
						+ "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
						+ "    \n"
						+ "    <script src=\"./tokenCode.js\"></script>\n"
						+ "	\n"
						+ "	<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.4.0/jquery.min.js\"></script>\n"
						+ "	<script src=\"jscolor.js\"></script>\n"
						+ "	\n"
						+ "	<link href=\"./rangeSlider.css\" rel=\"stylesheet\">\n"
						+ "	<link href=\"./toggleSwitch.css\" rel=\"stylesheet\">\n"
						+ "	<style>\n"
						+ "        @media only screen and (min-device-width: 600px) {\n"
						+ "            .container {\n"
						+ "                margin: 0 25% 0 25%;\n"
						+ "            }\n"
						+ "            .card {\n"
						+ "                width : calc(25% - 10px - 20px);\n"
						+ "            }\n"
						+ "        }\n"
						+ "        @media only screen and (max-device-width: 600px) {\n"
						+ "            .card {\n"
						+ "                width : calc(50% - 10px - 20px);\n"
						+ "            }\n"
						+ "        }\n"
						+ "        body {\n"
						+ "            font-family: Sans-Serif;\n"
						+ "            font-weight: bold;\n"
						+ "        }\n"
						+ "        a {\n"
						+ "            color: #3E3D3D;\n"
						+ "        }\n"
						+ "    </style>\n"
						+ "</head>\n"
						+ "\n"
						+ "<body class=\"w3-light-grey w3-content\" style=\"max-width:1600px\">\n"
						+ "    \n"
						+ "    <div class=\"container\">\n"
						+ "        <h1> Devices </h1>\n"
						+ "		<br>\n"
						+ "		<form action=\"show_data.html\">\n"
						+ "			" + contentPart
						+ "		</form>\n"
						+ "    </div>\n"
						+ "\n"
						+ "    <script type=\"text/javascript\"> \n"
						+ "        fillPageWithToken();\n"
						+ "    </script>\n"
						+ "</body>";
				// </editor-fold>
				resp.send(200, body);
				return 0;
			}
		}, "GET");
	}*/
	
    public static void printMessage(String line) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + "   " + line);
    }

}
