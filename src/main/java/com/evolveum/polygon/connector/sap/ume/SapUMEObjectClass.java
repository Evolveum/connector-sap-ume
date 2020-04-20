package com.evolveum.polygon.connector.sap.ume;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassUtil;
import org.identityconnectors.framework.common.objects.Uid;

public class SapUMEObjectClass {

    public static final String ROLE_NAME = ObjectClassUtil.createSpecialName("ROLE");
    private ObjectClass objectClass;
    private String base;

    private static final Log LOG = Log.getLog(SapUMEObjectClass.class);

    public SapUMEObjectClass(ObjectClass objectClass) {

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            base = SapUMESchema.OBJECT_SUPUSER;
        } else if (objectClass.is(ROLE_NAME)) {
            base = SapUMESchema.OBJECT_SAPROLE;
        } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
            base = SapUMESchema.OBJECT_SAPGROUP;
        } else {
            throw new ConnectorException("Unknown type ObjectClass " + objectClass.toString());
        }
        this.objectClass = objectClass;
    }

    public ObjectClass getObjectClass() {
        return this.objectClass;
    }

    public String getBase() {
        return base;
    }

    public boolean isAccount() {
        return objectClass.is(ObjectClass.ACCOUNT_NAME);
    }

    public boolean isRole() {
        return objectClass.is(ROLE_NAME);
    }

    public boolean isGroup() {
        return objectClass.is(ObjectClass.GROUP_NAME);
    }

    public String assignAttribute(String attrName) {
        if (Uid.NAME.equals(attrName)) {
            return SapUMESchema.ATTRIBUTE_ID;
        } else if ((isRole()) || (isGroup())) {
            if (Name.NAME.equals(attrName)) {
                return SapUMESchema.ATTRIBUTE_UNIQUENAME;
            } else {
                return attrName;
            }
        } else if ((isAccount())) {
            if (Name.NAME.equals(attrName)) {
                return SapUMESchema.ATTRIBUTE_LOGONNAME;
            } else {
                return attrName;
            }
        } else {
            return attrName;
        }
    }

    @Override
    public String toString() {
        return base + ":" + this.objectClass.toString();
    }
}
