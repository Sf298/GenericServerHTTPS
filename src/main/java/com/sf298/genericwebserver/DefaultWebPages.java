/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

/**
 *
 * @author saud
 */
public class DefaultWebPages {
	
	public static String getTokenCodeJS() {
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
				"function fillPageWithToken() {\n" +
				"    var links = document.getElementsByTagName(\"a\");\n" +
				"    var token = getToken();\n" +
				"    var re = new RegExp(\"0twtcht4m\"+\"token\"+\"0thv303c\");\n" +
				"    for(var i=0; i<links.length; i++) {\n" +
				"        links[i].href = links[i].href.replace(re, token);\n" +
				"    }\n" +
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
	
	/**
	 * requires homepage, login checker, token.js 
	 * @param title
	 * @param homePageAddress eg "/home.html"
	 * @return 
	 */
	public static String getLoginPage(String title, String homePageAddress) {
		return	"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"	<title>"+title+"</title>\n" +
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
	
}
