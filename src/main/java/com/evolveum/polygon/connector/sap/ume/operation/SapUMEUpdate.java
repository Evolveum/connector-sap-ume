package com.evolveum.polygon.connector.sap.ume.operation;

import com.evolveum.polygon.connector.sap.ume.*;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.Attribute;
import org.openspml.message.*;
import org.openspml.message.SearchResult;

import java.util.*;

public class SapUMEUpdate extends SapUMEAbstractOperation {

    private static final Log LOG = Log.getLog(SapUMEUpdate.class);

    public SapUMEUpdate(SapUMEConfiguration configuration, SapUMEConnection connection) {
        super(configuration, connection);
    }

    public Uid update(SapUMEObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes) {
        LOG.info("Update start : {0}");
        String updateBase = objectClass.getBase();
        String backUid = null;
        Attribute passwordAttrAbleToChange = null;
        ModifyRequest modifyRequest = new ModifyRequest();
        String uidValue = uid.getUidValue();
        Iterator<Attribute> attrsIter = replaceAttributes.iterator();
        modifyRequest.setIdentifier(uidValue);
        Object validFromDateName = null;
        Object validToDateName = null;
        Object validFromName = null;
        Object validToName = null;
        while (attrsIter.hasNext()) {
            Attribute attr = attrsIter.next();
            String attrName = objectClass.assignAttribute(attr.getName());
            if (isRoleAttribute(attrName)) {
                getListElement(modifyRequest, attr, uidValue, updateBase);
            } else if (isGroupAttribute(attrName) || attrName.equals(SapUMESchema.ATTRIBUTE_MEMBER) || attrName.equals(SapUMESchema.ATTRIBUTE_CERTIFICATE)) {
                getListElement(modifyRequest, attr, uidValue, updateBase);
            } else {
                if ((attrName.equals(SapUMESchema.ATTRIBUTE_PASSWORD) || (attrName.equals(OperationalAttributes.PASSWORD_NAME)))) {
                    if (getConfiguration().getInitialPasswordAfterUpdate()) {
                        modifyRequest.addModification(SapUMESchema.ATTRIBUTE_PASSWORD, SecurityUtil.decrypt((GuardedString) attr.getValue().get(0)));
                    } else {
                        String dummyPassword = SecurityUtil.decrypt(getConfiguration().getDummyPassword());
                        modifyRequest.addModification(SapUMESchema.ATTRIBUTE_PASSWORD, dummyPassword);
                        passwordAttrAbleToChange = attr;
                    }
                } else if (attrName.equals(OperationalAttributes.ENABLE_NAME)) {
                    String from = null;
                    String to = null;
                    if (attr.getValue() != null && !((Boolean) attr.getValue().get(0)).booleanValue()) { //disable
                        from = super.getConfiguration().getDisableValidFromTime();
                        to = super.getConfiguration().getDisableValidToTime();
                    } else { //enable
                        from = super.getConfiguration().getEnableValidFromTime();
                        to = super.getConfiguration().getEnableValidToTime();
                    }
                    validFromName = SapUMEDateHelper.parseValidTime(from, getConfiguration());
                    validToName = SapUMEDateHelper.parseValidTime(to, getConfiguration());
                } else if (attrName.equals(OperationalAttributes.LOCK_OUT_NAME)) {
                    Boolean locked = Boolean.FALSE;
                    if (attr.getValue() != null) {
                        locked = Boolean.valueOf((boolean) attr.getValue().get(0));
                    }
                    modifyRequest.addModification(SapUMESchema.ATTRIBUTE_ISLOCKED, locked);

                } else if (attrName.equals(SapUMESchema.ATTRIBUTE_ISLOCKED)) {
                    //cesspool - not implemented
                } else if (isDateAttribute(attrName)) {
                    if (attr.getValue() != null) {
                        Object sAttributeValue = SapUMEDateHelper.parseUmeDateValue(attr.getValue().get(0), getConfiguration());
                        if (attrName.equals(OperationalAttributes.ENABLE_DATE_NAME)) {
                            validFromDateName = sAttributeValue;
                        } else if (attrName.equals(OperationalAttributes.DISABLE_DATE_NAME)) {
                            validToDateName = sAttributeValue;
                        } else {
                            modifyRequest.addModification(attrName, sAttributeValue);
                        }
                    }
                } else {
                    Object sAttributeValue = attr.getValue() == null || attr.getValue().size() == 0 ? "" : attr.getValue().get(0);
                    modifyRequest.addModification(attrName, sAttributeValue.toString());
                }
            }
        }

        if(validFromDateName!=null) {
            modifyRequest.addModification(SapUMESchema.ATTRIBUTE_VALIDFROM, validFromDateName);
        } else if(validFromName!=null) {
            modifyRequest.addModification(SapUMESchema.ATTRIBUTE_VALIDFROM, validFromName);
        }
        if(validToDateName!=null) {
            modifyRequest.addModification(SapUMESchema.ATTRIBUTE_VALIDTO, validToDateName);
        } else if(validToName!=null) {
            modifyRequest.addModification(SapUMESchema.ATTRIBUTE_VALIDTO, validToName);
        }

        if ((modifyRequest.getModifications() == null) || (modifyRequest.getModifications().size() == 0)) {
            LOG.error("Modify Request attributes can't be empty : ", uid.toString());
            backUid = uidValue;
        } else {
            super.getConnection().connect(modifyRequest, LOG_OPERATION_UPDATE);
            backUid = uidValue;
            if (passwordAttrAbleToChange != null) {
                updatePassword(uidValue, passwordAttrAbleToChange);   //if I don't call this, then the password will be init password
            }
        }
        modifyRequest = null;
        LOG.info("Update finished : {0}", backUid);
        return new Uid(backUid);
    }

