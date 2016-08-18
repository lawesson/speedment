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
import com.speedment.runtime.config.parameter.DbmsType;
import com.speedment.runtime.db.DatabaseNamingConvention;
import com.speedment.runtime.db.metadata.TypeInfoMetaData;
import com.speedment.runtime.internal.db.DefaultDatabaseNamingConvention;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author  Emil Forslund
 * @since   2.4.0
 */
public abstract class AbstractDbmsType implements DbmsType {

    @Provides(type = Provides.Type.SET)
    DbmsType provideDbmsType() {
        return this;
    }
    
    @Override
    public String getResultSetTableSchema() {
        return "TABLE_SCHEM";
    }

    @Override
    public String getSchemaTableDelimiter() {
        return ".";
    }

    @Override
    public boolean isSupported() {
        try {
            Class.forName(
                getDriverName(),
                false,
                AbstractDbmsType.class.getClassLoader()
            );

            return true;
        } catch (final ClassNotFoundException ex) {
            return false;
        }
    }

    @Override
    public DatabaseNamingConvention getDatabaseNamingConvention() {
        return new DefaultDatabaseNamingConvention();
    }

    @Override
    public Set<TypeInfoMetaData> getDataTypes() {
        return Collections.emptySet();
    }

    @Override
    public Optional<String> getDefaultDbmsName() {
        return Optional.empty();
    }

    @Override
    public String getInitialQuery() {
        return "select 1 from dual";
    }
}