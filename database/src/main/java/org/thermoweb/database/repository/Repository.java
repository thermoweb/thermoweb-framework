package org.thermoweb.database.repository;

import org.thermoweb.database.annotations.Column;
import org.thermoweb.database.annotations.Entity;
import org.thermoweb.database.annotations.Id;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class Repository<T> {
    private static final int DEFAULT_TIMEOUT = 30;
    private static final String INSERT_STATEMENT = "INSERT INTO %s (%s) VALUES (%s);";

    private static final String postgreTimestampPattern = "YYYY-MM-dd HH24:MI:ss";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    ;

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
                    String.format("Class %s does not have Entity annotation", typeArgument.getName())
            );
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
        Field[] fields = dto.getClass().getDeclaredFields();
        StringJoiner columnJoiner = new StringJoiner(", ");
        StringJoiner valuesJoiner = new StringJoiner(", ");
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                if (field.getType().isAnnotationPresent(Entity.class)) {
                    // FIXME: handle child entity save
                    Field childIdField = Arrays.stream(field.getType().getDeclaredFields())
                            .filter(f -> f.isAnnotationPresent(Id.class))
                            .findFirst()
                            .orElseThrow();
                    childIdField.setAccessible(true);
                    field.setAccessible(true);
                    Integer value = (Integer) childIdField.get(field.get(dto));
                    valuesJoiner.add(String.format("%d", value));
                    columnJoiner.add(field.getAnnotation(Column.class).name());
                } else {
                    field.setAccessible(true);
                    Object value = field.get(dto);
                    field.setAccessible(false);

                    if (value != null) {
                        columnJoiner.add(field.getAnnotation(Column.class).name());
                        if (field.getType().equals(String.class)) {
                            valuesJoiner.add(String.format("'%s'", value));
                        } else if (field.getType().equals(LocalDateTime.class)) {
                            valuesJoiner.add(String.format("TO_TIMESTAMP('%s', '%s')",
                                    formatter.format((LocalDateTime) value),
                                    postgreTimestampPattern));
                        }
                    }
                }
            }
        }

        try (Connection connection = ConnectionManager.getConnection()) {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(DEFAULT_TIMEOUT);
            String query = String.format(INSERT_STATEMENT, tableName, columnJoiner, valuesJoiner);
            statement.execute(query, Statement.RETURN_GENERATED_KEYS);

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return findById(generatedKeys.getInt(this.idField.getName())).orElseThrow();
                }
            }
        }

        throw new SQLException("failed to retrieve or create user");
    }

    public List<T> findAll() {
        return findByQuery(String.format("select * from %s", tableName));
    }

    public Optional<T> findById(Integer id) {
        try (Connection connection = ConnectionManager.getConnection()) {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(DEFAULT_TIMEOUT);

            ResultSet rs = statement.executeQuery(
                    String.format("select * from %s where %s = %d", tableName, idField.getName(), id)
            );

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
        } catch (InstantiationException | NoSuchMethodException |
                InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
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
                    LocalDateTime value = Optional.ofNullable(rs.getTimestamp(entry.getKey()))
                            .map(Timestamp::toLocalDateTime)
                            .orElse(null);
                    field.set(object, value);
                }
            } catch (IllegalAccessException | SQLException | InvocationTargetException |
                    InstantiationException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return object;
    }

    private void addChild(ResultSet rs, T object, Field field) throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException, SQLException {
        Optional<Field> childIdField = Arrays.stream(field.getType().getDeclaredFields())
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
}
