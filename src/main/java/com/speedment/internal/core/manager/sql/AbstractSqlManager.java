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
package com.speedment.internal.core.manager.sql;

import com.speedment.Speedment;
import com.speedment.config.db.Column;
import com.speedment.config.db.Dbms;
import com.speedment.config.db.PrimaryKeyColumn;
import com.speedment.config.db.Project;
import com.speedment.config.db.Table;
import com.speedment.config.db.mapper.TypeMapper;
import com.speedment.config.db.parameters.DbmsType;
import com.speedment.config.db.trait.HasEnabled;
import com.speedment.db.AsynchronousQueryResult;
import com.speedment.db.DatabaseNamingConvention;
import com.speedment.db.DbmsHandler;
import com.speedment.db.MetaResult;
import com.speedment.db.SqlFunction;
import com.speedment.db.SqlRunnable;
import com.speedment.exception.SpeedmentException;
import com.speedment.field.FieldIdentifier;
import com.speedment.field.trait.FieldTrait;
import com.speedment.field.trait.ReferenceFieldTrait;
import com.speedment.internal.core.manager.AbstractManager;
import com.speedment.internal.core.manager.metaresult.SqlMetaResultImpl;
import static com.speedment.internal.core.stream.OptionalUtil.unwrap;
import com.speedment.internal.core.stream.builder.ReferenceStreamBuilder;
import com.speedment.internal.core.stream.builder.pipeline.PipelineImpl;
import com.speedment.internal.util.LazyString;
import com.speedment.internal.util.document.DocumentDbUtil;
import static com.speedment.internal.util.document.DocumentDbUtil.dbmsTypeOf;
import static com.speedment.internal.util.document.DocumentDbUtil.isSame;
import com.speedment.internal.util.document.DocumentUtil;
import static com.speedment.internal.util.document.DocumentUtil.Name.DATABASE_NAME;
import static com.speedment.internal.util.document.DocumentUtil.ancestor;
import com.speedment.stream.StreamDecorator;
import static com.speedment.util.NullUtil.requireNonNulls;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import static java.util.function.Function.identity;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;

/**
 *
 * @author pemi
 *
 * @param <ENTITY> Entity type for this Manager
 */
public abstract class AbstractSqlManager<ENTITY> extends AbstractManager<ENTITY> implements SqlManager<ENTITY> {

    private final LazyString sqlColumnList;
    private final LazyString sqlTableReference;
    private final LazyString sqlSelect;
    private final Map<String, FieldTrait> fieldTraitMap;
    private final boolean hasPrimaryKeyColumns;

    private SqlFunction<ResultSet, ENTITY> entityMapper;

    protected AbstractSqlManager(Speedment speedment) {
        super(speedment);
        this.sqlColumnList = LazyString.create();
        this.sqlTableReference = LazyString.create();
        this.sqlSelect = LazyString.create();

        final Table thisTable = getTable();

        // Only include fields that point towards a column in this table.
        // In the future we might add fields that reference columns in foreign
        // tables.
        this.fieldTraitMap = fields()
            .filter(f
                -> f.findColumn(speedment)
                .map(c -> c.getParent())
                .map(t -> isSame(thisTable, t.get()))
                .orElse(false)
            )
            .collect(Collectors.toMap(f -> f.getIdentifier().columnName(), identity()));

        this.hasPrimaryKeyColumns = primaryKeyFields().findAny().isPresent();
    }

    @Override
    public Stream<ENTITY> nativeStream(StreamDecorator decorator) {
        final AsynchronousQueryResult<ENTITY> asynchronousQueryResult = decorator.apply(dbmsHandler().executeQueryAsync(sqlSelect(), Collections.emptyList(), entityMapper.unWrap()));
        final SqlStreamTerminator<ENTITY> terminator = new SqlStreamTerminator<>(this, asynchronousQueryResult, decorator);
        final Supplier<BaseStream<?, ?>> initialSupplier = () -> decorator.applyOnInitial(asynchronousQueryResult.stream());
        final Stream<ENTITY> result = decorator.applyOnFinal(new ReferenceStreamBuilder<>(new PipelineImpl<>(initialSupplier), terminator));

        // Make sure we are closing the ResultSet, Statement and Connection later
        result.onClose(asynchronousQueryResult::close);

        return result;
    }

    public <T> Stream<T> synchronousStreamOf(String sql, List<Object> values, SqlFunction<ResultSet, T> rsMapper) {
        requireNonNulls(sql, values, rsMapper);
        return dbmsHandler().executeQuery(sql, values, rsMapper);
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
        return dbmsTypeOf(speedment, getDbms());
    }

