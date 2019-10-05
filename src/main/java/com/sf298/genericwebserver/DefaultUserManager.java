/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sf298.genericwebserver;

import sauds.toolbox.Encryptor;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import sauds.toolbox.PropertiesFile;

/**
 * Performs all the passwords checking and token handling for users.
 * @author saud
 */
public class DefaultUserManager implements UserManager<String> {
    
	public final HashMap<Integer, String> tokens = new HashMap<>();
	private final Random r = new Random();
	private final int HASH_LEN = Encryptor.hashSHA256("a", "a").length();
	//private final String encryptionKey = Encryptor.genKey("npauvfnpfjlksmnvnpfd");
	private Thread fileLoaderThread;
	
	private final PropertiesFile users;
	private String hashSalt;
	private String fileEncryptorKey;
	private PagesAccessChecker pac;
	
	/**
	 * Creates a new user manager instance.
	 * @param scanUserFile Whether to scan for changes in the users file every 10 sec.
	 * @param hashSalt The salt to use when hashing the passwords.
	 * @param fileEncryptorKey The key to use when encrypting the save file. If
	 * null, the file is not encrypted.
	 */
	public DefaultUserManager(boolean scanUserFile, String hashSalt, String fileEncryptorKey) {
		this(new File("./users.prop"), scanUserFile, hashSalt, fileEncryptorKey);
	}

	/**
	 * Creates a new user manager instance
	 * @param usersFile The file path to store the usernames and password hashes
	 * @param scanUserFile Whether to scan for changes in the users file every 10 sec.
	 * @param hashSalt The salt to use when hashing the passwords.
	 * @param fileEncryptorKey The key to use when encrypting the save file. If
	 * null, the file is not encrypted.
	 */
	public DefaultUserManager(File usersFile, boolean scanUserFile, String hashSalt, String fileEncryptorKey) {
		this.users = new PropertiesFile(usersFile, " = ");
		this.hashSalt = hashSalt;
		this.fileEncryptorKey = fileEncryptorKey;
		this.pac = pac;
		if(!users.fileExists()) {
			System.out.println("File 'users.prop' not found. Creating...");
			users.save(fileEncryptorKey);
		} else {
			loadUserData();
		}
		if(scanUserFile) {
			if(fileLoaderThread!=null) fileLoaderThread.interrupt();
			fileLoaderThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!Thread.currentThread().isInterrupted()) {
					loadUserData();
					try {
						Thread.sleep(10*1000);
					} catch (InterruptedException ex) {
						return;
					}
				}
			}
		});
		fileLoaderThread.start();
		}
		/*
		Zf6j0V2HKgkk9tLarewYG

		*/
	}
	
	public void stop() {
		fileLoaderThread.interrupt();
	}
	
	/**
	 * Loads data from the users file.
	 */
	public void loadUserData() {
		users.load(fileEncryptorKey);
		
		HashSet<String> toHash = new HashSet<>(); // hash unhashed passwords
		for(Map.Entry<String, String> entry : users.getMap().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if(value.length() != HASH_LEN || value.matches("[^0-9a-f]")) { // check is hash
				toHash.add(key);
			}
		}
		for(String key : toHash) {
			users.put(key, Encryptor.hashSHA256(users.get(key),"",hashSalt));
		}
		if(!toHash.isEmpty()) {
			users.save(fileEncryptorKey);
		}
	}
	
	/**
	 * Checks if the provided password matches the password stored.
	 * @param uname The unique username for the user.
	 * @param unsaltedHash The password, unsalted and pre-hashed with SHA-256.
	 * @return Whether or not the password matches the stored password.
	 */
	@Override
	public boolean tryLogin(String uname, String unsaltedHash) {
		uname = uname.toLowerCase();
		String saltedHash = Encryptor.hashSHA256(unsaltedHash,hashSalt);
		//System.out.println(users.get(uname));
		//System.out.println(saltedHash);
		return users.hasKey(uname) && users.get(uname).equals(saltedHash);
	}
	
	/**
	 * Add a new user to the manager. If the user exists, changes the previously
	 * stored password.
	 * @param uname the username
	 * @param unsaltedHash the password, unsalted and pre-hashed with SHA-256
	 */
	@Override
	public void addUser(String uname, String unsaltedHash) {
		users.put(uname.toLowerCase(), Encryptor.hashSHA256(unsaltedHash,hashSalt));
		resetBatchSaveThread();
	}
	
	/**
	 * Remove a user from the manager.
	 * @param uname The username.
	 */
	@Override
	public void removeUser(String uname) {
		users.remove(uname);
		resetBatchSaveThread();
	}
	private Thread batchSaveThread;
	private void resetBatchSaveThread() {
		if(batchSaveThread != null) batchSaveThread.interrupt();
		batchSaveThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
					return;
				}
				users.save(fileEncryptorKey);
			}
		});
		batchSaveThread.start();
	}
	
	
	/**
	 * Get a new token for the given username.
	 * @param uname A username that exists in the manager.
	 * @return returns the new token, or -1 if the user does not exist.
	 */
	@Override
	public int newToken(String uname) {
		if(!users.hasKey(uname)) return -1;
		
		int t;
		do {
			t = r.nextInt(Integer.MAX_VALUE);
			t = Math.abs(t);
		} while(tokens.keySet().contains(t));

		tokens.put(t, uname);
		return t;
	}
	
	/**
	 * Checks if a token has been issued.
	 * @param token
	 * @return 
	 */
	@Override
	public boolean checkToken(int token) {
		if(token == -1) return false;
		return tokens.containsKey(token);
	}
	
	/**
	 * Gets the username associated with the given token.
	 * @param token
	 * @return The username of the associated user.
	 */
	@Override
	public String getUserID(int token) {
		return tokens.getOrDefault(token, null);
	}
	
	/**
	 * Removes all stored tokens.
	 */
	@Override
	public void clearTokens() {
		tokens.clear();
	}
	
	/**
	 * Removes the selected token.
	 * @param token The token to remove.
	 */
	@Override
	public void logout(int token) {
		tokens.remove(token);
	}
	
	/**
	 * Logs out all tokens issued to a given user.
	 * @param token Any token issued to the user.
	 */
	@Override
	public void logoutUser(int token) {
		String uname = tokens.get(token);
		logoutUser(uname);
    }
	
	/**
	 * Logs out all tokens issued to a given user.
	 * @param uname The username.
	 */
	public void logoutUser(String uname) {
		HashSet<Integer> toRemove = new HashSet<>();
		for(Map.Entry<Integer, String> entry : tokens.entrySet()) {
			Integer key = entry.getKey();
			String value = entry.getValue();
			if(uname.equals(value))
			toRemove.add(key);
		}
		for(Integer integer : toRemove) {
			tokens.remove(integer);
		}
    }

	@Override
	public PagesAccessChecker getPAC() {
		return pac;
	}
	
	@Override
	public void setPAC(PagesAccessChecker pac) {
		this.pac = pac;
	}
    
}