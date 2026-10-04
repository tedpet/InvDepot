package com.eltek;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOEditingContext;

import er.directtoweb.ERD2WDirectAction;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXStringUtilities;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eltek.components.Main;
import com.eltekfw.model.Person;
import com.eltekfw.model.Vendor;


public class DirectAction extends ERD2WDirectAction {
	
	private static final Logger log = LoggerFactory.getLogger(DirectAction.class);

	public DirectAction(WORequest request) {
		super(request);
	}

	@Override
	public WOActionResults defaultAction() {
		return pageWithName(Main.class.getName());
	}
	
    /**
     * Checks if a page configuration is allowed to render.
     * Provide a more intelligent access scheme as the default just returns false. And
     * be sure to read the javadoc to the super class.
     * @param pageConfiguration
     * @return
     */
    protected boolean allowPageConfiguration(String pageConfiguration) {
        return false;
    }
    
	public WOActionResults loginAction() {
		log.debug("loginAction called");

		String username = request().stringFormValueForKey("username");
		String password = request().stringFormValueForKey("password");

		String errorMessage = null;

		if (ERXStringUtilities.stringIsNullOrEmpty(username) || ERXStringUtilities.stringIsNullOrEmpty(password)) {
			errorMessage = "Please enter a username and password.";
		} else {
			try {
				ERXGenericRecord user = validateLogin(username, password);
				if (user != null) {
					Session session = (Session) session();
					session.setUser(user);
					return session.navController().homeAction();
				}
				errorMessage = "No user found for that combination of username and password.";
			} catch (Exception e) {
				log.error("Login failed for username {}", username, e);
				errorMessage = "Some error other than a bad username and password combination: " + e;
			}
		}

		log.debug("Login failed: {}", errorMessage);
		WOComponent nextPage = pageWithName(Main.class.getName());
		nextPage.takeValueForKey(errorMessage, "errorMessage");
		nextPage.takeValueForKey(username, "username");
		nextPage.takeValueForKey(password, "password");
		return nextPage;
	}

	/**
	 * Looks the login up as a Person first, then as a Vendor.
	 *
	 * @return the matching Person or Vendor, or null if neither matches
	 */
	private ERXGenericRecord validateLogin(String username, String password) {
		EOEditingContext ec = ERXEC.newEditingContext();
		try {
			return Person.validateLogin(ec, username, password);
		} catch (NoSuchElementException e) {
			log.debug("No Person matches username {}; trying Vendor", username);
		}
		try {
			return Vendor.validateLogin(ec, username, password);
		} catch (NoSuchElementException e) {
			log.debug("No Vendor matches username {}", username);
		}
		return null;
	}

}
