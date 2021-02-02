package org.commcare.cases.model;

import org.javarosa.core.model.utils.PreloadUtils;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.ExtWrapMapPoly;
import org.javarosa.core.util.externalizable.ExtWrapNullable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * NOTE: All new fields should be added to the case class using the "data" class,
 * as it demonstrated by the "userid" field. This prevents problems with datatype
 * representation across versions.
 *
 * @author Clayton Sims
 */
public class Case implements Persistable, IMetaData {
    public static final String USER_ID_KEY = "userid";
    public static final String EXTERNAL_ID_KEY = "external_id";
    public static final String CATEGORY_KEY = "category";
    public static final String STATE_KEY = "state";
    public static final String PATIENT_TYPE_KEY = "patient_type";
    public static final String CURRENT_STATUS_KEY = "current_status";
    public static final String STORAGE_KEY = "CASE";

    public static final String INDEX_CASE_ID = "case-id";
    public static final String INDEX_CASE_TYPE = "case-type";
    public static final String INDEX_CASE_STATUS = "case-status";
    public static final String INDEX_CASE_INDEX_PRE = "case-in-";
    public static final String INDEX_OWNER_ID = "owner-id";
    public static final String INDEX_EXTERNAL_ID = "external-id";
    public static final String INDEX_CATEGORY = "category";
    public static final String INDEX_STATE = "state";

    private static final String ATTACHMENT_PREFIX = "attachmentdata";

    protected String typeId;
    protected String id;
    protected String name;

    protected boolean closed = false;

    protected Date dateOpened;

    protected int recordId;

    protected Hashtable<String, Object> data = new Hashtable<>();

    protected Vector<CaseIndex> indices = new Vector<>();

    /**
     * NOTE: This constructor is for serialization only.
     */
    public Case() {
        dateOpened = new Date();
    }

    public Case(String name, String typeId) {
        setID(-1);
        this.name = name;
        this.typeId = typeId;
        dateOpened = new Date();
        setLastModified(dateOpened);
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    @Override
    public int getID() {
        return recordId;
    }

    @Override
    public void setID(int id) {
        this.recordId = id;
    }

    public String getUserId() {
        return (String)data.get(USER_ID_KEY);
    }

    public void setUserId(String id) {
        data.put(USER_ID_KEY, id);
    }

    public String getExternalId() {
        return (String)data.get(EXTERNAL_ID_KEY);
    }

    public void setExternalId(String id) {
        data.put(EXTERNAL_ID_KEY, id);
    }

    public String getCategory() {
        String category = (String)data.get(CATEGORY_KEY);
        if (category == null) {
            category = (String)data.get(PATIENT_TYPE_KEY);
        }
        return category;
    }

    public void setCategory(String id) {
        data.put(CATEGORY_KEY, id);
    }

    public String getState() {
        String state = (String)data.get(STATE_KEY);
        if (state == null) {
            state = (String)data.get(CURRENT_STATUS_KEY);
        }
        return state;
    }

    public void setState(String id) {
        data.put(STATE_KEY, id);
    }

    public void setCaseId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return id;
    }

    public Date getDateOpened() {
        return dateOpened;
    }

