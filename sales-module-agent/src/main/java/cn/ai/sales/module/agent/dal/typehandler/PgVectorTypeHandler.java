package cn.ai.sales.module.agent.dal.typehandler;

import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgVectorTypeHandler extends BaseTypeHandler<List<Double>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Double> parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject vector = new PGobject();
        vector.setType("vector");
        vector.setValue(toVectorText(parameter));
        ps.setObject(i, vector);
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVectorText(rs.getString(columnName));
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVectorText(rs.getString(columnIndex));
    }

    @Override
    public List<Double> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVectorText(cs.getString(columnIndex));
    }

    private String toVectorText(List<Double> values) {
        return "[" + values.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    private List<Double> parseVectorText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String normalized = StrUtil.removeSuffix(StrUtil.removePrefix(text.trim(), "["), "]");
        if (StrUtil.isBlank(normalized)) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        for (String part : normalized.split(",")) {
            if (StrUtil.isNotBlank(part)) {
                values.add(Double.valueOf(part.trim()));
            }
        }
        return values;
    }
}
