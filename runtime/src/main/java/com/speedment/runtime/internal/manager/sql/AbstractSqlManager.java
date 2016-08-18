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
package com.speedment.runtime.internal.manager.sql;

import com.speedment.common.mapstream.MapStream;
import com.speedment.runtime.component.DbmsHandlerComponent;
import com.speedment.runtime.component.ProjectComponent;
import com.speedment.runtime.component.resultset.ResultSetMapperComponent;
import com.speedment.runtime.config.Column;
import com.speedment.runtime.config.Dbms;
import com.speedment.runtime.config.PrimaryKeyColumn;
import com.speedment.runtime.config.Project;
import com.speedment.runtime.config.Table;
import com.speedment.runtime.config.mapper.TypeMapper;
import com.speedment.runtime.config.parameter.DbmsType;
import com.speedment.runtime.db.AsynchronousQueryResult;
import com.speedment.runtime.db.DatabaseNamingConvention;
import com.speedment.runtime.db.DbmsOperationHandler;
import com.speedment.runtime.db.MetaResult;
import com.speedment.runtime.db.SqlFunction;
import com.speedment.runtime.db.SqlRunnable;
import com.speedment.runtime.exception.SpeedmentException;
import com.speedment.runtime.field.Field;
import com.speedment.runtime.internal.manager.AbstractManager;
import com.speedment.runtime.internal.manager.metaresult.SqlMetaResultImpl;
import com.speedment.runtime.internal.stream.builder.ReferenceStreamBuilder;
import com.speedment.runtime.internal.stream.builder.pipeline.PipelineImpl;
import com.speedment.runtime.internal.util.document.DocumentDbUtil;
import com.speedment.runtime.internal.util.document.DocumentUtil;
import com.speedment.runtime.stream.StreamDecorator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.speedment.common.lazy.specialized.LazyString;
import static com.speedment.runtime.internal.util.document.DocumentDbUtil.*;
import static com.speedment.runtime.internal.util.document.DocumentUtil.Name.DATABASE_NAME;
import static com.speedment.runtime.internal.util.document.DocumentUtil.ancestor;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static com.speedment.runtime.util.OptionalUtil.unwrap;
import static com.speedment.runtime.util.NullUtil.requireNonNulls;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import javax.inject.Inject;

/**
 *
 * @param <ENTITY> entity type for this Manager
 *
 * @author Per Minborg
 */
public abstract class AbstractSqlManager<ENTITY> extends AbstractManager<ENTITY> implements SqlManager<ENTITY> {

    private final LazyString sqlColumnList;
    private final LazyString sqlTableReference;
    private final LazyString sqlSelect;
    private final Map<String, Field<ENTITY>> fieldMap;
    private final boolean hasPrimaryKeyColumns;

    private SqlFunction<ResultSet, ENTITY> entityMapper;

    @Inject DbmsHandlerComponent dbmsHandlerComponent;
    @Inject ResultSetMapperComponent resultSetMapperComponent;
    @Inject ProjectComponent projectComponent;

    protected AbstractSqlManager(ProjectComponent projectComponent) {
        this.sqlColumnList     = LazyString.create();
        this.sqlTableReference = LazyString.create();
        this.sqlSelect         = LazyString.create();
        this.fieldMap          = new HashMap<>();

        this.hasPrimaryKeyColumns = primaryKeyFields().findAny().isPresent();
        
        createFieldTraitMap(projectComponent);
    }

    private void createFieldTraitMap(ProjectComponent projectComponent) {
        final Project project = projectComponent.getProject();
        final Table thisTable = getTable();

        // Only include fields that point towards a column in this table.
        // In the future we might add fields that reference columns in foreign
        // tables.
        fieldMap.putAll(fields()
            .filter(f -> f.findColumn(project)
                .map(c -> c.getParent())
                .map(t -> isSame(thisTable, t.get()))
                .orElse(false)
            )
            .collect(Collectors.toMap(f -> f.identifier().columnName(), identity()))
        );
    }

    @Override
    public Stream<ENTITY> nativeStream(StreamDecorator decorator) {
        final AsynchronousQueryResult<ENTITY> asynchronousQueryResult = decorator.apply(dbmsHandler().executeQueryAsync(getDbms(), sqlSelect(), Collections.emptyList(), entityMapper.unWrap()));
        final SqlStreamTerminator<ENTITY> terminator = new SqlStreamTerminator<>(this, asynchronousQueryResult, decorator);
        final Supplier<BaseStream<?, ?>> initialSupplier = () -> decorator.applyOnInitial(asynchronousQueryResult.stream());
        final Stream<ENTITY> result = decorator.applyOnFinal(new ReferenceStreamBuilder<>(new PipelineImpl<>(initialSupplier), terminator));

        // Make sure we are closing the ResultSet, Statement and Connection later
        result.onClose(asynchronousQueryResult::close);

        return result;
    }

