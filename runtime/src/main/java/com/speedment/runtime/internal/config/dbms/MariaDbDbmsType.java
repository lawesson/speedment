/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.runtime.internal.config.dbms;

import com.speedment.common.dagger.Provides;
import com.speedment.runtime.config.Dbms;
import com.speedment.runtime.db.ConnectionUrlGenerator;
import com.speedment.runtime.db.DatabaseNamingConvention;
import com.speedment.runtime.db.DbmsMetadataHandler;
import com.speedment.runtime.db.DbmsOperationHandler;
import com.speedment.runtime.internal.db.AbstractDatabaseNamingConvention;
import com.speedment.runtime.internal.db.mysql.MySqlDbmsMetadataHandler;
import com.speedment.runtime.internal.db.mysql.MySqlDbmsOperationHandler;
import com.speedment.runtime.internal.manager.sql.MySqlSpeedmentPredicateView;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toSet;
import com.speedment.runtime.field.predicate.FieldPredicateView;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author  Per Minborg
 * @author  Emil Forslund
 */
public final class MariaDbDbmsType extends AbstractDbmsType {
    
    public final static String INJECT_NAME = "MariaDB";
    
    private final MariaDbNamingConvention namingConvention;
    private final MariaDbConnectionUrlGenerator connectionUrlGenerator;
    
    private @Inject MySqlDbmsMetadataHandler metadataHandler;
    private @Inject MySqlDbmsOperationHandler operationHandler;
    
    private @Inject @Named(INJECT_NAME) MariaDbDbmsType() {
        namingConvention       = new MariaDbNamingConvention();
        connectionUrlGenerator = new MariaDbConnectionUrlGenerator();
    }
    
    @Override
    public String getName() {
        return "MariaDB";
    }

    @Override
    public String getDriverManagerName() {
        return "MariaDB JDBC Driver";
    }

    @Override
    public int getDefaultPort() {
        return 3305;
    }

    @Override
    public String getDbmsNameMeaning() {
        return "Just a name";
    }

    @Override
    public String getDriverName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override @Provides @Named(INJECT_NAME)
    public DatabaseNamingConvention getDatabaseNamingConvention() {
        return namingConvention;
    }

    @Override @Provides @Named(INJECT_NAME)
    public DbmsMetadataHandler getMetadataHandler() {
        return metadataHandler;
    }

    @Override @Provides @Named(INJECT_NAME)
    public DbmsOperationHandler getOperationHandler() {
        return operationHandler;
    }

    @Override @Provides @Named(INJECT_NAME)
    public ConnectionUrlGenerator getConnectionUrlGenerator() {
        return connectionUrlGenerator;
    }

    @Override @Provides @Named(INJECT_NAME)
    public FieldPredicateView getSpeedmentPredicateView() {
        return new MySqlSpeedmentPredicateView(namingConvention);
    }

    @Override
    public String getInitialQuery() {
        return "select version() as `MariaDB version`";
    }

    private final static class MariaDbNamingConvention extends AbstractDatabaseNamingConvention {

        private final static String 
            ENCLOSER = "`",
            QUOTE    = "'";
        
        private final static Set<String> EXCLUDE_SET = Stream.of(
            "information_schema"
        ).collect(collectingAndThen(toSet(), Collections::unmodifiableSet));
        
        private MariaDbNamingConvention() {}
        
        @Override
        public Set<String> getSchemaExcludeSet() {
            return EXCLUDE_SET;
        }

        @Override
        protected String getFieldQuoteStart() {
            return QUOTE;
        }

        @Override
        protected String getFieldQuoteEnd() {
            return QUOTE;
        }

        @Override
        protected String getFieldEncloserStart() {
            return ENCLOSER;
        }

        @Override
        protected String getFieldEncloserEnd() {
            return ENCLOSER;
        }
    }
    
    private final static class MariaDbConnectionUrlGenerator implements ConnectionUrlGenerator {

        private MariaDbConnectionUrlGenerator() {}
        
        @Override
        public String from(Dbms dbms) {
            final StringBuilder result = new StringBuilder()
                .append("jdbc:mariadb://")
                .append(dbms.getIpAddress().orElse(""));
            
            dbms.getPort().ifPresent(p -> result.append(":").append(p));
            result.append(
                "/?useUnicode=true" +
                "&characterEncoding=UTF-8" +
                "&useServerPrepStmts=true" +
                "&useCursorFetch=true" +
                "&zeroDateTimeBehavior=convertToNull"
            );
            
            return result.toString();
        }
    }
}
