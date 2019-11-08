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
public interface PagesAccessChecker {
	
	public static PagesAccessChecker ALLOWED = new PagesAccessChecker() {
		@Override
		public boolean allowed(String content, int sessionID) {
			return true;
		}
	};
	public static PagesAccessChecker BLOCKED = new PagesAccessChecker() {
		@Override
		public boolean allowed(String content, int sessionID) {
			return false;
		}
	};
	
	public boolean allowed(String content, int sessionID);
	
}
