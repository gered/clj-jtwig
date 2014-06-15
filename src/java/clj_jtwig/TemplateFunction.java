package clj_jtwig;

import com.lyncode.jtwig.functions.exceptions.FunctionException;

// this is defined in Java only because Clojure defprotocol/definterface don't allow
// including method definitions with vararg parameters

public interface TemplateFunction {
	public abstract Object execute (Object... arguments) throws FunctionException;
}