    public <T> Stream<T> synchronousStreamOf(String sql, List<Object> values, SqlFunction<ResultSet, T> rsMapper) {
        requireNonNulls(sql, values, rsMapper);
        return dbmsHandler().executeQuery(getDbms(), sql, values, rsMapper);
    }

    /**
     * Counts the number of elements in the current table by querying the
     * database.
     *
     * @return the number of elements in the table
     */
    public long count() {
        return synchronousStreamOf(
            "SELECT COUNT(*) FROM " + sqlTableReference(),
            Collections.emptyList(),
            rs -> rs.getLong(1)
        ).findAny().get();
    }

    /**
     * Returns a {@code SELECT/FROM} SQL statement with the full column list and
     * the current table specified in accordance to the current
     * {@link DbmsType}. The specified statement will not have any trailing
     * spaces or semicolons.
     * <p>
     * <b>Example:</b>
     * <code>SELECT `id`, `name` FROM `myschema`.`users`</code>
     *
     * @return the SQL statement
     */
    public String sqlSelect() {
        return sqlSelect.getOrCompute(() -> "SELECT " + sqlColumnList() + " FROM " + sqlTableReference());
    }

    @Override
    public SqlFunction<ResultSet, ENTITY> getEntityMapper() {
        return entityMapper;
    }

    @Override
    public void setEntityMapper(SqlFunction<ResultSet, ENTITY> entityMapper) {
        this.entityMapper = requireNonNull(entityMapper);
    }

    @Override
    public ENTITY persist(ENTITY entity) throws SpeedmentException {
        return persistHelp(entity, Optional.empty());
    }

    @Override
    public ENTITY persist(ENTITY entity, Consumer<MetaResult<ENTITY>> listener) throws SpeedmentException {
        requireNonNulls(entity, listener);
        return persistHelp(entity, Optional.of(listener));
    }

    @Override
    public ENTITY update(ENTITY entity) {
        requireNonNull(entity);
        return updateHelper(entity, Optional.empty());
    }

    @Override
    public ENTITY update(ENTITY entity, Consumer<MetaResult<ENTITY>> listener) throws SpeedmentException {
        requireNonNulls(entity, listener);
        return updateHelper(entity, Optional.of(listener));
    }

    @Override
    public ENTITY remove(ENTITY entity) {
        requireNonNull(entity);
        return removeHelper(entity, Optional.empty());
    }

    @Override
    public ENTITY remove(ENTITY entity, Consumer<MetaResult<ENTITY>> listener) throws SpeedmentException {
        requireNonNulls(entity, listener);
        return removeHelper(entity, Optional.of(listener));
    }

    /**
     * Short-cut for retrieving the current {@link Dbms}.
     *
     * @return the current dbms
     */
    protected final Dbms getDbms() {
        return ancestor(getTable(), Dbms.class).get();
    }

    /**
     * Short-cut for retrieving the current {@link DbmsType}.
     *
     * @return the current dbms type
     */
    protected final DbmsType getDbmsType() {
        return dbmsTypeOf(dbmsHandlerComponent, getDbms());
    }

    /**
     * Short-cut for retrieving the current {@link DbmsOperationHandler}.
     *
     * @return the current dbms handler
     */
    protected final DbmsOperationHandler dbmsHandler() {
        return findDbmsType(dbmsHandlerComponent, getDbms()).getOperationHandler();
    }

    /**
     * Returns the column corresponding to a particular field in an entity
     * managed by this manager.
     * 
     * @param field  the field
     * @return       the corresponding Column
     */
    protected Column getColumn(Field<ENTITY> field) {
        return field.findColumn(projectComponent.getProject()).orElseThrow(() -> new SpeedmentException("Could not find column '" + field.identifier() + "'.")
        );
    }
    
    /**
     * Short-cut for retrieving the current {@link DatabaseNamingConvention}.
     *
     * @return the current naming convention
     */
    protected final DatabaseNamingConvention naming() {
        return getDbmsType().getDatabaseNamingConvention();
    }

