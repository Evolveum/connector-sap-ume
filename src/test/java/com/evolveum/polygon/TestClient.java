package com.evolveum.polygon;

import com.evolveum.polygon.connector.sap.ume.*;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.openspml.message.Filter;
import org.openspml.message.FilterTerm;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.time.ZonedDateTime;
import java.util.*;

/*
 * @since   2020-02-14
 * @author  Frantisek Reznicek
 */

public class TestClient {

    private static final Log LOG = Log.getLog(TestClient.class);
    private SapUMEConnector sapUMEConnector;

    private String testUserID;
    private String testRoleID1;
    private String testRoleID2 = "ROLE.UME_ROLE_PERSISTENCE.un:SPNEGO_READONLY";
    private String testRoleID3 = "ROLE.UME_ROLE_PERSISTENCE.un:NWA_READONLY";
    private String testGroupID;

    private static final String TEST_USER_NAME = "Test.User";
    private static final String TEST_ROLE_NAME = "Guest"; //roles cannot be created and deleted
    private static final String TEST_GROUP_NAME = "Test.Group";
    private static final String[] MINIMAL_ACCOUNT_ATTRIBUTE_LIST = {"id", "uniquename", "datasource", "displayname", "description", "lastmodifydate", "assignedroles", "assignedgroups"};

    private SapUMEConnector getSapUMEConnector() {
        if (sapUMEConnector == null) {
            sapUMEConnector = new SapUMEConnector();
            sapUMEConnector.init(initSapUMEConfiguration());
        }
        return sapUMEConnector;
    }

    private SapUMEConfiguration initSapUMEConfiguration() {
        //setting minimal configuration
        SapUMEConfiguration sapUMEConfiguration = new SapUMEConfiguration();
        sapUMEConfiguration.setURL("http://<host>:<port>/spml/spmlservice");
        sapUMEConfiguration.setUser("<user>");
        sapUMEConfiguration.setPassword(new GuardedString("<password>".toCharArray()));
        //view debug details
        //sapUMEConfiguration.setLogSPMLRequest(Boolean.TRUE);
        //sapUMEConfiguration.setLogSPMLResponse(Boolean.TRUE);
        return sapUMEConfiguration;
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (sapUMEConnector != null) {
            sapUMEConnector.dispose();
        }
    }

    @Test(dependsOnMethods = {"testSchema"})
    public void testConn() {
        SapUMEConnector suc = getSapUMEConnector();
        suc.test();
    }

    @Test
    public void testSchema() throws Exception {
        SapUMEConnector suc = getSapUMEConnector();
        Schema schema = suc.schema();
        LOG.info("generated schema is:\n{0}", schema);
        checkNeededAttributes(schema);
    }

    private void checkNeededAttributes(Schema schema) throws Exception {
        List<String> found = new LinkedList<String>();
        for (ObjectClassInfo objectClassInfo : schema.getObjectClassInfo()) {
            for (AttributeInfo attributeInfo : objectClassInfo.getAttributeInfo()) {
                found.add(attributeInfo.getName());
            }
        }
        List<String> notFound = new LinkedList<String>();
        for (String attribute : MINIMAL_ACCOUNT_ATTRIBUTE_LIST) {
            if (!found.contains(attribute)) {
                notFound.add(attribute);
            }
        }
        if (notFound.size() > 0) {
            throw new Exception("these required atrributes are not in schema: " + notFound);
        }
        LOG.info("Needed attributes are in schema");
    }

