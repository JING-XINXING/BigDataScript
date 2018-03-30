package org.bds.lang.expression;

import org.antlr.v4.runtime.tree.ParseTree;
import org.bds.compile.CompilerMessages;
import org.bds.lang.BdsNode;
import org.bds.run.BdsThread;
import org.bds.symbol.SymbolTable;

/**
 * A division
 *
 * @author pcingola
 */
public class ExpressionDivide extends ExpressionMath {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public ExpressionDivide(BdsNode parent, ParseTree tree) {
		super(parent, tree);
	}

	@Override
	protected String op() {
		return "/";
	}

	/**
	 * Evaluate an expression
	 */
	@Override
	public void runStep(BdsThread bdsThread) {
		bdsThread.run(left);
		bdsThread.run(right);
		if (bdsThread.isCheckpointRecover()) return;

		if (isInt()) {
			long r = bdsThread.popInt();
			long l = bdsThread.popInt();
			bdsThread.push(l / r);
			return;
		} else if (isReal()) {
			double r = bdsThread.popReal();
			double l = bdsThread.popReal();
			bdsThread.push(l / r);
			return;
		}

		throw new RuntimeException("Unknown return type " + returnType + " for expression " + getClass().getSimpleName());
	}

	@Override
	public String toAsm() {
		String eb = super.toAsm();

		String op = "div";
		String type = "";

		if (isInt()) type = "i";
		else if (isReal()) type = "f";
		else throw new RuntimeException("Unknown type. This should never happen!");

		return eb + op + type + "\n";
	}

	@Override
	public void typeCheckNotNull(SymbolTable symtab, CompilerMessages compilerMessages) {
		left.checkCanCastToNumeric(compilerMessages);
		right.checkCanCastToNumeric(compilerMessages);
	}

}