    /**
     * Returns a comma separated list of column names, fully formatted in
     * accordance to the current {@link DbmsType}.
     *
     * @return the comma separated column list
     */
    private String sqlColumnList() {
        return sqlColumnList.getOrCompute(() -> sqlColumnList(Function.identity()));
    }

    /**
     * Returns a {@code AND} separated list of {@link PrimaryKeyColumn} database
     * names, formatted in accordance to the current {@link DbmsType}.
     *
     * @param postMapper mapper to be applied to each column name
     * @return list of fully quoted primary key column names
     */
    protected String sqlColumnList(Function<String, String> postMapper) {
        requireNonNull(postMapper);
        return getTable().columns()
            .filter(Column::isEnabled)
            .map(Column::getName)
            .map(naming()::encloseField)
            .map(postMapper)
            .collect(joining(","));
    }

    /**
     * Returns a {@code AND} separated list of {@link PrimaryKeyColumn} database
     * names, formatted in accordance to the current {@link DbmsType}.
     *
     * @return list of fully quoted primary key column names
     */
    private String sqlPrimaryKeyColumnList(Function<String, String> postMapper) {
        requireNonNull(postMapper);
        return getTable().primaryKeyColumns()
            .map(this::findColumn)
            .map(Column::getName)
            .map(naming()::encloseField)
            .map(postMapper)
            .collect(joining(" AND "));
    }

    private Column findColumn(PrimaryKeyColumn pkc) {
        return pkc.findColumn().orElseThrow(() -> new SpeedmentException("Cannot find column for " + pkc));
    }

    /**
     * Returns the full name of a table formatted in accordance to the current
     * {@link DbmsType}. The returned value will be within quotes if that is
     * what the database expects.
     *
     * @return the full quoted table name
     */
    protected String sqlTableReference() {
        return sqlTableReference.getOrCompute(() -> naming().fullNameOf(getTable()));
    }

    private <F extends Field<ENTITY>> Object toDatabaseType(F field, ENTITY entity) {
        final Object javaValue = unwrap(get(entity, field.identifier()));
        
        @SuppressWarnings("unchecked")
        final Object dbValue = ((TypeMapper<Object, Object>) field.typeMapper()).toDatabaseType(javaValue);
        
        return dbValue;
    }

    private ENTITY persistHelp(ENTITY entity, Optional<Consumer<MetaResult<ENTITY>>> listener) throws SpeedmentException {
        final List<Column> cols = persistColumns(entity);
        final StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(sqlTableReference());
        sb.append(" (").append(persistColumnList(cols)).append(")");
        sb.append(" VALUES ");
        sb.append("(").append(persistColumnListWithQuestionMarks(cols)).append(")");

        @SuppressWarnings("unchecked")
        final List<Object> values = cols.stream()
            .map(Column::getName)
            .map(fieldMap::get)
            .map(f -> toDatabaseType(f, entity))
            .collect(toList());
 
        @SuppressWarnings("unchecked")
        final Map<Field<ENTITY>, Column> generatedFields = MapStream.fromKeys(fields(), 
            f -> DocumentDbUtil.referencedColumn(projectComponent.getProject(), f.identifier()))
                .filterValue(Column::isAutoIncrement)
                .toMap();

        final Function<ENTITY, Consumer<List<Long>>> generatedKeyconsumer = newEntity -> {
            return l -> {
                if (!l.isEmpty()) {
                    final AtomicInteger cnt = new AtomicInteger();
                    // Just assume that they are in order, what else is there to do?
                    generatedFields
                        .forEach((f, col) -> {

                            // Cast from Long to the column target type
                            final Object val = resultSetMapperComponent
                                .apply(col.findDatabaseType())
                                .parse(l.get(cnt.getAndIncrement()));

                            @SuppressWarnings("unchecked")
                            final Object javaValue = ((TypeMapper<Object, Object>) 
                                f.typeMapper()).toJavaType(col, getEntityClass(), val);
                            
                            set(newEntity, f.identifier(), javaValue);
                        });
                }
            };
        };

        executeInsert(entity, sb.toString(), values, generatedFields.keySet(), generatedKeyconsumer, listener);
        return entity;
    }

    private ENTITY updateHelper(ENTITY entity, Optional<Consumer<MetaResult<ENTITY>>> listener) throws SpeedmentException {
        assertHasPrimaryKeyColumns();
        final StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(sqlTableReference()).append(" SET ");
        sb.append(sqlColumnList(n -> n + " = ?"));
        sb.append(" WHERE ");
        sb.append(sqlPrimaryKeyColumnList(pk -> pk + " = ?"));

        final List<Object> values = fields()
            .map(f -> toDatabaseType(f, entity))
            .collect(Collectors.toList());

        primaryKeyFields()
            .map(Field::identifier)
            .forEachOrdered(f -> values.add(get(entity, f)));

        executeUpdate(sb.toString(), values, listener);
        return entity;
    }

