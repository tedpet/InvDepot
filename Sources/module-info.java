module com.eltek.InvDepot {
	exports com.eltek;
	exports com.eltek.components;
	
	requires org.slf4j;
	requires org.wocommunity.webobjects.directtoweb;
	requires org.wocommunity.webobjects.dtwgeneration;
	requires org.wocommunity.webobjects.eocontrol;
	requires org.wocommunity.webobjects.foundation;
	requires org.wocommunity.webobjects.webobjects;
	requires org.wocommunity.wonder.directtoweb;
	requires org.wocommunity.wonder.erextensions;
	
	requires com.eltekfw.InvDepotFW;
	
	requires java.desktop;
	requires org.apache.pdfbox;
	requires org.apache.poi.poi;
	
    // Needed by the Vert.x adaptor for HTTPS
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.util;

    // Needed by AttachmentDisplay (PDF and spreadsheet previews)
    requires org.apache.poi.ooxml;
    requires org.apache.poi.scratchpad;
    
    requires com.github.luben.zstd_jni;
	requires org.wocommunity.wonder.erattachment;
	requires org.wocommunity.wonder.ercorebusinesslogic;
}