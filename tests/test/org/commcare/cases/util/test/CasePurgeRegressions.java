package org.commcare.cases.util.test;

import org.commcare.cases.model.Case;
import org.javarosa.core.services.storage.IStorageIterator;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;
import org.javarosa.core.util.DataUtil;
import org.junit.Assert;

import org.commcare.cases.util.CasePurgeFilter;
import org.commcare.core.parse.ParseUtils;
import org.commcare.core.sandbox.SandboxUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.util.DAG;
import org.junit.Test;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import static org.junit.Assert.fail;

/**
 * Quick test to be able to restore a set of user data
 * and ensure users and groups are properly being included
 * in case purges.
 *
 * @author ctsims
 */
public class CasePurgeRegressions {

    @Test
    public void testSimpleExtensions() throws Exception {
        MockUserDataSandbox sandbox;
        Vector<String> owners;
        sandbox = MockDataUtils.getStaticStorage();

        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/simple_extension_test.xml"), sandbox);
        owners = SandboxUtils.extractEntityOwners(sandbox);

        CasePurgeFilter purger = new CasePurgeFilter(sandbox.getCaseStorage(), owners);
        int removedCases = sandbox.getCaseStorage().removeAll(purger).size();

        if (removedCases > 0) {
            throw new RuntimeException("Incorrectly removed cases");
        }
    }

    @Test
    public void testValidateCaseGraphBeforePurge_simple() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/validate_case_graph_test_simple.xml"), sandbox);
        IStorageUtilityIndexed<Case> storage = sandbox.getCaseStorage();

        HashMap<String, Integer> caseIdsToRecordIds = createCaseIdsMap(storage);
        CasePurgeFilter filter = new CasePurgeFilter(storage);

        Set<String> nodesExpectedToBeLeft = new HashSet<>();
        nodesExpectedToBeLeft.add("case_one");
        nodesExpectedToBeLeft.add("case_two");

        Set<String[]> edgesExpectedToBeLeft = new HashSet<>();
        edgesExpectedToBeLeft.add(new String[]{"case_two", "case_one"});

        // Check that the edges and nodes still present in the graph are as expected
        DAG<String, int[], String> internalCaseGraph = filter.getInternalCaseGraph();
        checkProperNodesPresent(nodesExpectedToBeLeft, internalCaseGraph);
        checkProperEdgesPresent(edgesExpectedToBeLeft, internalCaseGraph);

