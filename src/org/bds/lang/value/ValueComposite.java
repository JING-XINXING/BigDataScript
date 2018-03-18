package org.bds.lang.value;

import org.bds.lang.type.Type;

public abstract class ValueComposite extends Value {

	protected Type type;

	public ValueComposite(Type type) {
		super();
		this.type = type;
	}

	/**
	 * Convert to 'bool'
	 */
	@Override
	public boolean asBool() {
		throw new RuntimeException("Cannot convert type '" + getType() + "' to bool");
	}

	/**
	 * Convert to 'int'
	 */
	@Override
	public long asInt() {
		throw new RuntimeException("Cannot convert type '" + getType() + "' to int");
	}

	/**
	 * Convert to 'real'
	 */
	@Override
	public double asReal() {
		throw new RuntimeException("Cannot convert type '" + getType() + "' to real");
	}

	/**
	 * Convert to 'string'
	 */
	@Override
	public String asString() {
		return get().toString();
	}

	@Override
	public Type getType() {
		return type;
	}

}