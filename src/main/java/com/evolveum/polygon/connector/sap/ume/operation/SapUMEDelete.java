package com.evolveum.polygon.connector.sap.ume.operation;

import com.evolveum.polygon.connector.sap.ume.SapUMEConfiguration;
import com.evolveum.polygon.connector.sap.ume.SapUMEConnection;
import com.evolveum.polygon.connector.sap.ume.SapUMEObjectClass;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Uid;
import org.openspml.message.DeleteRequest;

public class SapUMEDelete extends SapUMEAbstractOperation {

    private static final Log LOG = Log.getLog(SapUMEDelete.class);

    public SapUMEDelete(SapUMEConfiguration configuration, SapUMEConnection connection) {
        super(configuration, connection);
    }

    public void delete(SapUMEObjectClass objectClass, Uid uid) {
        LOG.info("Delete start : {0}:{1}", objectClass.toString(), uid.toString());
        if (objectClass.isRole()) {
            throw new ConnectorException("Deletion of roles is not supported from SAP UME");
        }
        DeleteRequest delReq = new DeleteRequest();
        delReq.setIdentifier(uid.getUidValue());
        super.getConnection().connect(delReq,LOG_OPERATION_DELETE);
        LOG.info("Delete finished : {0}:{1}", objectClass.toString(), uid.toString());
    }
}
