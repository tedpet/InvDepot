package com.eltek;

import com.webobjects.appserver.WOComponent;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.directtoweb.D2WPage;
import com.webobjects.directtoweb.EditPageInterface;
import com.webobjects.directtoweb.ErrorPageInterface;
import com.webobjects.directtoweb.ListPageInterface;
import com.webobjects.directtoweb.QueryPageInterface;
import com.webobjects.eoaccess.EODatabaseDataSource;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;

import er.extensions.appserver.ERXWOContext;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXFetchSpecification;

import com.eltekfw.model.Person;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eltekfw.model.Client;
import com.eltekfw.model.Invoice;
import com.eltekfw.model.Vendor;
import com.eltekfw.model.Preference;


public class MainNavigationController {

	private static final Logger log = LoggerFactory.getLogger(MainNavigationController.class);

	private Session _session;
	public String PERSON = "Person";
	public String VENDOR = "Vendor";
	public String INVOICE = "Invoice";
	public String PREFERENCE = "Preference";

	public MainNavigationController(Session s) {
		super();
		_session = s;
	}

	// NAV ACTIONS
	
	/** Home page for the logged-in user: a Vendor lands on its own invoices, a Person on the default D2W page. */
	public WOComponent homeAction() {
		if (session().isVendor()) {
			return vendorHomeAction();
		}
		return personHomeAction();
	}

	/** Home for a Person: the default D2W page. */
	public WOComponent personHomeAction() {
		return D2W.factory().defaultPage(session());
	}

	/** Home for a Vendor: the list of that vendor's own invoices. */
	public WOComponent vendorHomeAction() {
		EOEditingContext ec = ERXEC.newEditingContext();
		ec.lock();

		ListPageInterface lpi;
		try {
			Vendor vendor = session().vendor().localInstanceIn(ec);

			EODatabaseDataSource ds = new EODatabaseDataSource(ec, INVOICE);

			ERXFetchSpecification<Invoice> fs = new ERXFetchSpecification<Invoice>(Invoice.ENTITY_NAME,
					Invoice.VENDOR.eq(vendor), null);

			ds.setFetchSpecification(fs);

			lpi = D2W.factory().listPageForEntityNamed(Invoice.ENTITY_NAME, session());
			lpi.setDataSource(ds);

		} finally {
			ec.unlock();
		}
		return (WOComponent) lpi;
	}
	
	public WOComponent preferencesAction() {
		EOEditingContext ec = ERXEC.newEditingContext();
		ec.lock();

		ListPageInterface lpi;
		try {
			EODatabaseDataSource ds = new EODatabaseDataSource(ec, PREFERENCE);

			ERXFetchSpecification<Preference> fs = new ERXFetchSpecification<Preference>(Preference.ENTITY_NAME,
					null, null);

			ds.setFetchSpecification(fs);

			lpi = D2W.factory().listPageForEntityNamed(Preference.ENTITY_NAME, session());
			lpi.setDataSource(ds);

		} finally {
			ec.unlock();
		}
		return (WOComponent) lpi;

		
	}
	
	public WOComponent listPersonAction() {
		EOEditingContext ec = ERXEC.newEditingContext();
		ec.lock();

		ListPageInterface lpi;
		try {
			EODatabaseDataSource ds = new EODatabaseDataSource(ec, PERSON);

			ERXFetchSpecification<Person> fs = new ERXFetchSpecification<Person>(Person.ENTITY_NAME,
					Person.CURRENT.eq(true), null);

			ds.setFetchSpecification(fs);

			lpi = D2W.factory().listPageForEntityNamed(Person.ENTITY_NAME, session());
			lpi.setDataSource(ds);

		} finally {
			ec.unlock();
		}
		return (WOComponent) lpi;

	}

	public WOComponent queryPersonAction() {
		return queryPageForEntityName(PERSON);
	}

	public WOComponent createPersonAction() {
		return newObjectForEntityName(PERSON);
	}
	
