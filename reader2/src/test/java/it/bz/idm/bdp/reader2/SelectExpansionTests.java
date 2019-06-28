package it.bz.idm.bdp.reader2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import it.bz.idm.bdp.reader2.utils.querybuilder.SelectExpansion;
import it.bz.idm.bdp.reader2.utils.querybuilder.SelectExpansion.ErrorCode;
import it.bz.idm.bdp.reader2.utils.querybuilder.SimpleException;

public class SelectExpansionTests {

	@Test
	public void testFlatStructure() {
		SelectExpansion se = new SelectExpansion();
		se.addColumn("A", "a", "A.a");
		se.addColumn("A", "b", "A.b");
		se.addColumn("B", "x", "B.x");
		se.addColumn("C", "i", "C.i");

		se.expand("a", "A", "C");

		// More select definitions than aliases
		List<String> res = se.getUsedAliases();
		assertEquals("a", res.get(0));
		assertTrue(res.size() == 1);

		// Alias that cannot be found
		try {
			se.expand("a, i, x", "A", "C");
			fail("Exception expected");
		} catch (SimpleException e) {
			assertEquals(ErrorCode.KEY_NOT_INSIDE_DEFLIST, e.getId());
			assertEquals("x", e.getData().get("alias"));
		}

	}

	@Test
	public void testNestedStructure() {
		SelectExpansion se = new SelectExpansion();
		se.addColumn("A", "a", "A.a");
		se.addColumn("A", "b", "A.b");
		se.addColumn("B", "x", "B.x");
		se.addSubDef("B", "y", "A");
		se.addColumn("C", "i", "C.i");
		se.addSubDef("C", "j", "B");
		se.addSubDef("E", "j", "B");
		se.addSubDef("A", "j", "A");


		// Select definitions inside a sub-expansion
		se.expand("a", "A", "C");
		List<String> res = se.getUsedAliases();
		assertEquals("a", res.get(0));
		assertTrue(res.size() == 1);

	}

	@Test
	public void testgetDefNames() throws Exception {
		SelectExpansion se = new SelectExpansion();
		se.addColumn("X", "h", "X.h");
		se.addColumn("A", "a", "A.a");
		se.addSubDef("A", "c", "X");
		se.addColumn("B", "x", "B.x");
		se.addSubDef("B", "y", "A");
		se.expand("a", "A");

		List<String> res = se.getUsedDefNames();
		assertEquals("A", res.get(0));
		assertTrue(res.size() == 1);
	}

	@Test
	public void testExpansion() throws Exception {
		SelectExpansion se = new SelectExpansion();
		se.addColumn("C", "h", "C.h");
		se.addColumn("A", "a", "A.a");
		se.addColumn("A", "b", "A.b");
		se.addSubDef("A", "c", "C");
		se.addColumn("B", "x", "B.x");
		se.addSubDef("B", "y", "A");


		se.expand("a", "A");
		assertEquals("a", se.getUsedAliases().get(0));
		assertEquals("A", se.getUsedDefNames().get(0));
		assertEquals("A.a as a", se.getExpansion().get("A"));
		assertTrue(se.getUsedAliases().size() == 1);
		assertTrue(se.getUsedDefNames().size() == 1);
		assertTrue(se.getExpansion().size() == 1);

		try {
			se.expand("a", "B");
			fail("Exception expected");
		} catch (Exception e) {
			// nothing to do
		}

		se.expand("a, b", "A", "B");
		assertEquals("a", se.getUsedAliases().get(0));
		assertEquals("b", se.getUsedAliases().get(1));
		assertEquals("A", se.getUsedDefNames().get(0));
		assertEquals("A.a as a, A.b as b", se.getExpansion().get("A"));
		assertTrue(se.getUsedAliases().size() == 2);
		assertTrue(se.getUsedDefNames().size() == 1);
		assertTrue(se.getExpansion().size() == 1);

		se.expand("x, y", "A", "B", "C");
		assertEquals("a", se.getUsedAliases().get(0));
		assertEquals("b", se.getUsedAliases().get(1));
		assertEquals("c", se.getUsedAliases().get(2));
		assertEquals("x", se.getUsedAliases().get(3));
		assertEquals("h", se.getUsedAliases().get(4));
		assertEquals("y", se.getUsedAliases().get(5));
		assertEquals("A", se.getUsedDefNames().get(0));
		assertEquals("B", se.getUsedDefNames().get(1));
		assertEquals("C", se.getUsedDefNames().get(2));
		assertEquals("A.a as a, A.b as b", se.getExpansion().get("A"));
		assertEquals("B.x as x", se.getExpansion().get("B"));
		assertEquals("C.h as h", se.getExpansion().get("C"));
		assertEquals(6, se.getUsedAliases().size());
		assertEquals(3, se.getUsedDefNames().size());
		assertEquals(3, se.getExpansion().size());

	}

	@Test
	public void testWhereClauseExpansion() {
		SelectExpansion se = new SelectExpansion();
		se.addColumn("A", "a", "A.a");
		se.addOperator("value", "eq", "= %s");
		se.addOperator("value", "neq", "<> %s");
		se.addOperator("null", "eq", "is null");
		se.addOperator("null", "neq", "is not null");
		se.addOperator("value", "lt", "< %s");
		se.addOperator("value", "gt", "> %s");
		se.addOperator("value", "lteq", "=< %s");
		se.addOperator("value", "gteq", ">= %s");
		se.addOperator("value", "re", "~ %s");
		se.addOperator("value", "ire", "~* %s");
		se.addOperator("value", "nre", "!~ %s");
		se.addOperator("value", "nire", "!~* %s");
		se.addOperator("list", "in", "in (%s)", t -> {
			return !(t.getChildren().size() == 1 && t.getChild("value").getValue() == null);
		});
		se.addOperator("list", "bbi", "&& ST_MakeEnvelope(%s)", t -> {
			return t.getChildren().size() == 4 || t.getChildren().size() == 5;
		});
		se.addOperator("list", "bbc", "@ ST_MakeEnvelope(%s)", t -> {
			return t.getChildren().size() == 4 || t.getChildren().size() == 5;
		});

		se.setWhereClause("a.bbi.(1,2,3,4,5,6)");
		try {
			se.expand("a", "A");
			fail("Exception expected; where clause bbi.<list> must have 4 or 5 elements");
		} catch (SimpleException e) {
			// nothing to do
		}

		se.setWhereClause("a.bbi.(1,2,3,4)");
		se.expand("a", "A");
		assertEquals("(A.a && ST_MakeEnvelope('1','2','3','4'))", se.getWhereSql());

		se.setWhereClause("a.in.()");
		se.expand("a", "A");
		assertEquals("(A.a in (''))", se.getWhereSql());

		se.setWhereClause("a.in.(null,null)");
		se.expand("a", "A");
		assertEquals("(A.a in (null,null))", se.getWhereSql());
}

}
