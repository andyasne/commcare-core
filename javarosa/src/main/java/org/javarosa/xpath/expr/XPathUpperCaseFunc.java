package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathUpperCaseFunc extends XPathFuncExpr {
    public static final String NAME = "upper-case";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathUpperCaseFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathUpperCaseFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.normalizeCase(evaluatedArgs[0], true);
    }

    @Override
    public String getDocumentation() {
        return "";
    }
}
