package org.commcare.xml;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import org.commcare.data.xml.SimpleNode;
import org.commcare.data.xml.TreeBuilder;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.RemoteRequestEntry;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.VirtualDataInstance;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class RemoteRequestEntryParserTest {

    @Test
    public void testParse() throws IOException, UnfullfilledRequirementsException, InvalidStructureException,
            XmlPullParserException, NoSuchFieldException, IllegalAccessException {
        String query = "<remote-request>\n"
                + "  <post url=\"https://www.fake.com/claim_patient/\">\n"
                + "    <data key=\"case_id\" ref=\"instance('session')/session/case_id\"/>\n"
                + "  </post>\n"
                + "  <command id=\"search\">\n"
                + "    <display>\n"
                + "      <text>Search</text>\n"
                + "    </display>\n"
                + "  </command>\n"
                + "</remote-request>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(query.getBytes("UTF-8"));
        KXmlParser parser = ElementParser.instantiateParser(inputStream);
        EntryParser entryParser = EntryParser.buildRemoteSyncParser(parser);
        RemoteRequestEntry entry = (RemoteRequestEntry)entryParser.parse();
        PostRequest post = entry.getPostRequest();
        Field paramsField = PostRequest.class.getDeclaredField("params");
        paramsField.setAccessible(true);
        List<QueryData> params = (List<QueryData>)paramsField.get(post);
        assertEquals(1, params.size());
        assertEquals("case_id", params.get(0).getKey());

        EvaluationContext evalContext = new EvaluationContext(null, buildSessionInstance());
        Multimap<String, String> evaluatedParams = post.getEvaluatedParams(evalContext);
        assertEquals(Arrays.asList("bang"), evaluatedParams.get("case_id"));
    }

    private Hashtable<String, DataInstance> buildSessionInstance() {
        List<SimpleNode> nodes = ImmutableList.of(
            SimpleNode.textNode("case_id", Collections.emptyMap(), "bang")
        );
        TreeElement root = TreeBuilder.buildTree("session", "session", nodes);
        Hashtable<String, DataInstance> instances = new Hashtable<>();
        instances.put("session", new VirtualDataInstance("session", root));
        return instances;
    }
}
