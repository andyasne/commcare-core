package org.commcare.cases.instance;

import org.commcare.cases.model.StorageBackedModel;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.expr.XPathPathExpr;

import java.util.Hashtable;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */

public class FixtureChildElement extends StorageBackedChildElement<StorageBackedModel> {
    private String name;
    private TreeElement empty;
    private Hashtable<XPathPathExpr, Hashtable<String, TreeElement[]>> childAttributeHintMap = null;

    protected FixtureChildElement(StorageInstanceTreeElement<StorageBackedModel, ?> parent,
                                  int mult, int recordId, String name) {
        super(parent, mult, recordId, null, null);
        this.name = name;
    }

    /*
     * Template constructor (For elements that need to create reference nodesets but never look up values)
     */
    private FixtureChildElement(StorageInstanceTreeElement<StorageBackedModel, ?> parent) {
        super(parent, TreeReference.INDEX_TEMPLATE, TreeReference.INDEX_TEMPLATE, null, null);

        empty = new TreeElement(name);
        empty.setMult(this.mult);
        empty.setAttribute(null, nameId, "");

        StorageBackedModel modelTemplate = parent.getModelTemplate();

        Hashtable<String, String> attributes = modelTemplate.getAttributes();
        for (String key : attributes.keySet()) {
            empty.setAttribute(null, key, "");
        }

        Hashtable<String, String> elements = modelTemplate.getElements();
        for (String key : elements.keySet()) {
            TreeElement scratch = new TreeElement(key);
            scratch.setAnswer(null);
            empty.addChild(scratch);
        }
    }

    @Override
    protected TreeElement cache() {
        if (recordId == TreeReference.INDEX_TEMPLATE) {
            return empty;
        }

        synchronized (parent.treeCache) {
            TreeElement element = parent.treeCache.retrieve(recordId);
            if (element != null) {
                return element;
            }

            TreeElement cacheBuilder = new TreeElement(name);

            StorageBackedModel model = parent.getElement(recordId);
            entityId = model.getEntityId();
            cacheBuilder.setMult(mult);

            Hashtable<String, String> attributes = model.getAttributes();
            for (String key : attributes.keySet()) {
                empty.setAttribute(null, key, attributes.get(key));
            }

            Hashtable<String, String> elements = model.getElements();
            for (String key : elements.keySet()) {
                TreeElement scratch = new TreeElement(key);
                String data = elements.get(key);
                // TODO PLM: do we want smarter type dispatch?
                scratch.setAnswer(new StringData(data == null ? "" : data));
                empty.addChild(scratch);
            }

            cacheBuilder.setParent(this.parent);

            parent.treeCache.register(recordId, cacheBuilder);

            return cacheBuilder;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public static FixtureChildElement buildFixtureChildTemplate(FlatFixtureInstanceTreeElement parent) {
        return new FixtureChildElement(parent);
    }
}
