package com.eltek.components;

import com.eltek.Session;
import com.webobjects.appserver.WOContext;
import er.extensions.components.ERXStatelessComponent;

public class Main extends ERXStatelessComponent {
	
	private String _username;
	private String _password;
	private String _errorMessage;
	
	public Main(WOContext context) {
		super(context);
	}
	
	/** Typed session so bindings such as session.userDisplayName resolve against com.eltek.Session. */
	@Override
	public Session session() {
		return (Session) super.session();
	}

	public void setUsername(String username) {
		_username = username;
	}

	public String username() {
		return _username;
	}

	public void setPassword(String password) {
		_password = password;
	}

	public String password() {
		return _password;
	}

	public void setErrorMessage(String errorMessage) {
		_errorMessage = errorMessage;
	}

	public String errorMessage() {
		return _errorMessage;
	}
	
}
