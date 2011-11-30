package org.commcare.entity;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.Suite;
import org.commcare.suite.model.Text;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.EntityFilter;
import org.javarosa.entity.model.Entity;

public class RecentFormEntity extends Entity<FormInstance> {
	public String schema;
	public Date dateSaved;
	/** XForm Namespace -> Entry Command Text **/
	Hashtable<String,Text> names;
	
	public RecentFormEntity(Vector<Suite> suites) {
		names = new Hashtable<String,Text>();
		for(Suite s : suites) {
			for(Enumeration en = s.getEntries().elements(); en.hasMoreElements() ;) {
				Entry entry = (Entry)en.nextElement();
				if(entry.getXFormNamespace() == null) {
					//This is a <view>, not an <entry>, so
					//it can't define a form
				} else {
					names.put(entry.getXFormNamespace(),entry.getText());
				}
			}
		}
	}
	
	public RecentFormEntity(Hashtable<String,Text> names) {
		this.names = names;
	}

	public String entityType() {
		return Localization.get("review.title");
	}

	public RecentFormEntity factory() {
		return new RecentFormEntity(names);
	}

	public void loadEntity(FormInstance dmt) {
		this.schema = dmt.schema;
		this.dateSaved = dmt.getDateSaved();
	}

	public boolean match (String key) {
		return true;
	}
	
	public int[] getStyleHints (boolean header) {
		if(header) {
			return new int[] {-1, -1} ;
		} else {
			return new int[] {-1, 36 };
		}
	}
	
	public String[] getHeaders(boolean detailed) {
		return new String[] {Localization.get("review.type"), Localization.get("review.date")}; 
	}
	
	public String[] getLongFields(FormInstance dmt) {
		return getShortFields();
	}

	public String[] getShortFields() {
		return new String[] {getTypeName(schema), DateUtils.formatDate(dateSaved, DateUtils.FORMAT_HUMAN_READABLE_SHORT) };
	}
	
	public String getTypeName (String schema) {
		if(names.containsKey(schema)) {
			return names.get(schema).evaluate();
		}
		return Localization.get("review.type.unknown");
	}
	
	public String[] getSortFields () {
		return new String[] {"DATE"};
	}
	
	public String[] getSortFieldNames () {
		return new String[] {Localization.get("review.date")};
	}
	
	public Object getSortKey (String fieldKey) {
		if (fieldKey.equals("DATE")) {
			return new Long(-dateSaved.getTime());
		} else {
			throw new RuntimeException("Sort Key [" + fieldKey + "] is not supported by this entity");
		}
	}
	
	public EntityFilter<FormInstance> getFilter () {
		return new EntityFilter<FormInstance> () {
			public boolean matches(FormInstance e) {
				return DateUtils.dateDiff(e.getDateSaved(), DateUtils.today()) <= 7;
			}
		};
	}
	
	public String getStyleKey () {
		return "model";
	}

	protected int readEntityId(FormInstance e) {
		return e.getID();
	}
}