	public WOComponent editCurrentUser() {
	    log.debug("entered editCurrentUser()");

	    Session session = (Session) session();

	    String pageConfiguration;
	    if (session.isPerson()) {
	        pageConfiguration = "EditSinglePerson";
	    } else if (session.isVendor()) {
	        pageConfiguration = "EditSingleVendor";
	    } else {
	        throw new IllegalStateException("editCurrentUser() called for a session that is neither a person nor a vendor");
	    }

	    EOEditingContext ec = ERXEC.newEditingContext();
	    EOEnterpriseObject user;
	    ec.lock();
	    try {
	        user = ERXEOControlUtilities.localInstanceOfObject(ec, (EOEnterpriseObject) session.user());
	    } finally {
	        ec.unlock();
	    }

	    EditPageInterface epi = (EditPageInterface) D2W.factory().pageForConfigurationNamed(pageConfiguration, session);
	    epi.setObject(user);
	    epi.setNextPage(ERXWOContext.currentContext().page());

	    if (epi instanceof D2WPage) {
	        D2WContext d2wContext = ((D2WPage) epi).d2wContext();
	        d2wContext.takeValueForKey("EditSingle", "navigationState");
	        log.debug("pageConfiguration = {}, navigationState = {}", pageConfiguration, d2wContext.valueForKey("navigationState"));
	    }

	    return (WOComponent) epi;
	}
	
	//                    Vendor Area
	
	public WOComponent listVendorAction() {
		EOEditingContext ec = ERXEC.newEditingContext();
		ec.lock();

		ListPageInterface lpi;
		try {
			EODatabaseDataSource ds = new EODatabaseDataSource(ec, VENDOR);

			ERXFetchSpecification<Person> fs = new ERXFetchSpecification<Person>(Vendor.ENTITY_NAME,
					Vendor.CURRENT.eq(true), null);

			ds.setFetchSpecification(fs);

			lpi = D2W.factory().listPageForEntityNamed(Vendor.ENTITY_NAME, session());
			lpi.setDataSource(ds);

//				Not needed as the D2Wfactory sets up the navigationState
			// ((D2WPage) lpi).d2wContext().takeValueForKey("Person", "navigationState");
		} finally {
			ec.unlock();
		}
		return (WOComponent) lpi;

	}

	public WOComponent queryVendorAction() {
		return queryPageForEntityName(VENDOR);
	}

	public WOComponent createVendorAction() {
		return newObjectForEntityName(VENDOR);
	}
	
	//                    Invoice Area
	
	public WOComponent listInvoiceAction() {
		
		  EOEditingContext ec = ERXEC.newEditingContext(); ec.lock();
		  
		  ListPageInterface lpi; try { EODatabaseDataSource ds = new
		  EODatabaseDataSource(ec, INVOICE);
		  
		  ERXFetchSpecification<Invoice> fs = new
		  ERXFetchSpecification<Invoice>(Invoice.ENTITY_NAME, Invoice.PAID.eq(false),
		  null);
		  
		  ds.setFetchSpecification(fs);
		  
		  lpi = D2W.factory().listPageForEntityNamed(Invoice.ENTITY_NAME, session());
		  lpi.setDataSource(ds);
		  
		  } finally { ec.unlock(); } return (WOComponent) lpi;
		 
	

	}

	public WOComponent queryInvoiceAction() {
		return queryPageForEntityName(INVOICE);
	}

//	public WOComponent createInvoiceAction() {
//		return newObjectForEntityName(INVOICE);
//	}
	
	public WOComponent createInvoiceAction() {
	    WOComponent page = newObjectForEntityName(INVOICE);

	    // Skip if we got the error page back
	    if (page instanceof EditPageInterface && page instanceof D2WPage) {
	        EOEnterpriseObject invoice = ((D2WPage) page).object();
	        if (invoice != null) {
	            EOEditingContext ec = invoice.editingContext();
	            ec.lock();
	            try {
	                Vendor vendor = ((Session) session()).vendor();
	                Vendor localVendor = (Vendor) EOUtilities.localInstanceOfObject(ec, vendor);
	                invoice.addObjectToBothSidesOfRelationshipWithKey(localVendor, "vendor");
	            } finally {
	                ec.unlock();
	            }
	        }
	    }
	    return page;
	}
	
	
	// GENERIC ACTIONS
	
    public WOComponent queryPageForEntityName(String entityName) {
        QueryPageInterface newQueryPage = D2W.factory().queryPageForEntityNamed(entityName, session());
        return (WOComponent) newQueryPage;
    }
    
    public WOComponent newObjectForEntityName(String entityName) {
        WOComponent nextPage = null;
        try {
            EditPageInterface epi = D2W.factory().editPageForNewObjectWithEntityNamed(entityName, session());
            epi.setNextPage(session().context().page());
            nextPage = (WOComponent) epi;
        } catch (IllegalArgumentException e) {
            ErrorPageInterface epf = D2W.factory().errorPage(session());
            epf.setMessage(e.toString());
            epf.setNextPage(session().context().page());
            nextPage = (WOComponent) epf;
        }
        return nextPage;
    }
    
    // ACCESSORS
    
    public Session session() {
		return _session;
	}

	public void setSession(Session s) {
		_session = s;
	}
}
