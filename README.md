# GenericServerHTTPS
Provides tools to easily create a new HTTPS server.

## Tutorial
This tutorial explains how a server may be created. The completed file can be found here: [ExampleWebsite.java](https://github.com/Sf298/GenericServerHTTPS/blob/master/src/main/java/com/sf298/genericwebserver/ExampleWebsite.java)

### Create keystore file
HTTPS websites use public/private key encryption. The required keys can be generated using the "keytool" program that ships with the jdk. Below is an example of the code to run. The program will then ask a series of questions to help randomise the generated keys.

Note: the Java folder (and consequently the keytool program) may not be found in the system's path variable.
~~~~
keytool -genkey -keyalg RSA -alias alias -keystore mykey.keystore -storepass 123456 -keypass 456789
~~~~

### Create Website Server
The SHTMLServer class creates and hosts the actual server. It is also where all of the pages can added. It requires a port to host the server on, 443 is the standard for the HTTPS protocol.
~~~~
SHTMLServer server = new SHTMLServer(port, keystoreFilePath, storepass, keypass);
server.start();
~~~~

### Login system
This library includes tools to easily set up a user access system. 

#### Managing user accounts
The UserManager class maintains the useraccounts and session IDs. Passwords are hashed on the client then sent to the server, where they are salted and hashed again for storage. Usernames and their password hashes are stored in a properties folder in the same location as the server file. The users file can be stored in 2 ways:
 * encrypted - the entire file is encrypted, meaning user information must be updated programatically.
 * unencrypted - user names and password hashes are stored in plain text and therefore may be updated by directly editing the file. Any unhashed passwords are hashed appropriately when the file is read.
~~~~
boolean scanUserFile = false;
String hashSalt = "suhbgfogv", fileEncryptorKey = "lsjfnrjn";
UserManager um = new UserManager(scanUserFile, hashSalt, fileEncryptorKey);
~~~~

#### Adding the login pages
The login pages and services can be added to the server with:
~~~~
WSLoginInit.addToServer(server, um, "Website Title", "/home.html");
~~~~


### Adding Client-Side Resources
Client-side resources could consist of files, images, or complete pages (that may be served directly to the client without additional processing). These can be easily added to the server using the "WSFilesLoaderInit" class.

The root folder for all resources must be located in the "%project%\src\main\resources" folder. This is so that it may be packaged up easily. The class also supports the built in login system and the accessability of certain files may be specified using the filtering parameters.
~~~~
String resourcesPath = "WebPages";
boolean isBlackList = false;
WSFilesLoaderInit.addToServer(server, um, resourcesPath, isBlackList,
				"login.html",
				"loginPhoto.png");
~~~~

### Adding Custom Server-Side Pages or Services
This project uses [JLHTTP](https://www.freeutils.net/source/jlhttp/) and therefore follows the same adding context handlers. However the "WSLoginInit" and "UserManager" classes do provide additional functions to handle login data. Example:
~~~~
server.addContext("/home.html", new HTTPServer.ContextHandler() {
  @Override
  public int serve(HTTPServer.Request req, HTTPServer.Response resp) throws IOException {
    // ensure session ID is valid and return error if not
    if(!WSLoginInit.reqLoginValid(req, resp, um)) {
      return 0;
    }

    int sessionID = WSLoginInit.getSessionID(req);
    String uname = um.getUser(sessionID);

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
~~~~

## Credits
 * [JLHTTP](https://www.freeutils.net/source/jlhttp/) - provides the layer on top of which, this project runs.
