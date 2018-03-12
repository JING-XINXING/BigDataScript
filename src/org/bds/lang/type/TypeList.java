package org.bds.lang.type;

import java.util.List;

import org.antlr.v4.runtime.tree.ParseTree;
import org.bds.lang.BdsNode;
import org.bds.lang.nativeMethods.MethodNative;
import org.bds.lang.nativeMethods.list.MethodNativeListAdd;
import org.bds.lang.nativeMethods.list.MethodNativeListAddIndex;
import org.bds.lang.nativeMethods.list.MethodNativeListAddList;
import org.bds.lang.nativeMethods.list.MethodNativeListClone;
import org.bds.lang.nativeMethods.list.MethodNativeListCount;
import org.bds.lang.nativeMethods.list.MethodNativeListFilter;
import org.bds.lang.nativeMethods.list.MethodNativeListForEach;
import org.bds.lang.nativeMethods.list.MethodNativeListHas;
import org.bds.lang.nativeMethods.list.MethodNativeListHead;
import org.bds.lang.nativeMethods.list.MethodNativeListIndexOf;
import org.bds.lang.nativeMethods.list.MethodNativeListIsEmpty;
import org.bds.lang.nativeMethods.list.MethodNativeListJoin;
import org.bds.lang.nativeMethods.list.MethodNativeListJoinStr;
import org.bds.lang.nativeMethods.list.MethodNativeListMap;
import org.bds.lang.nativeMethods.list.MethodNativeListPop;
import org.bds.lang.nativeMethods.list.MethodNativeListPush;
import org.bds.lang.nativeMethods.list.MethodNativeListRemove;
import org.bds.lang.nativeMethods.list.MethodNativeListRemoveIdx;
import org.bds.lang.nativeMethods.list.MethodNativeListReverse;
import org.bds.lang.nativeMethods.list.MethodNativeListRm;
import org.bds.lang.nativeMethods.list.MethodNativeListRmOnExit;
import org.bds.lang.nativeMethods.list.MethodNativeListSize;
import org.bds.lang.nativeMethods.list.MethodNativeListSort;
import org.bds.lang.nativeMethods.list.MethodNativeListTail;
import org.bds.lang.value.Value;
import org.bds.lang.value.ValueList;
import org.bds.util.Gpr;

/**
 * A list/array/stack type
 *
 * @author pcingola
 */
public class TypeList extends TypeComposite {

	public static boolean debug = false;

	public static TypeList LIST_ANY = new TypeList(Types.ANY);

	Type elementType; // Type of elements in the list

	public static TypeList factory(BdsNode parent, ParseTree tree) {
		TypeList typeList = new TypeList(parent, tree);
		return get(typeList.getElementType());
	}

	/**
	 * Get a (cached) list type
	 */
	public static TypeList get(Type elementType) {
		// Get type from hash
		String key = typeKey(elementType);
		TypeList type = (TypeList) Types.get(key);

		// No type cached? Create & add
		if (type == null) {
			type = new TypeList(elementType);
			Types.put(type);
			type.addNativeMethods();
		}

		return type;
	}

	public static String typeKey(Type elementType) {
		return elementType + "[]";
	}

	private TypeList(BdsNode parent, ParseTree tree) {
		super(parent, tree);
		primitiveType = PrimitiveType.LIST;
	}

	private TypeList(Type baseType) {
		super(PrimitiveType.LIST);
		elementType = baseType;
	}

	/**
	 * Can this type be casted to 'type'?
	 */
	@Override
	public boolean canCastTo(Type type) {
		return equals(type) // Same type
				|| type.isBool() // Cast to boolean, same as: "! list.isEmpty()"
				|| getElementType().isVoid() // Empty list can be converted in assignment. e.g.: " list := [] "
		;
	}

	@Override
	public Value cast(Value v) {
		if (is(v.getType())) return v; // Same type? No need to cast
		if (v.getType().isList() && ((TypeList) v.getType()).getElementType().isVoid()) return newDefaultValue(); // Empty list?
		throw new RuntimeException("Cannot cast type '" + v.getType() + "' to type '" + this + "'");
	}

	@Override
	public int compareTo(Type type) {
		// Compare type
		int cmp = super.compareTo(type);
		if (cmp != 0) return cmp;

		// Compare element type
		TypeList ltype = (TypeList) type;
		return elementType.compareTo(ltype.elementType);
	}

	/**
	 * Declare all native methods for this class
	 */
	@Override
	protected List<MethodNative> declateNativeMethods() {
		List<MethodNative> methods = super.declateNativeMethods();
		try {
			methods.add(new MethodNativeListAdd(this));
			methods.add(new MethodNativeListAddIndex(this));
			methods.add(new MethodNativeListAddList(this));
			methods.add(new MethodNativeListClone(this));
			methods.add(new MethodNativeListCount(this));
			methods.add(new MethodNativeListFilter(this));
			methods.add(new MethodNativeListForEach(this));
			methods.add(new MethodNativeListHas(this));
			methods.add(new MethodNativeListHead(this));
			methods.add(new MethodNativeListIndexOf(this));
			methods.add(new MethodNativeListIsEmpty(this));
			methods.add(new MethodNativeListJoin(this));
			methods.add(new MethodNativeListJoinStr(this));
			methods.add(new MethodNativeListMap(this));
			//!!! TODO: UNCOMENT
			//			methods.add(new MethodNativeListMapToInt(this));
			//			methods.add(new MethodNativeListMapToReal(this));
			//			methods.add(new MethodNativeListMapToString(this));
			methods.add(new MethodNativeListPop(this));
			methods.add(new MethodNativeListPush(this));
			methods.add(new MethodNativeListSize(this));
			methods.add(new MethodNativeListSort(this));
			methods.add(new MethodNativeListRemove(this));
			methods.add(new MethodNativeListRemoveIdx(this));
			methods.add(new MethodNativeListReverse(this));
			methods.add(new MethodNativeListRmOnExit(this));
			methods.add(new MethodNativeListRm(this));
			methods.add(new MethodNativeListTail(this));

			// Show
			if (debug) {
				Gpr.debug("Type " + this + ", library methods added: ");
				for (MethodNative method : methods)
					Gpr.debug("\t" + method.signature());
			}
		} catch (Throwable t) {
			t.printStackTrace();
			throw new RuntimeException("Error while adding native mehods for class '" + this + "'", t);
		}
		return methods;
	}

	@Override
	public boolean equals(Type type) {
		return type.isList() //
				&& elementType.equals(((TypeList) type).getElementType()) //
		;
	}

	public Type getElementType() {
		return elementType;
	}

	@Override
	public boolean isList() {
		return true;
	}

	@Override
	public boolean isList(Type baseType) {
		// If elementType is void, then the list must be empty.
		// An empty list complies with all types.
		if (elementType.isVoid() || elementType.isAny() || baseType.isAny()) return true;
		return elementType.equals(baseType);
	}

	@Override
	public Value newDefaultValue() {
		return new ValueList(this);
	}

	@Override
	protected void parse(ParseTree tree) {
		// !!! TODO: We are only allowing to build lists of primitive types
		String listTypeName = tree.getChild(0).getChild(0).getText();
		primitiveType = PrimitiveType.LIST;
		elementType = Types.get(listTypeName);
	}

	@Override
	public String toString() {
		return typeKey(elementType);
	}

	@Override
	public String toStringSerializer() {
		return primitiveType + ":" + elementType.toStringSerializer();
	}

}