    private ENTITY removeHelper(ENTITY entity, Optional<Consumer<MetaResult<ENTITY>>> listener) throws SpeedmentException {
        assertHasPrimaryKeyColumns();
        final StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(sqlTableReference());
        sb.append(" WHERE ");
        sb.append(sqlPrimaryKeyColumnList(pk -> pk + " = ?"));

        final List<Object> values = primaryKeyFields()
            .map(f -> toDatabaseType(f, entity))
            .collect(toList());

        executeDelete(sb.toString(), values, listener);
        return entity;
    }

    private String persistColumnList(List<Column> cols) {
        return cols.stream()
            .map(Column::getName)
            .map(naming()::encloseField)
            .collect(joining(","));
    }

    private String persistColumnListWithQuestionMarks(List<Column> cols) {
        return cols.stream()
            .map(c -> "?")
            .collect(joining(","));
    }

    /**
     * Returns a List of the columns that shall be used in an insert/update
     * statement. Some database types (e.g. Postgres) does not allow auto
     * increment columns that are null in an insert/update statement.
     *
     * @param entity to be inserted/updated
     * @return a List of the columns that shall be used in an insert/update
     * statement
     */
    protected List<Column> persistColumns(ENTITY entity) {
        return getTable().columns()
            .filter(c -> isPersistColumn(entity, c))
            .collect(toList());
    }

    /**
     * Returns if a columns that shall be used in an insert/update statement.
     * Some database types (e.g. Postgres) does not allow auto increment columns
     * that are null in an insert/update statement.
     *
     * @param entity to be inserted/updated
     * @param c column
     * @return if a columns that shall be used in an insert/update statement
     */
    protected boolean isPersistColumn(ENTITY entity, Column c) {
        if (c.isAutoIncrement()) {
            final Field<ENTITY> field = fieldMap.get(c.getName());
            if (field != null) {
                final Object colValue = get(entity, field.identifier());
                if (colValue != null) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void executeInsert(
        final ENTITY entity,
        final String sql,
        final List<Object> values,
        final Collection<Field<ENTITY>> generatedFields,
        final Function<ENTITY, Consumer<List<Long>>> generatedKeyconsumer,
        final Optional<Consumer<MetaResult<ENTITY>>> listener
    ) throws SpeedmentException {
        executeHelper(sql, values, listener,
            () -> dbmsHandler().executeInsert(
                getDbms(), sql, values, generatedFields, generatedKeyconsumer.apply(entity)
            )
        );
    }

    private void executeUpdate(
        final String sql,
        final List<Object> values,
        final Optional<Consumer<MetaResult<ENTITY>>> listener
    ) throws SpeedmentException {
        executeHelper(sql, values, listener, () -> dbmsHandler().executeUpdate(getDbms(), sql, values));
    }

    private void executeDelete(
        final String sql,
        final List<Object> values,
        final Optional<Consumer<MetaResult<ENTITY>>> listener
    ) throws SpeedmentException {
        executeHelper(sql, values, listener, () -> dbmsHandler().executeDelete(getDbms(), sql, values));
    }

    private void executeHelper(
        String sql,
        List<Object> values,
        Optional<Consumer<MetaResult<ENTITY>>> listener,
        SqlRunnable action
    ) throws SpeedmentException {
        requireNonNulls(sql, values, listener, action);

        final SqlMetaResultImpl<ENTITY> meta = listener.isPresent()
            ? new SqlMetaResultImpl<ENTITY>()
            .setQuery(sql)
            .setParameters(values)
            : null;

        try {
            action.run();
        } catch (final SQLException sqle) {
            if (meta != null) {
                meta.setThrowable(sqle);
            }
            throw new SpeedmentException(sqle);
        } finally {
            listener.ifPresent(c -> c.accept(meta));
        }
    }

    private void assertHasPrimaryKeyColumns() {
        if (!hasPrimaryKeyColumns) {
            throw new SpeedmentException(
                "The table "
                + DocumentUtil.relativeName(getTable(), Project.class, DATABASE_NAME)
                + " does not have any primary keys. Some operations like "
                + "update() and remove() requires at least one primary key."
            );
        }
    }
}
