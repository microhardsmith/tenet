package cn.zorcc.common.util;

import cn.zorcc.common.Constants;
import cn.zorcc.common.enums.ExceptionType;
import cn.zorcc.common.exception.FrameworkException;

import java.util.List;

public final class SqlUtil {
    private SqlUtil() {
        throw new UnsupportedOperationException();
    }

    public static StringBuilder values(StringBuilder sb, int count) {
        sb.append(" values ");
        return params(sb, count);
    }

    public static StringBuilder params(StringBuilder sb, int count, int startIndex) {
        if(count > 0) {
            sb.append(Constants.L_BRACKET);
            for(int i = startIndex; i < count + startIndex; i++) {
                sb.append(Constants.SIGN_CHAR).append(i).append(i == count + startIndex - 1 ? Constants.R_BRACKET : Constants.COMMA_CHAR);
            }
            return sb;
        }else {
            throw new FrameworkException(ExceptionType.SQL, Constants.UNREACHED);
        }
    }

    public static StringBuilder params(StringBuilder sb, int count) {
        return params(sb, count, 1);
    }


    /**
     *  Pattern : insert into table_name(a,b,c)
     */
    public static StringBuilder insertInto(StringBuilder sb, String tableName, List<String> databaseColumns) {
        sb.append("insert into ").append(tableName);
        String databaseColumnString = String.join(",", databaseColumns);
        sb.append(Constants.L_BRACKET).append(databaseColumnString).append(Constants.R_BRACKET);
        return sb;
    }
}
