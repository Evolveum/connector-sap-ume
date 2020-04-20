package com.evolveum.polygon.connector.sap.ume.operation;

import com.evolveum.polygon.connector.sap.ume.SapUMEConfiguration;
import com.evolveum.polygon.connector.sap.ume.SapUMEConnection;
import com.evolveum.polygon.connector.sap.ume.SapUMESchema;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

public abstract class SapUMEAbstractOperation {

    public static final String LOG_OPERATION_CREATE = "CREATE";
    public static final String LOG_OPERATION_DELETE = "DELETE";
    public static final String LOG_OPERATION_QUERY = "QUERY";
    public static final String LOG_OPERATION_UPDATE = "UPDATE";
    public static final String LOG_OPERATION_TEST = "TEST";

    private SapUMEConfiguration configuration;
    private SapUMEConnection connection;

    private static final Log LOG = Log.getLog(SapUMEAbstractOperation.class);

    public SapUMEAbstractOperation(SapUMEConfiguration configuration, SapUMEConnection connection) {
        this.configuration = configuration;
        this.connection = connection;
    }

    SapUMEConfiguration getConfiguration() {
        return this.configuration;
    }

    SapUMEConnection getConnection() {
        return this.connection;
    }

    public void dispose() {
        LOG.info("Dispose start");
        this.configuration = null;
        this.connection = null;
        LOG.info("Dispose finished");
    }

    boolean isRoleAttribute(String name) {
        return (name.equals(SapUMESchema.ATTRIBUTE_ASSIGNEDROLES) || name.equals(SapUMESchema.ATTRIBUTE_ALLASSIGNEDROLES));
    }

    boolean isGroupAttribute(String name) {
        return (name.equals(SapUMESchema.ATTRIBUTE_ASSIGNEDGROUPS) || name.equals(SapUMESchema.ATTRIBUTE_ALLASSIGNEDGROUPS));
    }

    boolean isDateAttribute(String name) {
        return (name.equals(SapUMESchema.ATTRIBUTE_VALIDFROM) || name.equals(SapUMESchema.ATTRIBUTE_VALIDTO) || name.equals(OperationalAttributes.ENABLE_DATE_NAME) || name.equals(OperationalAttributes.DISABLE_DATE_NAME) || name.equals(SapUMESchema.ATTRIBUTE_LASTMODIFYDATE));
    }
}
