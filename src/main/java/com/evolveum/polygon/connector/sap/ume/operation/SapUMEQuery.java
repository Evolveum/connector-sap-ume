package com.evolveum.polygon.connector.sap.ume.operation;

import com.evolveum.polygon.connector.sap.ume.*;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.Attribute;
import org.openspml.message.*;
import org.openspml.message.SearchResult;

import java.util.*;

import static org.identityconnectors.common.StringUtil.isBlank;

public class SapUMEQuery extends SapUMEAbstractOperation {

    private static final Log LOG = Log.getLog(SapUMEQuery.class);

    public SapUMEQuery(SapUMEConfiguration configuration, SapUMEConnection connection) {
        super(configuration, connection);
    }

    public void executeQuery(SapUMEObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("Execute query start : {0}", objectClass.toString());
        String searchBase = objectClass.getBase();
        String fullSearchName = null;
        ArrayList<String> attrsToGet = new ArrayList();
        if (objectClass.isAccount()) {
            attrsToGet.addAll(SapUMESchema.getAccountAttributeNames(getConfiguration()));

            attrsToGet.add(SapUMESchema.ATTRIBUTE_LOGONNAME);
            attrsToGet.add(SapUMESchema.ATTRIBUTE_ISLOCKED);
            attrsToGet.add(SapUMESchema.ATTRIBUTE_VALIDFROM);
            attrsToGet.add(SapUMESchema.ATTRIBUTE_VALIDTO);

            attrsToGet.remove(OperationalAttributes.LOCK_OUT_NAME);
            attrsToGet.remove(OperationalAttributes.ENABLE_NAME);
            attrsToGet.remove(OperationalAttributes.PASSWORD_NAME);
            attrsToGet.remove(OperationalAttributes.ENABLE_DATE_NAME);
            attrsToGet.remove(OperationalAttributes.DISABLE_DATE_NAME);
            attrsToGet.remove(Name.NAME);

            fullSearchName = SapUMESchema.ATTRIBUTE_LOGONNAME;
        } else if (objectClass.isRole()) {
            attrsToGet.addAll(SapUMESchema.getRoleAttributeNames(getConfiguration()));
            attrsToGet.remove(Name.NAME);
            fullSearchName = SapUMESchema.ATTRIBUTE_UNIQUENAME;
        } else if (objectClass.isGroup()) {
            attrsToGet.addAll(SapUMESchema.getGroupAttributeNames(getConfiguration()));
            attrsToGet.remove(Name.NAME);
            fullSearchName = SapUMESchema.ATTRIBUTE_UNIQUENAME;
        }

        SearchRequest searchReq = new SearchRequest();
        searchReq.setSearchBase(searchBase);

        //find all
        if (query == null) {
            query = new Filter();
            String is = getConfiguration().getFullSearchStringPattern();
            FilterTerm fin = new FilterTerm();
            fin.setOperation(FilterTerm.OP_OR);
            int charCount = is.length();
            for (int i = 0; i < charCount; i++) {
                FilterTerm sub = new FilterTerm();
                sub.setOperation(FilterTerm.OP_SUBSTRINGS);
                sub.setInitialSubstring(is.substring(i, i + 1));
                sub.setName(fullSearchName);
                fin.addOperand(sub);
            }
            query.addTerm(fin);
        } else {
            List<FilterTerm> terms = query.getTerms();
            for (FilterTerm term : terms) {
                if(term.getName().equals(Name.NAME)&&objectClass.isAccount()) {
                    terms.remove(term);
                    term.setName(SapUMESchema.ATTRIBUTE_LOGONNAME);
                    terms.add(term);
                }
            }
        }

        searchReq.setFilter(query);
        for (String sAttribute : attrsToGet) {
            searchReq.addAttribute(sAttribute);
        }

        SpmlResponse spmlResponse = super.getConnection().connect(searchReq, LOG_OPERATION_QUERY);

        SearchResponse resp = (SearchResponse) spmlResponse;
        List results = resp.getResults();
        if (results != null) {
            for (Object res : results) {
                org.openspml.message.SearchResult searchResult = (org.openspml.message.SearchResult) res;
                ConnectorObject co = null;
                if (objectClass.isAccount()) {
                    co = createConnectorObjectUser(searchResult, attrsToGet);
                } else if (objectClass.isRole()) {
                    co = createConnectorObjectRoleAndGroup(searchResult, attrsToGet);
                } else if (objectClass.isGroup()) {
                    co = createConnectorObjectRoleAndGroup(searchResult, attrsToGet);
                }
                handler.handle(co);
            }
        }
        spmlResponse = null;
        resp = null;
        searchReq = null;
        LOG.info("Execute query finished : {0}", objectClass.toString());
    }

