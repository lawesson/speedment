/**
 *
 * Copyright (c) 2006-2015, Speedment, Inc. All Rights Reserved.
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
package com.speedment.internal.core.config.db.mutator;

import com.speedment.config.db.Column;
import com.speedment.config.db.Dbms;
import com.speedment.config.db.ForeignKey;
import com.speedment.config.db.ForeignKeyColumn;
import com.speedment.config.db.Index;
import com.speedment.config.db.IndexColumn;
import com.speedment.config.db.PrimaryKeyColumn;
import com.speedment.config.db.Project;
import com.speedment.config.db.Schema;
import com.speedment.config.db.Table;

/**
 *
 * @author Per Minborg
 */
public interface DocumentMutator {

    void put(String key, Object value);

    
    static ColumnMutator of(Column column) {
        return new ColumnMutator(column);
    }

    static DbmsMutator of(Dbms column) {
        return new DbmsMutator(column);
    }

    static ForeignKeyColumnMutator of(ForeignKeyColumn fkcolumn) {
        return new ForeignKeyColumnMutator(fkcolumn);
    }

    static ForeignKeyMutator of(ForeignKey fk) {
        return new ForeignKeyMutator(fk);
    }

    static IndexColumnMutator of(IndexColumn indexColumn) {
        return new IndexColumnMutator(indexColumn);
    }

    static IndexMutator of(Index index) {
        return new IndexMutator(index);
    }

    static PrimaryKeyColumnMutator of(PrimaryKeyColumn pkColumn) {
        return new PrimaryKeyColumnMutator(pkColumn);
    }

    static ProjectMutator of(Project project) {
        return new ProjectMutator(project);
    }

    static SchemaMutator of(Schema schema) {
        return new SchemaMutator(schema);
    }

    static TableMutator of(Table table) {
        return new TableMutator(table);
    }

    
}