    @Test(dependsOnMethods = {"testConn"})
    public void testFindAllUsers() throws Exception {
        SapUMEConnector suc = getSapUMEConnector();
        Filter query = null;
        final int[] count = {0};
        ResultsHandler handler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                count[0]++;
                return true; // continue
            }
        };

        OperationOptions options = null;
        suc.executeQuery(new ObjectClass(ObjectClass.ACCOUNT_NAME), query, handler, options);
        LOG.info("count[0] = " + count[0]);
        Assert.assertTrue(count[0] > 0, "Find all users return zero users");
    }

    @Test(dependsOnMethods = {"testConn"})
    public void testFindAllGroups() throws Exception {
        SapUMEConnector suc = getSapUMEConnector();
        Filter query = null;
        final int[] count = {0};
        ResultsHandler handler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                count[0]++;
                return true; // continue
            }
        };
        OperationOptions options = null;
        suc.executeQuery(new ObjectClass(ObjectClass.GROUP_NAME), query, handler, options);
        LOG.info("count[0] = " + count[0]);
        Assert.assertTrue(count[0] > 0, "Find all groups return zero users");
    }

    @Test(dependsOnMethods = {"testConn"})
    public void testFindAllRoles() throws Exception {
        SapUMEConnector suc = getSapUMEConnector();
        Filter query = null;
        final int[] count = {0};
        ResultsHandler handler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                count[0]++;
                return true; // continue
            }
        };
        OperationOptions options = null;
        suc.executeQuery(new ObjectClass(SapUMEObjectClass.ROLE_NAME), query, handler, options);
        LOG.info("count[0] = " + count[0]);
        Assert.assertTrue(count[0] > 0, "Find all roles return zero users");
    }

    @Test(dependsOnMethods = {"testCreateUser"})
    public void testFindUser() throws Exception {
        Filter query = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_EQUAL);
        filterTerm.setName(Name.NAME);

        filterTerm.setValue(TEST_USER_NAME);
        query.addTerm(filterTerm);

        final boolean[] found = {false};
        ResultsHandler handler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                found[0] = true;
                testUserID = connectorObject.getUid().getUidValue();
                return true; // continue
            }
        };

        OperationOptions options = null;
        SapUMEConnector suc = getSapUMEConnector();
        suc.executeQuery(new ObjectClass(ObjectClass.ACCOUNT_NAME), query, handler, options);
        Assert.assertTrue(found[0], "User " + TEST_USER_NAME + " not found");
    }

    @Test(dependsOnMethods = {"testConn"})
    public void testFindRole() throws Exception {
        Filter query = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_EQUAL);
        filterTerm.setValue(TEST_ROLE_NAME);
        filterTerm.setName(SapUMESchema.ATTRIBUTE_UNIQUENAME);
        query.addTerm(filterTerm);
        final boolean[] found = {false};
        ResultsHandler handler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                found[0] = true;
                testRoleID1 = connectorObject.getUid().getUidValue();
                return true; // continue
            }
        };
        SapUMEConnector suc = getSapUMEConnector();
        suc.executeQuery(new ObjectClass(SapUMEObjectClass.ROLE_NAME), query, handler, null);
        Assert.assertTrue(found[0], "Role " + TEST_ROLE_NAME + " not found");
    }

    @Test(dependsOnMethods = {"testCreateGroup"})
    public void testFindGroup() throws Exception {
        Filter query = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_EQUAL);
        filterTerm.setName(SapUMESchema.ATTRIBUTE_UNIQUENAME);
        filterTerm.setValue(TEST_GROUP_NAME);
        query.addTerm(filterTerm);
        final boolean[] found = {false};
        ResultsHandler handler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                found[0] = true;
                testGroupID = connectorObject.getUid().getUidValue();
                return true; // continue
            }
        };
        SapUMEConnector suc = getSapUMEConnector();
        suc.executeQuery(new ObjectClass(ObjectClass.GROUP_NAME), query, handler, null);
        Assert.assertTrue(found[0], "Group " + TEST_GROUP_NAME + " not found");
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser", "testFindRole", "testFindGroup"})
    public void testUpdateUserFull() throws Exception {
        String firstName = "Ignacius";
        String lastName = "Reilly";
        String salutation = "Mr.";
        String title = "Dr.";
        String jobTitle = "midPoint tester";
        String mobile = "123456789";
        String telephone = "+420987654321";
        String displayName = "Modern Don Quixote (updated)";
        String description = "Eccentric, idealistic, and creative, sometimes to the point of delusion (created)";
        String email = "ignacius@reilly.com";
        String fax = "123456789";
        String locale = "EN";
        String timeZone = SapUMEDateHelper.getTimeZone().getID();
        ZonedDateTime validFrom = SapUMEDateHelper.convertDateToZDT(SapUMEDateHelper.getCurrentTime());
        //ZonedDateTime validTo = SapUMEDateHelper.convertDateToZDT(SapUMEDateHelper.convertUmeStringToDate("20300920093040Z")); //UME format
        Long validTo = Long.parseLong("1735686000000");

        String[] roles = new String[]{testRoleID1,testRoleID2,testRoleID3};
        String[] groups = new String[]{testGroupID};
        String password = "h0tD0gPassword";

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(Uid.NAME, testUserID));
        attributes.add(AttributeBuilder.build(Name.NAME, TEST_USER_NAME)); //here logoname may change
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_FIRSTNAME, firstName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_LASTNAME, lastName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_SALUTATION, salutation));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_TITLE, title));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_JOBTITLE, jobTitle));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_MOBILE, mobile));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_TELEPHONE, telephone));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DISPLAYNAME, displayName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DESCRIPTION, description));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_EMAIL, email));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_FAX, fax));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_LOCALE, locale));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_TIMEZONE, timeZone));
        attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, validFrom));
        attributes.add(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, validTo));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_ASSIGNEDGROUPS, groups));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_ASSIGNEDROLES, roles));
        attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, new GuardedString(password.toCharArray())));

        SapUMEConnector suc = getSapUMEConnector();
        suc.update(new ObjectClass(ObjectClass.ACCOUNT_NAME), new Uid(testUserID), attributes, null);
        simpleVerify(new ObjectClass(ObjectClass.ACCOUNT_NAME), SapUMESchema.ATTRIBUTE_LASTNAME, lastName);
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser","testUpdateUserFull"})
    public void testDisableUser() throws Exception {
        disableUser(true);
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser", "testUpdateUserFull","testDisableUser"})
    public void testEnableUser() throws Exception {
        disableUser(false);
    }

    private void disableUser(boolean disable) throws Exception {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(Uid.NAME, testUserID));
        attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.valueOf(!disable)));
        SapUMEConnector suc = getSapUMEConnector();
        suc.update(new ObjectClass(ObjectClass.ACCOUNT_NAME), new Uid(testUserID), attributes, null);
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser"})
    public void testLockUser() throws Exception {
        lockUser(true);
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser", "testLockUser"})
    public void testUnlockUser() throws Exception {
        lockUser(false);
    }

    private void lockUser(boolean lock) throws Exception {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(Uid.NAME, testUserID));
        attributes.add(AttributeBuilder.build(OperationalAttributes.LOCK_OUT_NAME, Boolean.valueOf(!lock)));
        SapUMEConnector suc = getSapUMEConnector();
        suc.update(new ObjectClass(ObjectClass.ACCOUNT_NAME), new Uid(testUserID), attributes, null);
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser"})
    public void testChangePassword() throws Exception {
        String newPassword = "MyNewPWdIs007";
        Set<Attribute> attributes = new HashSet<Attribute>();
        GuardedString password = new GuardedString(newPassword.toCharArray());
        attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, password));
        SapUMEConnector suc = getSapUMEConnector();
        suc.update(new ObjectClass(ObjectClass.ACCOUNT_NAME), new Uid(testUserID), attributes, null);
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser"})
    public void testUpdateRole() throws Exception {
        String description = "Guest role (updated)"; //please change after test to default value "Guest role"
        //String description = "Guest role";
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(Uid.NAME, testRoleID1));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DESCRIPTION, description));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_MEMBER, new String[]{testUserID}));
        SapUMEConnector suc = getSapUMEConnector();
        suc.update(new ObjectClass(SapUMEObjectClass.ROLE_NAME), new Uid(testRoleID1), attributes, null);
        simpleVerify(new ObjectClass(SapUMEObjectClass.ROLE_NAME), SapUMESchema.ATTRIBUTE_DESCRIPTION, description);
    }

    @Test(dependsOnMethods = {"testFindUser", "testFindRole", "testCreateGroup"})
    public void testUpdateGroup() throws Exception {
        String displayName = "Test group (updated)";
        String description = "Test group (updated)";
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(Uid.NAME, testGroupID));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DISPLAYNAME, displayName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DESCRIPTION, description));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_MEMBER, new String[]{testUserID}));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_ASSIGNEDROLES, new String[]{testRoleID1}));
        SapUMEConnector suc = getSapUMEConnector();
        suc.update(new ObjectClass(ObjectClass.GROUP_NAME), new Uid(testGroupID), attributes, null);
        simpleVerify(new ObjectClass(ObjectClass.GROUP_NAME), SapUMESchema.ATTRIBUTE_DISPLAYNAME, displayName);
    }

    @Test(dependsOnMethods = {"testConn"})
    public void testCreateUser() throws Exception {
        String firstName = "John";
        String lastName = "Toole";
        String salutation = "Mr.";
        String title = "Dr.";
        String jobTitle = "writter";
        String mobile = "987654321";
        String telephone = "+420123456789";
        String displayName = "John Kennedy Toole (created)";
        String description = "A Confederacy of Dunces (created)";
        String email = "john.kenedy@toole.com";
        String fax = "987654321";
        String locale = "EN";

        String timeZone = SapUMEDateHelper.getTimeZone().getID();
        ZonedDateTime validFrom = SapUMEDateHelper.convertDateToZDT(SapUMEDateHelper.getCurrentTime());
        Long validTo = Long.parseLong("1579788000000");

        String newPassword = "Publication1980";

        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(Name.NAME, TEST_USER_NAME));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_FIRSTNAME, firstName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_LASTNAME, lastName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_SALUTATION, salutation));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_TITLE, title));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_JOBTITLE, jobTitle));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_MOBILE, mobile));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_TELEPHONE, telephone));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DISPLAYNAME, displayName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DESCRIPTION, description));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_EMAIL, email));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_FAX, fax));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_LOCALE, locale));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_TIMEZONE, timeZone));
        attributes.add(AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, validFrom));
        attributes.add(AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, validTo));

        GuardedString password = new GuardedString(newPassword.toCharArray());
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_PASSWORD, password));

        SapUMEConnector suc = getSapUMEConnector();
        suc.create(new ObjectClass(ObjectClass.ACCOUNT_NAME), attributes, null);
        simpleVerify(new ObjectClass(ObjectClass.ACCOUNT_NAME), SapUMESchema.ATTRIBUTE_JOBTITLE, jobTitle);
    }

    @Test(dependsOnMethods = {"testConn"})
    public void testCreateGroup() throws Exception {
        String displayName = "Test group (created)";
        String description = "Test group (created)";
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_UNIQUENAME, TEST_GROUP_NAME));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DISPLAYNAME, displayName));
        attributes.add(AttributeBuilder.build(SapUMESchema.ATTRIBUTE_DESCRIPTION, description));
        SapUMEConnector suc = getSapUMEConnector();
        suc.create(new ObjectClass(ObjectClass.GROUP_NAME), attributes, null);
        simpleVerify(new ObjectClass(ObjectClass.GROUP_NAME), SapUMESchema.ATTRIBUTE_DISPLAYNAME, displayName);
    }

    @Test(dependsOnMethods = {"testCreateUser", "testFindUser", "testUpdateUserFull", "testLockUser", "testUnlockUser", "testDisableUser","testEnableUser","testChangePassword"})
    public void testDeleteUser() throws Exception {
        Uid uid = new Uid(testUserID);
        SapUMEConnector suc = getSapUMEConnector();
        suc.delete(new ObjectClass(ObjectClass.ACCOUNT_NAME), uid, null);
    }

    @Test(dependsOnMethods = {"testCreateGroup", "testFindGroup", "testUpdateGroup"})
    public void testDeleteGroup() throws Exception {
        Set<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(AttributeBuilder.build(Uid.NAME, testGroupID));
        Uid uid = new Uid(testGroupID);
        SapUMEConnector suc = getSapUMEConnector();
        suc.delete(new ObjectClass(ObjectClass.GROUP_NAME), uid, null);
    }

    private void simpleVerify(ObjectClass objectClass, String key, String value) throws Exception {
        final String[] found = {null};
        String find = null;
        String name = null;
        if(objectClass.getObjectClassValue().equals(ObjectClass.ACCOUNT_NAME)) {
            find = Name.NAME;
            name = TEST_USER_NAME;
        } else if (objectClass.getObjectClassValue().equals(ObjectClass.GROUP_NAME)) {
            find = SapUMESchema.ATTRIBUTE_UNIQUENAME;
            name = TEST_GROUP_NAME;
        } else {
            find = SapUMESchema.ATTRIBUTE_UNIQUENAME;
            name = TEST_ROLE_NAME;
        }

        Filter query = new Filter();
        FilterTerm filterTerm = new FilterTerm();
        filterTerm.setOperation(FilterTerm.OP_EQUAL);
        filterTerm.setName(find);
        filterTerm.setValue(name);
        query.addTerm(filterTerm);

        ResultsHandler handler = new ResultsHandler() {
            @Override
            public boolean handle(ConnectorObject connectorObject) {
                found[0] = (String) connectorObject.getAttributeByName(key).getValue().get(0);
                return true; // continue
            }
        };
        OperationOptions options = null;
        SapUMEConnector suc = getSapUMEConnector();
        suc.executeQuery(objectClass, query, handler, options);
        Assert.assertEquals(found[0], value, "In the attribute '" + key + "' expected other value - ");
    }
}
