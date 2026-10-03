package com.eltek;

import er.extensions.appserver.ERXSession;
import er.extensions.foundation.ERXThreadStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eltekfw.model.Person;

import er.corebusinesslogic.ERCoreBusinessLogic;

public class Session extends ERXSession {
	private static final long serialVersionUID = 1L;
	
	private Person user;
	private static final Logger log = LoggerFactory.getLogger(Session.class);

	public static final String CURRENT_PERSON_KEY = "currentPerson";

	private MainNavigationController _navController;
	
	public Session() {
	}
	
	public MainNavigationController navController() {
		if (_navController == null) {
			_navController = new MainNavigationController(this);
		}
		return _navController;
	}
	
	public void awake() {
        super.awake();
        if (user() != null) {
        	ERXThreadStorage.takeValueForKey(user(), CURRENT_PERSON_KEY);
		}
     }

	public Person user() {
		return user;
	}
	
//	public void setUser(Person user) {
//		System.out.println("this is setUser in the Session");
//		log.debug( "this is the Session class  debug = {}" + "Test");
//		log.info( "this is the Session class info ");
//		
//		user = user;
//		ERXThreadStorage.takeValueForKey(user, CURRENT_PERSON_KEY);
//
//		ERCoreBusinessLogic.setActor(user);
//	}
	
	public void setUser(Person u) {
	    user = (u == null) ? null : u.localInstanceIn(defaultEditingContext());
	    ERXThreadStorage.takeValueForKey(user(), CURRENT_PERSON_KEY);
	    
	    if (user() != null) {
	        Person.ensurePreferences(user());
	    }
	}
	
	public void sleep() {
	    ERXThreadStorage.removeValueForKey(CURRENT_PERSON_KEY);
		super.sleep();
	}
}
