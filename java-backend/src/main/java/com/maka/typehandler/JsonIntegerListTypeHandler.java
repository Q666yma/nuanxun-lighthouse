package com.maka.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.Collections;
import java.util.List;

public class JsonIntegerListTypeHandler extends BaseTypeHandler<List<Integer>> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, mapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("List<Integer> 转 JSON 字符串失败", e);
        }
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return mapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return mapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return mapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}