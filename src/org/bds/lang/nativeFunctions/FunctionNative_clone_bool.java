package org.bds.lang.nativeFunctions;

import org.bds.lang.Parameters;
import org.bds.lang.type.Type;
import org.bds.lang.type.Types;
import org.bds.run.BdsThread;

public class FunctionNative_clone_bool extends FunctionNative {
	public FunctionNative_clone_bool() {
		super();
	}

	@Override
	protected void initFunction() {
		functionName = "clone";
		returnType = Types.BOOL;

		String argNames[] = { "x" };
		Type argTypes[] = { Types.BOOL };
		parameters = Parameters.get(argTypes, argNames);
		addNativeFunctionToScope();
	}

	@Override
	protected Object runFunctionNative(BdsThread bdsThread) {
		return bdsThread.getBool("x");
	}
}
