package org.thermoweb.database.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.thermoweb.database.annotations.Column;
import org.thermoweb.database.annotations.Entity;
import org.thermoweb.database.annotations.Id;
import org.thermoweb.database.annotations.ManyToMany;
import org.thermoweb.database.connection.ConnectionManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TimeZone;

@Slf4j
public class Repository<T> {
    private static final int DEFAULT_TIMEOUT = 30;
    private static final String INSERT_STATEMENT = "INSERT INTO %s (%s) VALUES (%s);";

    private static final String postgreTimestampPattern = "YYYY-MM-dd HH24:MI:ss";
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(Locale.FRANCE)
                    .withZone(ZoneId.of("UTC"));
    private static final Calendar tzUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private final String tableName;
    private final Class<?> typeArgument;
    private final Map<String, Field> columnMap;
    private Field idField;

    protected Repository() {
        Type superclass = getClass().getGenericSuperclass();
        ParameterizedType parameterized = (ParameterizedType) superclass;
        typeArgument = (Class<?>) parameterized.getActualTypeArguments()[0];

        if (!typeArgument.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException(
                    String.format("Class %s does not have Entity annotation", typeArgument.getName()));
        }

        tableName = typeArgument.getAnnotation(Entity.class).table();

        columnMap = new HashMap<>();
        for (Field field : typeArgument.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                field.setAccessible(true);
                String bddFieldName = field.getAnnotation(Column.class).name();
                if (bddFieldName.equals("")) {
                    bddFieldName = field.getName();
                }
                columnMap.put(bddFieldName, field);
            }
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
            }
        }
    }

    public T save(T dto) throws SQLException, IllegalAccessException {
        log.debug("saving dto " + toString(dto));
        Optional<T> dbDto = findById((Integer) idField.get(dto));
        if (dbDto.isPresent()) {
            return update(dto, dbDto.get());
        }
        return create(dto);
    }

    private T create(T dto) throws SQLException, IllegalAccessException {

        Field[] fields = dto.getClass().getDeclaredFields();
        StringJoiner columnJoiner = new StringJoiner(", ");
        StringJoiner valuesJoiner = new StringJoiner(", ");
        List<Object> values = new ArrayList<>();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Optional.ofNullable(getColumnInfos(field, dto))
                        .ifPresent(
                                c -> {
                                    columnJoiner.add(c.getLeft());
                                    valuesJoiner.add("?");
                                    values.add(c.getRight());
                                });
            } else if (field.isAnnotationPresent(ManyToMany.class)) {
                ManyToMany many = field.getAnnotation(ManyToMany.class);
                // FIXME: handle many to many relations :/
            }
        }

        try (Connection connection = ConnectionManager.getConnection()) {
            String preparedStatement =
                    String.format(INSERT_STATEMENT, tableName, columnJoiner, valuesJoiner);
            PreparedStatement statement =
                    connection.prepareStatement(preparedStatement, Statement.RETURN_GENERATED_KEYS);
            statement.setQueryTimeout(DEFAULT_TIMEOUT);
            setObjects(values, statement);

            log.debug("executing statement " + statement);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return findById(generatedKeys.getInt(this.idField.getName())).orElseThrow();
                }
            }
        }

        throw new SQLException("failed to retrieve or create entity");
    }

    private T update(T dto, T databaseDto) {
        // FIXME: implements update
        // option 1 : stream each field and compare values then edit "update statement"
        // option 2 : use PatchUtils and then generate "update statement"
        return dto;
    }

    private Pair<String, Object> getColumnInfos(Field field, T dto) throws IllegalAccessException {
        if (field.getType().isAnnotationPresent(Entity.class)) {
            Field childIdField =
                    Arrays.stream(field.getType().getDeclaredFields())
                            .filter(f -> f.isAnnotationPresent(Id.class))
                            .findFirst()
                            .orElseThrow();
            childIdField.setAccessible(true);
            field.setAccessible(true);
            Integer value = (Integer) childIdField.get(field.get(dto));
            return Pair.of(field.getAnnotation(Column.class).name(), value);
        } else {
            field.setAccessible(true);
            Object value = field.get(dto);
            field.setAccessible(false);

            if (value != null) {
                String column = field.getAnnotation(Column.class).name();

                if (List.of(String.class, Instant.class).contains(field.getType())) {
                    return Pair.of(column, value);
                } else if (field.getType().equals(LocalDateTime.class)) {
                    return Pair.of(
                            column,
                            String.format(
                                    "TO_TIMESTAMP('%s', '%s')",
                                    formatter.format((LocalDateTime) value), postgreTimestampPattern));
                }
            }
        }

        return null;
    }

    private void setObjects(List<Object> values, PreparedStatement statement) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (value instanceof Instant) {
                statement.setTimestamp(i + 1, Timestamp.from((Instant) value), tzUTC);
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }

    public List<T> findAll() {
        return findByQuery(String.format("select * from %s", tableName));
    }

    public Optional<T> findById(Integer id) {
        try (Connection connection = ConnectionManager.getConnection()) {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(DEFAULT_TIMEOUT);

            String query = String.format("select * from %s where %s = %d", tableName, idField.getName(), id);
            log.debug("executing query : " + query);
            ResultSet rs = statement.executeQuery(query);

            if (rs.next()) {
                return Optional.ofNullable(createEntity(rs));
            }

            return Optional.empty();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return Optional.empty();
    }

    protected List<T> findByQuery(String query, Object... args) {
        try (Connection connection = ConnectionManager.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(query);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return findByPreparedStatement(statement);
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }

        return Collections.emptyList();
    }

    private List<T> findByPreparedStatement(PreparedStatement statement) throws SQLException {
        log.debug("executing query " + statement);
        statement.setQueryTimeout(DEFAULT_TIMEOUT);
        ResultSet rs = statement.executeQuery();

        List<T> resultList = new ArrayList<>();
        while (rs.next()) {
            resultList.add(createEntity(rs));
        }

        return resultList;
    }

    private T createEntity(ResultSet rs) {
        T object = null;
        try {
            object = (T) typeArgument.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                | NoSuchMethodException
                | InvocationTargetException
                | IllegalAccessException e) {
            log.error("error getting object : " + e.getMessage());
        }

        for (Map.Entry<String, Field> entry : columnMap.entrySet()) {
            Field field = entry.getValue();
            try {
                if (field.getType().isAnnotationPresent(Entity.class)) {
                    addChild(rs, object, field);
                } else if (field.getType().equals(String.class)) {
                    field.set(object, rs.getString(entry.getKey()));
                } else if (field.getType().equals(Integer.class)) {
                    field.set(object, rs.getInt(entry.getKey()));
                } else if (field.getType().equals(double.class)) {
                    field.set(object, rs.getDouble(entry.getKey()));
                } else if (field.getType().equals(LocalDate.class)) {
                    String textDate = rs.getString(field.getName());
                    field.set(object, LocalDate.parse(textDate));
                } else if (field.getType().equals(YearMonth.class)) {
                    String textMonth = rs.getString(field.getName());
                    field.set(object, YearMonth.parse(textMonth));
                } else if (field.getType().equals(LocalDateTime.class)) {
                    LocalDateTime value =
                            Optional.ofNullable(rs.getTimestamp(entry.getKey()))
                                    .map(Timestamp::toLocalDateTime)
                                    .orElse(null);
                    field.set(object, value);
                } else if (field.getType().equals(Instant.class)) {
                    Instant value =
                            Optional.ofNullable(rs.getTimestamp(entry.getKey()))
                                    .map(Timestamp::toInstant)
                                    .orElse(null);
                    field.set(object, value);
                }
            } catch (IllegalAccessException
                    | SQLException
                    | InvocationTargetException
                    | InstantiationException
                    | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        log.debug("returning entity : " + toString(object));

        return object;
    }

    private void addChild(ResultSet rs, T object, Field field)
            throws InstantiationException, IllegalAccessException, InvocationTargetException,
            NoSuchMethodException, SQLException {
        Optional<Field> childIdField =
                Arrays.stream(field.getType().getDeclaredFields())
                        .filter(f -> f.isAnnotationPresent(Id.class))
                        .findFirst();
        if (childIdField.isPresent()) {
            Field childField = childIdField.get();
            Object childObject = field.getType().getDeclaredConstructor().newInstance();
            childField.setAccessible(true);
            String fieldName = field.getAnnotation(Column.class).name();
            if (fieldName.equals("")) {
                fieldName = field.getName();
            }
            if (childField.getType().equals(Integer.class)) {
                childField.set(childObject, rs.getInt(fieldName));
            } else if (childField.getType().equals(String.class)) {
                childField.set(childIdField, rs.getString(childField.getName()));
            }
            field.set(object, childObject);
        }
    }

    public <S> String toString(S entity) {
        if (entity == null) {
            return "";
        }
        StringBuilder s = new StringBuilder("[");
        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                if (field.getType().isAnnotationPresent(Entity.class)) {
                    s.append(field.getName()).append(" : ").append(toString(field.get(entity))).append(", ");
                } else {
                    s.append(field.getName()).append(" : ").append(field.get(entity)).append(", ");
                }
            } catch (IllegalAccessException e) {
                log.error("error reading value : " + e.getMessage());
            }
        }
        s.append("]");

        return s.toString();
    }
}