    /**
     * Short-cut for retrieving the current {@link DbmsHandler}.
     *
     * @return the current dbms handler
     */
    protected final DbmsHandler dbmsHandler() {
        return speedment.getDbmsHandlerComponent().get(getDbms());
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
            .filter(HasEnabled::isEnabled)
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

    private <F extends FieldTrait & ReferenceFieldTrait<ENTITY, ?, ?>> Object toDatabaseType(F field, ENTITY entity) {
        final Object javaValue = unwrap(get(entity, field.getIdentifier()));
        @SuppressWarnings("unchecked")
        final Object dbValue = ((TypeMapper<Object, Object>) field.typeMapper()).toDatabaseType(javaValue);
        return dbValue;
    }

    private <F extends FieldTrait & ReferenceFieldTrait<ENTITY, ?, ?>> ENTITY persistHelp(ENTITY entity, Optional<Consumer<MetaResult<ENTITY>>> listener) throws SpeedmentException {
        final List<Column> cols = persistColumns(entity);
        final StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(sqlTableReference());
        sb.append(" (").append(persistColumnList(cols)).append(")");
        sb.append(" VALUES ");
        sb.append("(").append(persistColumnListWithQuestionMarks(cols)).append(")");

        @SuppressWarnings("unchecked")
        final List<Object> values = cols.stream()
            .map(Column::getName)
            .map(fieldTraitMap::get)
            .filter(ReferenceFieldTrait.class::isInstance)
            .map(f -> (FieldTrait & ReferenceFieldTrait<ENTITY, ?, ?>) f)
            .map(f -> toDatabaseType(f, entity))
            .collect(toList());

        // TODO: Make autoinc part of FieldTrait
        @SuppressWarnings("unchecked")
        final List<F> generatedFields = fields()
            .filter(f -> DocumentDbUtil.referencedColumn(speedment, f.getIdentifier()).isAutoIncrement())
            .filter(ReferenceFieldTrait.class::isInstance)
            .map(f -> (F) f)
            .collect(toList());

        final Function<ENTITY, Consumer<List<Long>>> generatedKeyconsumer = builder -> {
            return l -> {
                if (!l.isEmpty()) {
                    final AtomicInteger cnt = new AtomicInteger();
                    // Just assume that they are in order, what else is there to do?
                    generatedFields
                        .forEach(f -> {

                            // Cast from Long to the column target type
                            final Object val = speedment
                                .getResultSetMapperComponent()
                                .apply(f.typeMapper().getJavaType())
                                .parse(l.get(cnt.getAndIncrement()));

                            @SuppressWarnings("unchecked")
                            final Object javaValue = ((TypeMapper<Object, Object>) f.typeMapper()).toJavaType(val);
                            set(builder, f.getIdentifier(), javaValue);
                        });
                }
            };
        };

        executeInsert(entity, sb.toString(), values, generatedFields, generatedKeyconsumer, listener);
        return entity;
    }

    private ENTITY updateHelper(ENTITY entity, Optional<Consumer<MetaResult<ENTITY>>> listener) throws SpeedmentException {
        assertHasPrimaryKeyColumns();
        final StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(sqlTableReference()).append(" SET ");
        sb.append(sqlColumnList(n -> n + " = ?"));
        sb.append(" WHERE ");
        sb.append(sqlPrimaryKeyColumnList(pk -> pk + " = ?"));

        final List<Object> values = castedFieldsOf(this::fields)
            .map(f -> toDatabaseType(f, entity))
            .collect(Collectors.toList());

        castedFieldsOf(this::primaryKeyFields)
            .map(ReferenceFieldTrait::getIdentifier)
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

        final List<Object> values = castedFieldsOf(this::primaryKeyFields)
            .map(f -> toDatabaseType(f, entity))
            .collect(toList());

        executeDelete(sb.toString(), values, listener);
        return entity;
    }

    private <T extends FieldTrait & ReferenceFieldTrait<ENTITY, ?, ?>> Stream<T> castedFieldsOf(Supplier<Stream<FieldTrait>> supplier) {
        @SuppressWarnings("unchecked")
        final Stream<T> result = supplier.get()
            .filter(ReferenceFieldTrait.class::isInstance)
            .map(f -> (T) f);
        return result;
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
            final FieldTrait ft = fieldTraitMap.get(c.getName());
            if (ft != null) {
                @SuppressWarnings("unchecked")
                final FieldIdentifier<ENTITY> fi = (FieldIdentifier<ENTITY>) ft.getIdentifier();
                final Object colValue = get(entity, fi);
                if (colValue != null) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private <F extends FieldTrait & ReferenceFieldTrait<ENTITY, ?, ?>> void executeInsert(
        final ENTITY entity,
        final String sql,
        final List<Object> values,
        final List<F> generatedFields,
        final Function<ENTITY, Consumer<List<Long>>> generatedKeyconsumer,
        final Optional<Consumer<MetaResult<ENTITY>>> listener
    ) throws SpeedmentException {
        executeHelper(sql, values, listener,
            () -> dbmsHandler().executeInsert(
                sql, values, generatedFields, generatedKeyconsumer.apply(entity)
            )
        );
    }

    private void executeUpdate(
        final String sql,
        final List<Object> values,
        final Optional<Consumer<MetaResult<ENTITY>>> listener
    ) throws SpeedmentException {
        executeHelper(sql, values, listener, () -> dbmsHandler().executeUpdate(sql, values));
    }

    private void executeDelete(
        final String sql,
        final List<Object> values,
        final Optional<Consumer<MetaResult<ENTITY>>> listener
    ) throws SpeedmentException {
        executeHelper(sql, values, listener, () -> dbmsHandler().executeDelete(sql, values));
    }

    private void executeHelper(
        final String sql,
        final List<Object> values,
        final Optional<Consumer<MetaResult<ENTITY>>> listener,
        final SqlRunnable action
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

    @Override
    public Clob createClob() throws SQLException {
        return dbmsHandler().createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return dbmsHandler().createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return dbmsHandler().createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return dbmsHandler().createSQLXML();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return dbmsHandler().createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return dbmsHandler().createStruct(typeName, attributes);
    }

    @Override
    public String fullColumnName(FieldTrait fieldTrait) {
        return naming().fullNameOf(fieldTrait.getIdentifier());
    }

}
