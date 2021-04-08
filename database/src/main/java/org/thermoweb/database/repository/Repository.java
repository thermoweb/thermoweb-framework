package org.thermoweb.database.repository;

import org.thermoweb.database.annotations.Column;
import org.thermoweb.database.annotations.Entity;
import org.thermoweb.database.annotations.Id;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Repository<T> {
    private static final int DEFAULT_TIEMOUT = 30;

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
                columnMap.put(field.getAnnotation(Column.class).name(), field);
            }
            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
            }
        }
    }

    public List<T> findAll() {
        return findByQuery(String.format("select * from %s", tableName));
    }

    public Optional<T> findById(Integer id) {
        try (Connection connection = Repository.getConnection()) {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(DEFAULT_TIEMOUT);

            ResultSet rs = statement.executeQuery(
                    String.format("select * from %s where %s = %d", tableName, idField.getName(), id)
            );

            if (rs.next()) {
                return Optional.ofNullable(createDto(rs));
            }

            return Optional.empty();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return Optional.empty();
    }

    protected List<T> findByQuery(String query) {
        try (Connection connection = Repository.getConnection()) {
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(DEFAULT_TIEMOUT);

            ResultSet rs = statement.executeQuery(query);

            List<T> resultList = new ArrayList<>();
            while (rs.next()) {
                resultList.add(createDto(rs));
            }

            return resultList;
        } catch (SQLException throwable) {
            throwable.printStackTrace();
        }

        return Collections.emptyList();
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:sample.db");
    }

    private T createDto(ResultSet rs) {
        T object = null;
        try {
            object = (T) typeArgument.getDeclaredConstructor(null).newInstance();
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
                }
            } catch (IllegalAccessException | SQLException | InvocationTargetException |
                    InstantiationException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return object;
    }

    private void addChild(ResultSet rs, T object, Field field) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, SQLException {
        Field childIdField = Arrays.stream(field.getType().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseGet(null);
        if (childIdField!= null) {
            Object childObject = field.getType().getDeclaredConstructor(null).newInstance();
            childIdField.setAccessible(true);
            if (childIdField.getType().equals(Integer.class)) {
                childIdField.set(childObject, rs.getInt(field.getName()));
            } else if (childIdField.getType().equals(String.class)) {
                childIdField.set(childIdField, rs.getString(childIdField.getName()));
            }
            field.set(object, childObject);
        }
    }
}
