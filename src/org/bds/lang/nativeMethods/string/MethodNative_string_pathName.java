package org.bds.lang.nativeMethods.string;

import org.bds.data.Data;
import org.bds.lang.Parameters;
import org.bds.lang.type.Type;
import org.bds.lang.type.Types;
import org.bds.run.BdsThread;

public class MethodNative_string_pathName extends MethodNativeString {
	public MethodNative_string_pathName() {
		super();
	}

	@Override
	protected void initMethod() {
		functionName = "pathName";
		classType = Types.STRING;
		returnType = Types.STRING;

		String argNames[] = { "this" };
		Type argTypes[] = { Types.STRING };
		parameters = Parameters.get(argTypes, argNames);
		addNativeMethodToClassScope();
	}

	@Override
	protected Object runMethodNative(BdsThread bdsThread, Object objThis) {
		try {
			String parenPath = bdsThread.data(objThis.toString()).getParent();
			Data d = bdsThread.data(parenPath);
			return d.getAbsolutePath();
		} catch (Exception e) {
			return "";
		}
	}
}
