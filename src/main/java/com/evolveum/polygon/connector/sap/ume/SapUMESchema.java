package com.evolveum.polygon.connector.sap.ume;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SapUMESchema {

    private static final Log LOG = Log.getLog(SapUMESchema.class);

    private static Set<String> accountAttributeNames;
    private static Set<String> groupAttributeNames;
    private static Set<String> roleAttributeNames;
    private static Schema schema;

    //--- Schema Description: Sources ----------------------------------------------------------------------------------
    // https://www.immagic.com/eLibrary/ARCHIVES/GENERAL/SAP_DE/S050921X.pdf
    // https://help.sap.com/doc/saphelp_nw70/7.0.31/en-US/e6/d75d3760735b41be930f2dddae3126/content.htm?no_cache=true

    //--- Schema Identifiers -------------------------------------------------------------------------------------------
    public static final String PROVIDER_ID = "SAP";
    public static final String SCHEMA_ID = "SAPprincipals";

    //--- Schema Object Classes ----------------------------------------------------------------------------------------
    //SAP system user object
    public static final String OBJECT_SUPUSER = "sapuser";

    //SAP system role object
    public static final String OBJECT_SAPROLE = "saprole";

    //SAP system group object
    public static final String OBJECT_SAPGROUP = "sapgroup";

    //--- Attributes Used by Object Classes ----------------------------------------------------------------------------

    public static final String ATTRIBUTE_LOGONNAME = "logonname"; // Unique name and logonid
    public static final String ATTRIBUTE_FIRSTNAME = "firstname"; // First name
    public static final String ATTRIBUTE_LASTNAME = "lastname"; // Last name
    public static final String ATTRIBUTE_SALUTATION = "salutation"; // Salutation
    public static final String ATTRIBUTE_TITLE = "title"; // Title
    public static final String ATTRIBUTE_JOBTITLE = "jobtitle"; // Title of the job
    public static final String ATTRIBUTE_MOBILE = "mobile"; // Mobile number
    public static final String ATTRIBUTE_TELEPHONE = "telephone"; // Complete telephone number
    public static final String ATTRIBUTE_DISPLAYNAME = "displayname"; // Display name
    public static final String ATTRIBUTE_DESCRIPTION = "description"; // Human readable description
    public static final String ATTRIBUTE_PASSWORD = "password"; // Logon password
    public static final String ATTRIBUTE_OLDPASSWORD = "oldpassword"; // Logon password
    public static final String ATTRIBUTE_EMAIL = "email"; // Email address
    public static final String ATTRIBUTE_FAX = "fax"; // Complete fax number
    public static final String ATTRIBUTE_LOCALE = "locale"; // Locale code
    public static final String ATTRIBUTE_TIMEZONE = "timezone"; // Timezone

    public static final String ATTRIBUTE_VALIDFROM = "validfrom"; // Date the user gets valid
    public static final String ATTRIBUTE_VALIDTO = "validto"; // Date the user gets invalid

    public static final String ATTRIBUTE_CERTIFICATE = "certificate"; // User certificate (base 64 encoding)
    public static final String ATTRIBUTE_LASTMODIFYDATE = "lastmodifydate"; // Date of last change
    public static final String ATTRIBUTE_ISLOCKED = "islocked"; // Is user locked" type=boolean
    public static final String ATTRIBUTE_ISPASSWORDDISABLED = "ispassworddisabled"; // Is password disabled" type=boolean
    public static final String ATTRIBUTE_UNIQUENAME = "uniquename"; //Unique identifier
    public static final String ATTRIBUTE_MEMBER = "member"; // Assigned members" multivalued="true
    public static final String ATTRIBUTE_DEPARTMENT = "department"; // Department code
    public static final String ATTRIBUTE_ID = "id"; // Backend id
    public static final String ATTRIBUTE_ISSERVICEUSER = "isserviceuser"; // Specifies if object is a technical user
    public static final String ATTRIBUTE_SECURITYPOLICY = "securitypolicy"; // Specifies the type of the user (default,technical,unknown)
    public static final String ATTRIBUTE_DATASOURCE = "datasource"; // Specifies the home data source of the object, readonly
    public static final String ATTRIBUTE_ASSIGNEDROLES = "assignedroles"; // List of all directly assigned roles
    public static final String ATTRIBUTE_ALLASSIGNEDROLES = "allassignedroles"; // List of all assigned roles, readonly
    public static final String ATTRIBUTE_ASSIGNEDGROUPS = "assignedgroups"; // List of all directly assigned groups
    public static final String ATTRIBUTE_ALLASSIGNEDGROUPS = "allassignedgroups"; // List of all assigned groups, readonly
    public static final String ATTRIBUTE_DISTINGUISHEDNAME = "distinguishedname"; // Returns the LDAP distinguished name if the object is stored on an LDAP server
    public static final String ATTRIBUTE_COMPANY = "company"; // Name of the assigned company
    public static final String ATTRIBUTE_STREETADDRESS = "streetaddress"; // Home address of the user
    public static final String ATTRIBUTE_CITY = "city"; // Name of the city
    public static final String ATTRIBUTE_ZIP = "zip"; // Postal code of the city
    public static final String ATTRIBUTE_POBOX = "pobox"; // PO box
    public static final String ATTRIBUTE_COUNTRY = "country"; // Contry code following ISO code 3166
    public static final String ATTRIBUTE_STATE = "state"; // Name of a state
    public static final String ATTRIBUTE_ORGUNIT = "orgunit"; // Name of an organization
    public static final String ATTRIBUTE_ACCESSIBILITYLEVEL = "accessibilitylevel"; // Accessibility level of the user
    public static final String ATTRIBUTE_PASSWORDCHANGEREQUIRED = "passwordchangerequired"; // Specifies if the provided password is a productive one, can only be set to true if a secure transport layer is used

    //------------------------------------------------------------------------------------------------------------------

    public static Set<String> getAccountAttributeNames(SapUMEConfiguration configuration) {
        createSchema(configuration);
        return accountAttributeNames;
    }

    public static Set<String> getGroupAttributeNames(SapUMEConfiguration configuration) {
        createSchema(configuration);
        return groupAttributeNames;
    }

    public static Set<String> getRoleAttributeNames(SapUMEConfiguration configuration) {
        createSchema(configuration);
        return roleAttributeNames;
    }

    public static Schema getSchema(SapUMEConfiguration configuration) {
        createSchema(configuration);
        return schema;
    }

    private static AttributeInfo createSimpleAttribute(String name) {
        AttributeInfoBuilder aib = new AttributeInfoBuilder();
        aib.setName(name);
        aib.setType(String.class);
        return aib.build();
    }

    private static void createSchema(SapUMEConfiguration configuration) {

        LOG.info("Start create schema");

        if (schema != null) {
            return;
        }

        SchemaBuilder schemaBuilder = new SchemaBuilder(SapUMEConnector.class);

        ObjectClassInfoBuilder generalClassBuilder = new ObjectClassInfoBuilder();

        AttributeInfoBuilder uidAib = new AttributeInfoBuilder(ATTRIBUTE_ID);
        uidAib.setType(String.class);
        uidAib.setRequired(false); // Must be optional. It is not present for create operations
        uidAib.setCreateable(false);
        uidAib.setUpdateable(false);
        uidAib.setReadable(true);
        generalClassBuilder.addAttributeInfo(uidAib.build());

        AttributeInfoBuilder uname = new AttributeInfoBuilder(ATTRIBUTE_UNIQUENAME);
        uname.setType(String.class);
        uname.setRequired(true);
        uname.setUpdateable(false);
        generalClassBuilder.addAttributeInfo(uname.build());

        AttributeInfoBuilder datasource = new AttributeInfoBuilder(ATTRIBUTE_DATASOURCE);
        datasource.setType(String.class);
        datasource.setUpdateable(false); //readonly
        generalClassBuilder.addAttributeInfo(datasource.build());

        generalClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_DISPLAYNAME));
        generalClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_DESCRIPTION));
        generalClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_LASTMODIFYDATE));

        ObjectClassInfoBuilder accountClassBuilder = new ObjectClassInfoBuilder();
        accountClassBuilder.setType(ObjectClass.ACCOUNT_NAME);
        accountClassBuilder.addAllAttributeInfo(generalClassBuilder.build().getAttributeInfo());

        AttributeInfoBuilder allRoles = new AttributeInfoBuilder(ATTRIBUTE_ALLASSIGNEDROLES);
        allRoles.setType(String.class);
        allRoles.setUpdateable(false); //readonly
        allRoles.setMultiValued(true);
        accountClassBuilder.addAttributeInfo(allRoles.build());

        AttributeInfoBuilder allGroups = new AttributeInfoBuilder(ATTRIBUTE_ALLASSIGNEDGROUPS);
        allGroups.setType(String.class);
        allGroups.setUpdateable(false); //readonly
        allGroups.setMultiValued(true);
        accountClassBuilder.addAttributeInfo(allGroups.build());

        accountClassBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        accountClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        accountClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE_DATE);
        accountClassBuilder.addAttributeInfo(OperationalAttributeInfos.DISABLE_DATE);
        accountClassBuilder.addAttributeInfo(OperationalAttributeInfos.LOCK_OUT);

        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_ISSERVICEUSER));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_FIRSTNAME));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_LASTNAME));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_SALUTATION));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_TITLE));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_JOBTITLE));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_MOBILE));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_PASSWORD));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_OLDPASSWORD));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_EMAIL));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_FAX));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_LOCALE));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_TIMEZONE));

        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_ISPASSWORDDISABLED));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_TELEPHONE));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_DEPARTMENT));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_SECURITYPOLICY));

        //accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_CERTIFICATE));
        AttributeInfoBuilder certs = new AttributeInfoBuilder(ATTRIBUTE_CERTIFICATE);
        certs.setType(String.class);
        certs.setUpdateable(true);
        certs.setMultiValued(true);
        accountClassBuilder.addAttributeInfo(certs.build());

        AttributeInfoBuilder roles = new AttributeInfoBuilder(ATTRIBUTE_ASSIGNEDROLES);
        roles.setType(String.class);
        roles.setUpdateable(true);
        roles.setMultiValued(true);
        accountClassBuilder.addAttributeInfo(roles.build());

        AttributeInfoBuilder groups = new AttributeInfoBuilder(ATTRIBUTE_ASSIGNEDGROUPS);
        groups.setType(String.class);
        groups.setUpdateable(false); //readonly
        groups.setMultiValued(true);
        accountClassBuilder.addAttributeInfo(groups.build());

        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_COMPANY));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_STREETADDRESS));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_CITY));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_ZIP));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_POBOX));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_COUNTRY));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_STATE));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_ORGUNIT));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_ACCESSIBILITYLEVEL));
        accountClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_PASSWORDCHANGEREQUIRED));

        String[] umeAddAttrs = configuration.getUmeAddAttrs();
        if (umeAddAttrs != null) {
            for (String umeAddAttr : umeAddAttrs) {
                accountClassBuilder.addAttributeInfo(createSimpleAttribute(umeAddAttr));
            }
        }

        ObjectClassInfoBuilder roleClassBuilder = new ObjectClassInfoBuilder();
        roleClassBuilder.setType(SapUMEObjectClass.ROLE_NAME);
        roleClassBuilder.addAllAttributeInfo(generalClassBuilder.build().getAttributeInfo());

        AttributeInfoBuilder member = new AttributeInfoBuilder(ATTRIBUTE_MEMBER);
        member.setType(String.class);
        member.setUpdateable(false); //readonly
        member.setMultiValued(true);
        roleClassBuilder.addAttributeInfo(member.build());

        ObjectClassInfoBuilder groupClassBuilder = new ObjectClassInfoBuilder();
        groupClassBuilder.setType(ObjectClass.GROUP_NAME);
        groupClassBuilder.addAllAttributeInfo(generalClassBuilder.build().getAttributeInfo());

        groupClassBuilder.addAttributeInfo(member.build());
        groupClassBuilder.addAttributeInfo(allRoles.build());
        groupClassBuilder.addAttributeInfo(roles.build());

        groupClassBuilder.addAttributeInfo(createSimpleAttribute(ATTRIBUTE_DISTINGUISHEDNAME));

        ObjectClassInfo role = roleClassBuilder.build();
        ObjectClassInfo group = groupClassBuilder.build();
        ObjectClassInfo account = accountClassBuilder.build();

        schemaBuilder.defineObjectClass(ObjectClass.ACCOUNT_NAME, account.getAttributeInfo());
        schemaBuilder.defineObjectClass(SapUMEObjectClass.ROLE_NAME, role.getAttributeInfo());
        schemaBuilder.defineObjectClass(ObjectClass.GROUP_NAME, group.getAttributeInfo());
        roleAttributeNames = createAtributeNames(role);
        groupAttributeNames = createAtributeNames(group);
        accountAttributeNames = createAtributeNames(account);
        schema = schemaBuilder.build();

        LOG.info("Schema created {0}", schema);
    }

    private static Set createAtributeNames(ObjectClassInfo oci) {
        Set result = new HashSet();
        Iterator<AttributeInfo> iterator = oci.getAttributeInfo().iterator();

        while (iterator.hasNext()) {
            AttributeInfo a = iterator.next();
            result.add(a.getName());
        }
        return result;
    }
}
