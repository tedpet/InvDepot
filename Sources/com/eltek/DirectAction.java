package com.eltek;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WORequest;
import com.webobjects.directtoweb.D2W;
import com.webobjects.foundation.NSLog;

import er.directtoweb.ERD2WDirectAction;
import er.extensions.eof.ERXEC;
import er.extensions.foundation.ERXStringUtilities;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eltek.components.Main;
import com.eltekfw.model.Person;


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
    
	/*
	 * public WOActionResults loginAction() {
	 * 
	 * String username = request().stringFormValueForKey("username"); String
	 * password = request().stringFormValueForKey("password");
	 * 
	 * NSLog.out.appendln("***DirectAction.loginAction - username: " + username +
	 * " : password: " + password + "***");
	 * 
	 * // ENHANCEME - add appropriate login behaviour here
	 * 
	 * return D2W.factory().defaultPage(session()); }
	 *
	 */
    
    public WOActionResults loginAction() {
		log.debug("We called the WOActionResults loginAction()");
		log.info("We called the WOActionResults loginAction() for info level");
		WOComponent nextPage = null;

		String username = request().stringFormValueForKey("username");
		String password = request().stringFormValueForKey("password");
		
		boolean authFailed = true;

		String errorMessage = null;

		if (ERXStringUtilities.stringIsNullOrEmpty(username) || ERXStringUtilities.stringIsNullOrEmpty(password)){
			//there is something wrong so set the errorMessage
			errorMessage = "Please enter a username and password.";
		}
		else 
		{
			try {

				authFailed = false;
							
				Person user = Person.validateLogin(ERXEC.newEditingContext(), username, password);
				log.info("We called the validateLogin");
				((Session) session()).setUser(user);
				nextPage = ((Session) session()).navController().homeAction();
			
			}
			catch (NoSuchElementException e) {
				errorMessage = "No user found for that combination of username and password.";
				authFailed = true;
				
			}
			catch (Exception e) {
			  errorMessage = "Exception e)  Some Error other than bad username password combination: " + e;
			  authFailed = true;

			}
		}
		if (authFailed) {
			log.debug("authFailed ");
			nextPage = pageWithName(Main.class.getName());
			nextPage.takeValueForKey(errorMessage, "errorMessage");
			nextPage.takeValueForKey(username, "username");
			nextPage.takeValueForKey(password, "password");
		}

		return nextPage;
	}
	
}
