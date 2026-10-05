package com.eltek;

import er.extensions.appserver.ERXSession;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXThreadStorage;

import com.eltekfw.model.Person;
import com.eltekfw.model.Vendor;

public class Session extends ERXSession {
	private static final long serialVersionUID = 1L;

	/** The logged-in user: a {@link Person}, a {@link Vendor}, or null when nobody is logged in. */
	private ERXGenericRecord user;

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
		publishCurrentPerson();
	}

	/** The logged-in user, either a Person or a Vendor. Use {@link #person()} / {@link #vendor()} for the typed object. */
	public ERXGenericRecord user() {
		return user;
	}

	/** The logged-in Person, or null if nobody is logged in or the user is a Vendor. */
	public Person person() {
		return (user instanceof Person p) ? p : null;
	}

	/** The logged-in Vendor, or null if nobody is logged in or the user is a Person. */
	public Vendor vendor() {
		return (user instanceof Vendor v) ? v : null;
	}

	public boolean isPerson() {
		return user instanceof Person;
	}

	public boolean isVendor() {
		return user instanceof Vendor;
	}

	/** "Person", "Vendor", or null when nobody is logged in. Meant for D2W rules: session.userType = 'Vendor'. */
	public String userType() {
		return (user == null) ? null : user.entityName();
	}

	/** Name to show for the logged-in user: a Person's first name or a Vendor's vendor name; null when nobody is logged in. */
	public String userDisplayName() {
		if (user instanceof Person p) {
			return p.firstName() + " " + p.lastName();
		}
		if (user instanceof Vendor v) {
			return v.vendorName();
		}
		return null;
	}

	/** True only for a logged-in Person flagged as administrator; a Vendor is never an administrator. */
	public boolean isAdministrator() {
		Person p = person();
		return p != null && Boolean.TRUE.equals(p.administrator());
	}

	/**
	 * Sets the logged-in user.
	 *
	 * @param u a Person, a Vendor, or null to clear the user
	 */
	public void setUser(ERXGenericRecord u) {
		if (u == null) {
			user = null;
		} else if (u instanceof Person p) {
			user = p.localInstanceIn(defaultEditingContext());
		} else if (u instanceof Vendor v) {
			user = v.localInstanceIn(defaultEditingContext());
		} else {
			throw new IllegalArgumentException("Session user must be a Person or a Vendor, not " + u.getClass().getName());
		}

		publishCurrentPerson();

		// Preferences belong to a Person; a Vendor only sees the global ones.
		if (isPerson()) {
			Person.ensurePreferences(person());
		}
	}

	/** Makes the logged-in Person (never a Vendor) available to the framework for this request. */
	private void publishCurrentPerson() {
		if (isPerson()) {
			ERXThreadStorage.takeValueForKey(person(), CURRENT_PERSON_KEY);
		} else {
			ERXThreadStorage.removeValueForKey(CURRENT_PERSON_KEY);
		}
	}

	public void sleep() {
		ERXThreadStorage.removeValueForKey(CURRENT_PERSON_KEY);
		super.sleep();
	}
}