        // Check that the correct cases were actually purged
        Vector<Integer> expectedToRemove = new Vector<>();
        expectedToRemove.add(caseIdsToRecordIds.get("case_three"));
        Vector<Integer> removed = storage.removeAll(filter);
        checkProperCasesRemoved(expectedToRemove, removed);
    }

    @Test
    public void testValidateCaseGraphBeforePurge_multipleChildLevels() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/validate_case_graph_test_complex.xml"), sandbox);
        IStorageUtilityIndexed<Case> storage = sandbox.getCaseStorage();

        HashMap<String, Integer> caseIdsToRecordIds = createCaseIdsMap(storage);
        CasePurgeFilter filter = new CasePurgeFilter(storage);

        Set<String> nodesExpectedToBeLeft = new HashSet<>();
        nodesExpectedToBeLeft.add("case_one");
        nodesExpectedToBeLeft.add("case_two");

        Set<String[]> edgesExpectedToBeLeft = new HashSet<>();
        edgesExpectedToBeLeft.add(new String[]{"case_two", "case_one"});

        // Check that the edges and nodes still present in the graph are as expected
        DAG<String, int[], String> internalCaseGraph = filter.getInternalCaseGraph();
        checkProperNodesPresent(nodesExpectedToBeLeft, internalCaseGraph);
        checkProperEdgesPresent(edgesExpectedToBeLeft, internalCaseGraph);

        // Check that the correct cases were actually purged
        Vector<Integer> expectedToRemove = new Vector<>();
        expectedToRemove.add(caseIdsToRecordIds.get("case_three"));
        expectedToRemove.add(caseIdsToRecordIds.get("case_four"));
        expectedToRemove.add(caseIdsToRecordIds.get("case_five"));
        expectedToRemove.add(caseIdsToRecordIds.get("case_six"));
        expectedToRemove.add(caseIdsToRecordIds.get("case_seven"));
        Vector<Integer> removed = storage.removeAll(filter);
        checkProperCasesRemoved(expectedToRemove, removed);
    }

    @Test
    public void testValidateCaseGraphBeforePurge_twoChildrenSameLevel() throws Exception {

    }

    @Test
    public void testValidateCaseGraphBeforePurge_multipleParents() throws Exception {

    }

    @Test
    public void testValidateCaseGraphBeforePurge_noRemoval() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        ParseUtils.parseIntoSandbox(this.getClass().getClassLoader().
                getResourceAsStream("case_purge/validate_case_graph_test_no_change.xml"), sandbox);
        IStorageUtilityIndexed<Case> storage = sandbox.getCaseStorage();

        CasePurgeFilter filter = new CasePurgeFilter(storage);

        Set<String> nodesExpectedToBeLeft = new HashSet<>();
        nodesExpectedToBeLeft.add("case_one");
        nodesExpectedToBeLeft.add("case_two");
        nodesExpectedToBeLeft.add("case_three");
        nodesExpectedToBeLeft.add("case_four");

        Set<String[]> edgesExpectedToBeLeft = new HashSet<>();
        edgesExpectedToBeLeft.add(new String[]{"case_two", "case_one"});

        // Check that the edges and nodes still present in the graph are as expected
        DAG<String, int[], String> internalCaseGraph = filter.getInternalCaseGraph();
        checkProperNodesPresent(nodesExpectedToBeLeft, internalCaseGraph);
        checkProperEdgesPresent(edgesExpectedToBeLeft, internalCaseGraph);

        // Check that the correct cases (none int his case) were actually purged
        Vector<Integer> expectedToRemove = new Vector<>();
        Vector<Integer> removed = storage.removeAll(filter);
        checkProperCasesRemoved(expectedToRemove, removed);
    }

    /**
     * For all cases in this storage object, create a mapping from the case id to its record id,
     * so that we can later test that the correct record ids were removed
     */
    private static HashMap<String, Integer> createCaseIdsMap(
            IStorageUtilityIndexed<Case> storage) {
        IStorageIterator<Case> iterator = storage.iterate();
        HashMap<String, Integer> caseIdsToRecordIds = new HashMap<>();
        while (iterator.hasMore()) {
            Case c = iterator.nextRecord();
            caseIdsToRecordIds.put(c.getCaseId(), c.getID());
        }
        return caseIdsToRecordIds;
    }

    /**
     * Check that the set of nodes we expect to still be in the case DAG is identical to the
     * nodes actually there
     */
    private static void checkProperNodesPresent(Set<String> nodesExpected,
                                         DAG<String, int[], String> graph) {
        Set<String> nodesActuallyLeft = getSimpleFormNodes(graph.getIndices());
        Assert.assertTrue(nodesExpected.equals(nodesActuallyLeft));
    }

    /**
     * Check that the set of edges we expect to still be in the case DAG is identical to the
     * edges actually there
     */
    private static void checkProperEdgesPresent(Set<String[]> edgesExpected,
                                         DAG<String, int[], String> graph) {
        Set<String[]> edgesActuallyLeft = getSimpleFormEdges(graph.getEdges());
        for (String[] expected : edgesExpected) {
            Assert.assertTrue(checkContainsThisEdge(edgesActuallyLeft, expected));
        }
        for (String[] actual : edgesActuallyLeft) {
            Assert.assertTrue(checkContainsThisEdge(edgesExpected, actual));
        }
    }

    private static void checkProperCasesRemoved(Vector<Integer> expectedToRemove,
                                                Vector<Integer> removed) {
        // Check that the 2 vectors are same size
        Assert.assertTrue(removed.size() == expectedToRemove.size());

        // Check that every element in expectedToRmove is also in removed
        for (Integer caseId : expectedToRemove) {
            removed.removeElement(caseId);
        }

        // Check that the removed vector is empty now that all elements from expectedToRemove
        // were removed
        Assert.assertTrue(removed.size() == 0);
    }

    /**
     * Helper method for testing that a set of String[] contains the given String[], based upon
     * content value equality rather than reference equality
     */
    private static boolean checkContainsThisEdge(Set<String[]> setOfEdges, String[] edgeToFind) {
        for (String[] edge : setOfEdges) {
            if (Arrays.equals(edge, edgeToFind)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String[]> getSimpleFormEdges(
            Hashtable<String, Vector<DAG.Edge<String, String>>> edges) {
        Set<String[]> simpleFormEdges = new HashSet<>();
        for (String sourceIndex : edges.keySet()) {
            Vector<DAG.Edge<String, String>> edgesFromSource = edges.get(sourceIndex);
            for (DAG.Edge<String, String> edge : edgesFromSource) {
                simpleFormEdges.add(new String[]{sourceIndex, edge.i});
            }
        }
        return simpleFormEdges;
    }

    private static Set<String> getSimpleFormNodes(Enumeration e) {
        Set<String> simpleFormNodes = new HashSet<>();
        for (Enumeration iterator = e; iterator.hasMoreElements(); ) {
            simpleFormNodes.add((String)e.nextElement());
        }
        return simpleFormNodes;
    }

}
