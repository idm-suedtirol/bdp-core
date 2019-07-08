package it.bz.idm.bdp.reader2.utils.querybuilder;

import it.bz.idm.bdp.reader2.utils.miniparser.MiniParser;
import it.bz.idm.bdp.reader2.utils.miniparser.Token;
import it.bz.idm.bdp.reader2.utils.querybuilder.SelectExpansion.ErrorCode;
import it.bz.idm.bdp.reader2.utils.simpleexception.SimpleException;

public class WhereClauseParser extends MiniParser {

	public WhereClauseParser(String input) {
		super(input);
	}

	private Token clauseOrLogicalOp() {
		return doSingle("CLAUSE_OR_LOGICAL_OP", t -> {
			if (matchConsume("and(")) {
				t.add(logicalOpAnd());
			} else if (matchConsume("or(")) {
				t.add(logicalOpOr());
			} else {
				t.add(clause());
			}
			if (matchConsume(',')) {
				t.combine(clauseOrLogicalOp());
			}
			return true;
		});
	}

	private Token alias() {
		Token res = doWhile("ALIAS", t -> {
			if (!Character.isLetter(c())) {
				System.out.println("AAA = " + c());
				return false;
			}
			t.appendValue(c());
			return true;
		});
		if (res.getValue() == null || res.getValue().isEmpty()) {
			throw new SimpleException(ErrorCode.WHERE_SYNTAX_ERROR, "Found character '" + encode(la(-1)) + "', but an ALIAS was expected");
		}
		System.out.println("ALIAS = " + res.getValue());
		return res;
	}

	private Token clause() {
		return doSingle("CLAUSE", t -> {
			Token alias = alias();
			expectConsume('.');
			Token operator = operator();
			expectConsume('.');
			Token listOrValue = listOrValue();
			t.add(alias);
			t.add(operator);
			t.combineForce(listOrValue);
			return true;
		});
	}

	private Token operator() {
		return doWhile("OP", t -> {
			if (!Character.isLetter(c())) {
				return false;
			}
			t.appendValue(c());
			return true;
		});
	}

	private Token listOrValue() {
		return doSingle("LIST_OR_VALUE", t -> {
			if (matchConsume('(')) {
				t.add(list());
				expectConsume(')');
			} else  {
				t.add(value());
			}
			return true;
		});
	}

	private Token list() {
		return doSingle("LIST", t -> {
			t.add(value());
			if (matchConsume(',')) {
				t.combine(list());
			}
			return true;
		});
	}

	private Token value() {
		Token res = doWhile("VALUE", t -> {
			if ((match('(') || match(')') || match(',') || match('\'')) && clash('\\', -1)) {
				return false;
			}
			matchConsume('\\');
			t.appendValue(c());
			return true;
		});
		if (res.valueIs(null)) {
			res.setValue("");
		} else if (res.valueIs("null")) {
			res.setName("null");
			res.setValue(null);
		}
		return res;
	}

	private Token logicalOpAnd() {
		Token res = doWhile("AND", t -> {
			t.combineForce(clauseOrLogicalOp());
			if (matchConsume(',')) {
				t.combineForce(clauseOrLogicalOp());
			}
			return clashConsume(')');
		});
		expectConsume(')');
		return res;
	}

	private Token logicalOpOr() {
		Token res = doWhile("OR", t -> {
			t.combineForce(clauseOrLogicalOp());
			if (matchConsume(',')) {
				t.combineForce(clauseOrLogicalOp());
			}
			return clashConsume(')');
		});
		expectConsume(')');
		return res;
	}

	public Token parse() {
		if (ast != null)
			return ast;
		ast = doWhile("AND", t -> {
			t.combineForce(clauseOrLogicalOp());
			return matchConsume(',');
		});
		expect(EOL);
		System.out.println(c());
		return ast;
	}

	public static void main(String[] args) throws Exception {
		String input;
		input = "x.eq.e";
//		input = "and(x.eq.3,y.bbi.(1,2,3,4,5),or(z.neq.null,abc.in.(ciao,ha\\,llo),t.ire..*77|e3))";
//		input = "xy.in.(1,2)";
//		input = "a.eq.0,b.neq.3,or(a.eq.3,b.eq.5)";
//		input = "a.eq.0,b.neq.3,or(a.eq.3,b.eq.5),a.bbi.(1,2,3,4),d.eq.,f.in.()";
//		input = "f.eq.(null,null,null)";
		input = "f.eq.";//,or(a.eq.7,and(b.eq.9))";
		input = "a.eq.1.and(a.eq.0)";
		WhereClauseParser we = new WhereClauseParser(input);
		Token ast = we.parse();
		System.out.println(ast.prettyFormat());
		System.out.println(ast.format());

	}
}