    private void getListElement(ModifyRequest modifyRequest, Attribute multiValueAttr, String uidValue, String base) {
        List<String> lstAddRecords = new ArrayList();
        List<String> lstRmRecords = new ArrayList();

        String attrName = multiValueAttr.getName();
        List Newlst = multiValueAttr.getValue();

        List existlst = getAssignedList(uidValue, attrName, base);
        HashMap hmMultiValueAttrs = compare(existlst, Newlst);

        lstAddRecords = (List) hmMultiValueAttrs.get(Modification.OP_ADD);
        lstRmRecords = (List) hmMultiValueAttrs.get(Modification.OP_DELETE);

        if ((lstAddRecords != null) && (lstAddRecords.size() > 0)) {
            for (Object addRecord : lstAddRecords) {
                Modification mod = new Modification(attrName, addRecord);
                mod.setOperation(Modification.OP_ADD);
                modifyRequest.addModification(mod);
            }
        }
        if ((lstRmRecords != null) && (lstRmRecords.size() > 0)) {
            for (Object rmRecord : lstRmRecords) {
                Modification mod = new Modification(attrName, rmRecord);
                mod.setOperation(Modification.OP_DELETE);
                modifyRequest.addModification(mod);
            }
        }
    }

    protected void updatePassword(String uidValue, Attribute passwordAttr) {
        ModifyRequest req = new ModifyRequest();
        String dummyPassword = SecurityUtil.decrypt(super.getConfiguration().getDummyPassword());
        Object attrVal = passwordAttr.getValue().get(0);
        GuardedString gsPassword = (GuardedString) attrVal;
        String newPassword = SecurityUtil.decrypt(gsPassword);
        req.addModification(SapUMESchema.ATTRIBUTE_OLDPASSWORD, dummyPassword);
        req.addModification(SapUMESchema.ATTRIBUTE_PASSWORD, newPassword);
        req.setIdentifier(uidValue);
        super.getConnection().connect(req, LOG_OPERATION_UPDATE);
    }

    private ArrayList<String> getAssignedList(String uidValue, String attrName, String base) {
        ArrayList<String> assignedList = new ArrayList();
        SearchRequest searchReq = new SearchRequest();
        Filter filter = new Filter();
        FilterTerm oSub2FilterTerm = new FilterTerm();
        oSub2FilterTerm.setOperation(FilterTerm.OP_EQUAL);
        oSub2FilterTerm.setName(SapUMESchema.ATTRIBUTE_ID);
        oSub2FilterTerm.setValue(uidValue);

        filter.addTerm(oSub2FilterTerm);
        searchReq.setSearchBase(base);
        if (filter != null) {
            searchReq.setFilter(filter);
        }
        searchReq.addAttribute(attrName);
        SpmlResponse spmlResponse = super.getConnection().connect(searchReq, LOG_OPERATION_UPDATE);
        SearchResponse resp = (SearchResponse) spmlResponse;
        List results = resp.getResults();
        SearchResult searchResult = (SearchResult) results.get(0);
        if (searchResult.getAttribute(attrName) != null) {
            if ((searchResult.getAttributeValue(attrName) instanceof ArrayList)) {
                assignedList = (ArrayList) searchResult.getAttributeValue(attrName);
            } else {
                String s = searchResult.getAttributeValue(attrName).toString();
                assignedList.add(s);
            }
        }
        spmlResponse = null;
        resp = null;
        return assignedList;
    }

    private HashMap compare(List<String> lstExist, List<String> lstNew) {
        List<String> lstAdd = new ArrayList();
        List<String> lstRemove = new ArrayList();
        HashMap hmMultiValueAttr = new HashMap();
        if (lstNew == null) {
            lstNew = new ArrayList();
        }
        if ((lstExist != null) && (!lstExist.isEmpty())) {
            for (String lstElem : lstExist) {
                if (!lstNew.contains(lstElem)) {
                    lstRemove.add(lstElem);
                }
            }
            for (String lstElem : lstNew) {
                if (!lstExist.contains(lstElem)) {
                    lstAdd.add(lstElem);
                }
            }
            if (lstAdd.size() > 0) {
                hmMultiValueAttr.put(Modification.OP_ADD, lstAdd);
            }
            if (lstRemove.size() > 0) {
                hmMultiValueAttr.put(Modification.OP_DELETE, lstRemove);
            }
            if ((lstNew == null) || (lstNew.isEmpty())) {
                hmMultiValueAttr.put(Modification.OP_DELETE, lstRemove);
            }
        } else if ((lstExist.isEmpty()) &&
                (lstNew != null) && (!lstNew.isEmpty())) {
            hmMultiValueAttr.put(Modification.OP_ADD, lstNew);
        }
        return hmMultiValueAttr;
    }
}

