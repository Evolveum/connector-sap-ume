package com.evolveum.polygon.connector.sap.ume;

import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.*;
import org.openspml.message.Filter;
import org.openspml.message.FilterTerm;

public class SapUMEFilterTranslator extends AbstractFilterTranslator<Filter> {

    private static final Log LOG = Log.getLog(SapUMEFilterTranslator.class);
    private final SapUMEObjectClass objectClass;

    public SapUMEFilterTranslator(SapUMEObjectClass objectClass) {
        this.objectClass = objectClass;
    }

    protected Filter createEqualsExpression(EqualsFilter filter, boolean not) {
        Filter returnFilter = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_EQUAL);

        String name = filter.getName();
        if (this.objectClass.isAccount()) {
            if (Uid.NAME.equals(name)) {
                filterTerm.setName(SapUMESchema.ATTRIBUTE_ID);
            } else if (Name.NAME.equals(name)) {
                filterTerm.setName(SapUMESchema.ATTRIBUTE_LOGONNAME);
            } else {
                filterTerm.setName(name);
            }
        } else if ((this.objectClass.isGroup()) && ((Uid.NAME.equals(name)) || (Name.NAME.equals(name)))) {
            filterTerm.setName(SapUMESchema.ATTRIBUTE_ID);
        } else if ((this.objectClass.isRole()) && ((Uid.NAME.equals(name)) || (Name.NAME.equals(name)))) {
            filterTerm.setName(SapUMESchema.ATTRIBUTE_ID);
        } else {
            filterTerm.setName(name);
        }
        filterTerm.setValue(filter.getAttribute().getValue().get(0));
        returnFilter.addTerm(filterTerm);
        return returnFilter;
    }

    protected Filter createContainsExpression(ContainsFilter filter, boolean not) {
        Filter returnFilter = new Filter();
        List<String> substring = new ArrayList();
        substring.add((String) filter.getAttribute().getValue().get(0));
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_SUBSTRINGS);
        filterTerm.setName(filter.getName());
        filterTerm.setSubstrings(substring);
        returnFilter.addTerm(filterTerm);
        return returnFilter;
    }

    protected Filter createStartsWithExpression(StartsWithFilter filter, boolean not) {
        Filter returnFilter = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_SUBSTRINGS);
        filterTerm.setName(filter.getName());
        filterTerm.setInitialSubstring((String) filter.getAttribute().getValue().get(0));
        returnFilter.addTerm(filterTerm);
        return returnFilter;
    }

    protected Filter createEndsWithExpression(EndsWithFilter filter, boolean not) {
        Filter returnFilter = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_SUBSTRINGS);
        filterTerm.setName(filter.getName());
        filterTerm.setFinalSubstring((String) filter.getAttribute().getValue().get(0));
        returnFilter.addTerm(filterTerm);
        return returnFilter;
    }

    protected Filter createContainsAllValuesExpression(ContainsAllValuesFilter filter, boolean not) {
        Filter returnFilter = new Filter();

        List<Object> values = filter.getAttribute().getValue();
        List substring = new ArrayList();
        for (Object value : values) {
            substring.add(value);
        }
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_EQUAL);
        filterTerm.setName(filter.getName());
        filterTerm.setValues(substring);
        returnFilter.addTerm(filterTerm);
        return returnFilter;
    }

    protected Filter createAndExpression(Filter leftExpressionFilter, Filter rightExpressionFilter) {
        return createExpression(leftExpressionFilter, rightExpressionFilter, FilterTerm.OP_AND);
    }

    protected Filter createOrExpression(Filter leftExpressionFilter, Filter rightExpressionFilter) {
        return createExpression(leftExpressionFilter, rightExpressionFilter, FilterTerm.OP_OR);
    }

    private Filter createExpression(Filter leftExpressionFilter, Filter rightExpressionFilter, String operation) {
        Filter filter = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(operation);

        List<FilterTerm> lstFilterTerms = leftExpressionFilter.getTerms();
        if ((lstFilterTerms.get(0)).getOperation().equals(operation)) {
            List<FilterTerm> lstFilter = ((FilterTerm) lstFilterTerms.get(0)).getOperands();
            lstFilter.add((FilterTerm) rightExpressionFilter.getTerms().get(0));
            for (FilterTerm term : lstFilter) {
                filterTerm.addOperand(term);
            }
        } else {
            filterTerm.addOperand((FilterTerm) leftExpressionFilter.getTerms().get(0));
            filterTerm.addOperand((FilterTerm) rightExpressionFilter.getTerms().get(0));
        }
        filter.addTerm(filterTerm);
        return filter;
    }
}