    private ConnectorObject createConnectorObjectUser(SearchResult searchResult, Collection<String> attrsToGet) {
        ConnectorObjectBuilder objectBuilder = new ConnectorObjectBuilder();
        Iterator<String> itr = attrsToGet.iterator();

        while (itr.hasNext()) {
            String attributeName = itr.next();

            if (isRoleAttribute(attributeName) || isGroupAttribute(attributeName) || attributeName.equals(SapUMESchema.ATTRIBUTE_CERTIFICATE)) {
                AttributeBuilder abuilder = new AttributeBuilder();
                abuilder.setName(attributeName);
                if (searchResult.getAttributeValue(attributeName) != null) {
                    Object value = searchResult.getAttributeValue(attributeName);

                    if ((value instanceof ArrayList)) {
                        ArrayList multiList = (ArrayList) value;
                        for (Object values : multiList) {
                            String attrValue = (String) values;
                            abuilder.addValue(new Object[]{attrValue});
                        }
                    } else {
                        String attrValue = (String) value;
                        abuilder.addValue(new Object[]{attrValue});
                    }
                } else {
                    ArrayList alMultiValues = new ArrayList();
                    abuilder.addValue(alMultiValues);
                }
                Attribute multiAttrs = abuilder.build();
                objectBuilder.addAttribute(new Attribute[]{multiAttrs});
            } else if (isDateAttribute(attributeName)) {
                AttributeBuilder abuilder = new AttributeBuilder();
                String value = (String) searchResult.getAttributeValue(attributeName);
                if(attributeName.equals(SapUMESchema.ATTRIBUTE_VALIDFROM)) {
                    abuilder.setName(OperationalAttributes.ENABLE_DATE_NAME);
                } else if(attributeName.equals(SapUMESchema.ATTRIBUTE_VALIDTO)) {
                    abuilder.setName(OperationalAttributes.DISABLE_DATE_NAME);
                } else {
                    abuilder.setName(attributeName);
                }
                if (!isBlank(value)) {
                    try {
                        abuilder.addValue(SapUMEDateHelper.convertDateToLong(SapUMEDateHelper.convertUmeStringToDate(value,getConfiguration())));
                    } catch (Exception e) {
                        LOG.error(e,"Error in build user ume time, value : "+value);
                    }
                }
                objectBuilder.addAttribute(abuilder.build());
            } else if (attributeName.equals(SapUMESchema.ATTRIBUTE_ISLOCKED)) {
                Object value = searchResult.getAttributeValue(attributeName);
                AttributeBuilder abuilder1 = new AttributeBuilder();
                abuilder1.setName(OperationalAttributes.LOCK_OUT_NAME);
                Boolean locked = Boolean.parseBoolean((String)value);
                abuilder1.addValue(locked);
                objectBuilder.addAttribute(abuilder1.build());
//                AttributeBuilder abuilder2 = new AttributeBuilder();
//                abuilder2.setName(attributeName);
//                abuilder2.addValue(value);
//                objectBuilder.addAttribute(abuilder2.build());
            } else {
                AttributeBuilder abuilder = new AttributeBuilder();
                abuilder.setName(attributeName);
                Object value = searchResult.getAttributeValue(attributeName);
                abuilder.addValue(value);

                if (attributeName.equals(SapUMESchema.ATTRIBUTE_ID)) {
                    objectBuilder.setUid((String) value);
                } else if (attributeName.equals(SapUMESchema.ATTRIBUTE_LOGONNAME)) {
                    objectBuilder.setName((String) value);
                }
                if (!attributeName.equals(Name.NAME)&&!attributeName.equals(SapUMESchema.ATTRIBUTE_LOGONNAME)) {
                    objectBuilder.addAttribute(abuilder.build());
                }
            }
        }
        return objectBuilder.build();
    }


    private ConnectorObject createConnectorObjectRoleAndGroup(SearchResult searchResult, ArrayList<String> attrsToGet) {
        ConnectorObjectBuilder objectBuilder = new ConnectorObjectBuilder();
        Iterator<String> itr = attrsToGet.iterator();

        while (itr.hasNext()) {
            String attributeName = itr.next();

            if (isRoleAttribute(attributeName) || (attributeName.equals(SapUMESchema.ATTRIBUTE_MEMBER))) {
                AttributeBuilder abuilder = new AttributeBuilder();
                abuilder.setName(attributeName);
                Object value = searchResult.getAttributeValue(attributeName);

                if (value != null) {
                    if ((value instanceof ArrayList)) {
                        ArrayList multiList = (ArrayList) value;
                        for (Object values : multiList) {
                            String attrValue = (String) values;
                            abuilder.addValue(new Object[]{attrValue});
                        }
                    } else {
                        String attrValue = (String) value;
                        abuilder.addValue(new Object[]{attrValue});
                    }
                } else {
                    abuilder.addValue(new Object[]{value});
                }
                Attribute multiAttrs = abuilder.build();
                objectBuilder.addAttribute(new Attribute[]{multiAttrs});
            } else if (isDateAttribute(attributeName)) {
                AttributeBuilder abuilder = new AttributeBuilder();
                abuilder.setName(attributeName);
                String value = (String) searchResult.getAttributeValue(attributeName);
                if (!isBlank(value)) {
                    try {
                        abuilder.addValue(SapUMEDateHelper.convertDateToLong(SapUMEDateHelper.convertUmeStringToDate(value, getConfiguration())));
                        objectBuilder.addAttribute(abuilder.build());
                    } catch (Exception e) {
                        LOG.error(e,"Error in build group ume time, value : "+value);
                    }
                }
            } else {
                AttributeBuilder abuilder = new AttributeBuilder();
                abuilder.setName(attributeName);
                Object value = searchResult.getAttributeValue(attributeName);

                abuilder.addValue(value);

                if (attributeName.equals(SapUMESchema.ATTRIBUTE_ID)) {
                    if (value == null) { //bug finding group by id
                        value = searchResult.getIdentifier().getId();
                    }
                    objectBuilder.setUid((String) value);
                } else if (attributeName.equals(SapUMESchema.ATTRIBUTE_UNIQUENAME)) {
                    objectBuilder.setName((String) value);
                }

                if (!attributeName.equals(Name.NAME)) {
                    objectBuilder.addAttribute(abuilder.build());
                }
            }
        }
        return objectBuilder.build();
    }
}