    public void setDateOpened(Date dateOpened) {
        this.dateOpened = dateOpened;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        typeId = ExtUtil.readString(in);
        id = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        name = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
        closed = ExtUtil.readBool(in);
        dateOpened = (Date)ExtUtil.read(in, new ExtWrapNullable(Date.class), pf);
        recordId = ExtUtil.readInt(in);
        indices = (Vector<CaseIndex>)ExtUtil.read(in, new ExtWrapList(CaseIndex.class), pf);
        data = (Hashtable)ExtUtil.read(in, new ExtWrapMapPoly(String.class, true), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeString(out, typeId);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(id));
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(name));
        ExtUtil.writeBool(out, closed);
        ExtUtil.write(out, new ExtWrapNullable(dateOpened));
        ExtUtil.writeNumeric(out, recordId);
        ExtUtil.write(out, new ExtWrapList(indices));
        ExtUtil.write(out, new ExtWrapMapPoly(data));
    }

    public void setProperty(String key, Object value) {
        this.data.put(key, value);
    }

    public Object getProperty(String key) {
        if ("case-id".equals(key)) {
            return id;
        }
        return data.get(key);
    }

    public String getPropertyString(String key) {
        Object o = this.getProperty(key);
        if (o instanceof String) {
            return (String)o;
        } else {
            //This is not good, but it's also the uniform matching that's used in the
            //xml transform, essentially.
            return PreloadUtils.wrapIndeterminedObject(o).uncast().getString();
        }
    }

    public Hashtable getProperties() {
        return data;
    }

    public String getRestorableType() {
        return "case";
    }

    @Override
    public Object getMetaData(String fieldName) {
        if (fieldName.equals(INDEX_CASE_ID)) {
            return id;
        } else if (fieldName.equals("case-type")) {
            return typeId;
        } else if (fieldName.equals(INDEX_CASE_STATUS)) {
            return closed ? "closed" : "open";
        } else if (fieldName.startsWith(INDEX_CASE_INDEX_PRE)) {
            String name = fieldName.substring(fieldName.lastIndexOf('-') + 1, fieldName.length());

            for (CaseIndex index : this.getIndices()) {
                if (index.getName().equals(name)) {
                    return index.getTarget();
                }
            }
            return "";
        } else if (fieldName.equals(INDEX_OWNER_ID)) {
            String ownerId = getUserId();
            return ownerId == null ? "" : ownerId;
        } else if (fieldName.equals(INDEX_EXTERNAL_ID)) {
            String externalId = getExternalId();
            return externalId == null ? "" : externalId;
        } else if (fieldName.equals(INDEX_CATEGORY)) {
            String category = getCategory();
            return category == null ? "" : category;
        } else if (fieldName.equals(INDEX_STATE)) {
            String state = getState();
            return state == null ? "" : state;
        } else {
            throw new IllegalArgumentException("No metadata field " + fieldName + " in the case storage system");
        }
    }

    @Override
    public String[] getMetaDataFields() {
        return new String[]{INDEX_CASE_ID, INDEX_CASE_TYPE, INDEX_CASE_STATUS, INDEX_OWNER_ID, INDEX_EXTERNAL_ID, INDEX_CATEGORY, INDEX_STATE};
    }

    /**
     * Deprecated, use setIndex(CaseIndex) in the future.
     */
    public void setIndex(String indexName, String caseType, String indexValue) {
        setIndex(new CaseIndex(indexName, caseType, indexValue));
    }

    /**
     * Sets the provided index in this case. If a case index already existed with
     * the same name, it will be replaced.
     *
     * Returns true if an index was replaced, false if an index was not
     */
    public boolean setIndex(CaseIndex index) {
        boolean indexReplaced = false;
        //remove existing indices at this name
        for (CaseIndex i : this.indices) {
            if (i.getName().equals(index.getName())) {
                this.indices.removeElement(i);
                indexReplaced = true;
                break;
            }
        }
        this.indices.addElement(index);
        return indexReplaced;
    }

    public Vector<CaseIndex> getIndices() {
        return indices;
    }

    public void updateAttachment(String attachmentName, String reference) {
        data.put(ATTACHMENT_PREFIX + attachmentName, reference);
    }

    public String getAttachmentSource(String attachmentName) {
        return (String)data.get(ATTACHMENT_PREFIX + attachmentName);
    }

    //this is so terrible it hurts. We'll be redoing this
    public Vector<String> getAttachments() {
        Vector<String> attachments = new Vector<>();
        for (Enumeration en = data.keys(); en.hasMoreElements(); ) {
            String name = (String)en.nextElement();
            if (name.startsWith(ATTACHMENT_PREFIX)) {
                attachments.addElement(name.substring(ATTACHMENT_PREFIX.length()));
            }
        }
        return attachments;
    }

    public void removeAttachment(String attachmentName) {
        data.remove(ATTACHMENT_PREFIX + attachmentName);
    }

    // ugh, adding stuff to case models sucks. Need to code up a transition scheme in android so we
    // can stop having shitty models.

    private static final String LAST_MODIFIED = "last_modified";

    public void setLastModified(Date lastModified) {
        if (lastModified == null) {
            throw new NullPointerException("Case date last modified cannot be null");
        }
        data.put(LAST_MODIFIED, lastModified);
    }

    public Date getLastModified() {
        if (!data.containsKey(LAST_MODIFIED)) {
            return getDateOpened();
        }
        return (Date)data.get(LAST_MODIFIED);
    }

    /**
     * Removes any potential indices with the provided index name.
     *
     * If the index doesn't currently exist, nothing is changed.
     *
     * @param indexName The name of a case index that should be removed.
     */
    public boolean removeIndex(String indexName) {
        CaseIndex toRemove = null;

        for (CaseIndex index : indices) {
            if (index.mName.equals(indexName)) {
                toRemove = index;
                break;
            }
        }

        if (toRemove != null) {
            indices.removeElement(toRemove);
            return true;
        }
        return false;
    }
}
