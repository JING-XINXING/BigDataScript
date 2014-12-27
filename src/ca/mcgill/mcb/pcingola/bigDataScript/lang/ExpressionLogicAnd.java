package ca.mcgill.mcb.pcingola.bigDataScript.lang;

import org.antlr.v4.runtime.tree.ParseTree;

import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;

/**
 * Boolean AND
 *
 * @author pcingola
 */
public class ExpressionLogicAnd extends ExpressionLogic {

	public ExpressionLogicAnd(BigDataScriptNode parent, ParseTree tree) {
		super(parent, tree);
	}

	/**
	 * Evaluate an expression
	 */
	@Override
	public void runStep(BigDataScriptThread bdsThread) {
		left.run(bdsThread);
		if (!((Boolean) bdsThread.peek())) return; // Already false? No need to evaluate the other expression

		// 'AND' only depends on 'right' result (left was true)
		bdsThread.pop(); // Remove 'left' result from stack
		right.run(bdsThread);
	}

	@Override
	protected String op() {
		return "&&";
	}

}