package ca.mcgill.mcb.pcingola.bigDataScript.lang.nativeMethods.string;

import ca.mcgill.mcb.pcingola.bigDataScript.lang.Parameters;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.Type;
import ca.mcgill.mcb.pcingola.bigDataScript.lang.nativeMethods.MethodNative;
import ca.mcgill.mcb.pcingola.bigDataScript.run.BigDataScriptThread;

public class MethodNative_string_toLower extends MethodNative {
	public MethodNative_string_toLower() {
		super();
	}

	@Override
	protected void initMethod() {
		functionName = "toLower";
		classType = Type.STRING;
		returnType = Type.STRING;

		String argNames[] = { "this" };
		Type argTypes[] = { Type.STRING };
		parameters = Parameters.get(argTypes, argNames);
		addNativeMethodToScope();
	}

	@Override
	protected Object runMethodNative(BigDataScriptThread csThread, Object objThis) {
		return objThis.toString().toLowerCase();
	}
}
