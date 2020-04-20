package com.evolveum.polygon.connector.sap.ume.operation;

import com.evolveum.polygon.connector.sap.ume.*;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.openspml.message.*;

import java.util.Iterator;
import java.util.Set;

public class SapUMECreate extends SapUMEAbstractOperation {

    private static final Log LOG = Log.getLog(SapUMECreate.class);

    public SapUMECreate(SapUMEConfiguration configuration, SapUMEConnection connection) {
        super(configuration, connection);
    }

    public Uid create(SapUMEObjectClass objectClass, Set<Attribute> createAttributes) {
        LOG.info("Create start : {0}", objectClass.toString());
        if (objectClass.isRole()) {
            throw new ConnectorException("Creation of new roles is not supported from SAP UME");
        }

        String createBase = objectClass.getBase();
        String backUid = null;
        Attribute passwordAttrAbleToChange = null;

        AddRequest addRequest = new AddRequest();
        addRequest.setObjectClass(createBase);

        Object validFromDateName = null;
        Object validToDateName = null;
        Object validFromName = null;
        Object validToName = null;

        Iterator<Attribute> attrsIter = createAttributes.iterator();
        while (attrsIter.hasNext()) {
            Attribute attr = attrsIter.next();
            String attrName = objectClass.assignAttribute(attr.getName());
            if ((attrName.equals(SapUMESchema.ATTRIBUTE_PASSWORD) || (attrName.equals(OperationalAttributes.PASSWORD_NAME)))) {
                if (getConfiguration().getInitialPasswordAfterCreate()) {
                    addRequest.setAttribute(SapUMESchema.ATTRIBUTE_PASSWORD, SecurityUtil.decrypt((GuardedString) attr.getValue().get(0)));
                } else {
                    String dummyPassword = SecurityUtil.decrypt(getConfiguration().getDummyPassword());
                    addRequest.setAttribute(SapUMESchema.ATTRIBUTE_PASSWORD, dummyPassword);
                    passwordAttrAbleToChange = attr;
                }
            } else if (attrName.equals(OperationalAttributes.LOCK_OUT_NAME)) {
                Boolean locked = Boolean.FALSE;
                if(attr.getValue()!=null) {
                    locked = Boolean.valueOf((boolean)attr.getValue().get(0));
                }
                addRequest.setAttribute(SapUMESchema.ATTRIBUTE_ISLOCKED, locked);
            } else if (attrName.equals(SapUMESchema.ATTRIBUTE_ISLOCKED)) {
                //cesspool - not implemented
            } else if (isDateAttribute(attrName)) {
                Object sAttributeValue = SapUMEDateHelper.parseUmeDateValue(attr.getValue().get(0),getConfiguration());
                if (attrName.equals(OperationalAttributes.ENABLE_DATE_NAME)) {
                    validFromDateName = sAttributeValue;
                } else if (attrName.equals(OperationalAttributes.DISABLE_DATE_NAME)) {
                    validToDateName = sAttributeValue;
                } else {
                    addRequest.setAttribute(attrName, sAttributeValue);
                }
            } else if (attrName.equals(OperationalAttributes.ENABLE_NAME)) {
                String from = null;
                String to = null;
                if(attr.getValue()!=null && !((Boolean)attr.getValue().get(0)).booleanValue()) { //disable
                    from = super.getConfiguration().getDisableValidFromTime();
                    to = super.getConfiguration().getDisableValidToTime();
                } else { //enable
                    from = super.getConfiguration().getEnableValidFromTime();
                    to = super.getConfiguration().getEnableValidToTime();
                }
                validFromName = SapUMEDateHelper.parseValidTime(from,getConfiguration());
                validToName = SapUMEDateHelper.parseValidTime(to,getConfiguration());
            } else {
                Object sAttributeValue = null;
                if (attr.getValue() != null) {
                    sAttributeValue = attr.getValue().get(0);
                } else {
                    sAttributeValue = "";
                }
                addRequest.setAttribute(attrName, sAttributeValue.toString());
            }
        }

        if(validFromDateName!=null) {
            addRequest.setAttribute(SapUMESchema.ATTRIBUTE_VALIDFROM, validFromDateName);
        } else if(validFromName!=null) {
            addRequest.setAttribute(SapUMESchema.ATTRIBUTE_VALIDFROM, validFromName);
        }
        if(validToDateName!=null) {
            addRequest.setAttribute(SapUMESchema.ATTRIBUTE_VALIDTO, validToDateName);
        } else if(validToName!=null) {
            addRequest.setAttribute(SapUMESchema.ATTRIBUTE_VALIDTO, validToName);
        }

        if ((addRequest.getAttributes() == null) || (addRequest.getAttributes().size() == 0)) {
            LOG.error("Add Request attributes can't be empty");
            backUid = null;
        } else {
            SpmlResponse spmlResponse = super.getConnection().connect(addRequest, LOG_OPERATION_CREATE);
            backUid = ((AddResponse) spmlResponse).getIdentifierString();

            if (passwordAttrAbleToChange != null && backUid != null) {
                SapUMEUpdate update = new SapUMEUpdate(super.getConfiguration(), super.getConnection());
                update.updatePassword(backUid, passwordAttrAbleToChange);
                update.dispose();
                update = null;

            }
        }
        LOG.info("Create finished : {0}", backUid);
        return new Uid(backUid);
    }
}
