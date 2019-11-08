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
public interface UserManager <T> {
	
	public void addUser(T userID, String unsaltedHash);
	
	public void removeUser(T userID);
	
	public int newSessionID(T userID);
	
	public boolean checkSessionID(int sessionID);
	
	public T getUserID(int sessionID);
	
	public boolean tryLogin(T userID, String password);
	
	public void logout(int sessionID);
	
	public void logoutUser(int sessionID);
	
	public void clearSessionIDs();
	
	public PagesAccessChecker getPAC();
	
	/**
	 * 
	 * @param pac the pages access checker used to determine if a user needs to
	 * be logged in to access the page.
	 */
	public void setPAC(PagesAccessChecker pac);
	
}
