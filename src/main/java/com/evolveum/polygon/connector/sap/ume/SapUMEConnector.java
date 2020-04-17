/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.polygon.connector.sap.ume;

import com.evolveum.polygon.connector.sap.ume.operation.SapUMECreate;
import com.evolveum.polygon.connector.sap.ume.operation.SapUMEDelete;
import com.evolveum.polygon.connector.sap.ume.operation.SapUMEQuery;
import com.evolveum.polygon.connector.sap.ume.operation.SapUMEUpdate;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;
import org.openspml.message.Filter;

import java.util.Set;

/*
 * @version 0.0.5.0-SNAPSHOT
 * @since   2020-02-14
 * @author  Frantisek Reznicek
 */

@ConnectorClass(displayNameKey = "sap-ume.connector.display", configurationClass = SapUMEConfiguration.class)
public class SapUMEConnector implements Connector, TestOp, SchemaOp, SearchOp<Filter>, UpdateOp, CreateOp, DeleteOp, PoolableConnector {

    private static final Log LOG = Log.getLog(SapUMEConnector.class);

    private SapUMEQuery query = null;
    private SapUMEUpdate update = null;
    private SapUMECreate create = null;
    private SapUMEDelete delete = null;

    private SapUMEConfiguration configuration;
    private SapUMEConnection connection;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        LOG.info("Initialization start, configuration: {0}", configuration.toString());
        this.configuration = (SapUMEConfiguration) configuration;
        this.connection = new SapUMEConnection(this.configuration);
        this.query = new SapUMEQuery(this.configuration, this.connection);
        this.update = new SapUMEUpdate(this.configuration, this.connection);
        this.create = new SapUMECreate(this.configuration, this.connection);
        this.delete = new SapUMEDelete(this.configuration, this.connection);
        LOG.info("Initialization finished");
    }

    @Override
    public void dispose() {
        LOG.info("Dispose start");
        configuration = null;
        if (connection != null) {
            connection.dispose();
            connection = null;
        }
        if (this.query != null) {
            this.query.dispose();
            this.query = null;
        }
        if (this.update != null) {
            this.update.dispose();
            this.update = null;
        }
        if (this.create != null) {
            this.create.dispose();
            this.create = null;
        }
        if (this.delete != null) {
            this.delete.dispose();
            this.delete = null;
        }
        LOG.info("Dispose finished");
    }

    @Override
    public void test() {
        this.connection.test();
    }

    @Override
    public Schema schema() {
        return SapUMESchema.getSchema(this.configuration);
    }

    @Override
    public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
        return new SapUMEFilterTranslator(new SapUMEObjectClass(objectClass));
    }

    @Override
    public void executeQuery(ObjectClass objectClass, Filter filter, ResultsHandler resultsHandler, OperationOptions operationOptions) {
        this.query.executeQuery(new SapUMEObjectClass(objectClass), filter, resultsHandler, operationOptions);
    }

    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> set, OperationOptions operationOptions) {
        return update.update(new SapUMEObjectClass(objectClass), uid, set);
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> set, OperationOptions operationOptions) {
        return create.create(new SapUMEObjectClass(objectClass), set);
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {
        delete.delete(new SapUMEObjectClass(objectClass), uid);
    }

    @Override
    public void checkAlive() {
        if (this.connection == null) {
            throw new ConnectionFailedException("Connection check failed (connection is null), connection was not initialized");
        }
    }
}
