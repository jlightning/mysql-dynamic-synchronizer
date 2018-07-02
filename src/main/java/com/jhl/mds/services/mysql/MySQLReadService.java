package com.jhl.mds.services.mysql;

import com.jhl.mds.dto.MySQLFieldDTO;
import com.jhl.mds.dto.TableInfoDTO;
import com.jhl.mds.util.MySQLStringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class MySQLReadService {

    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private MySQLConnectionPool mySQLConnectionPool;
    private MySQLDescribeService mySQLDescribeService;

    @Autowired
    public MySQLReadService(MySQLConnectionPool mySQLConnectionPool, MySQLDescribeService mySQLDescribeService) {
        this.mySQLConnectionPool = mySQLConnectionPool;
        this.mySQLDescribeService = mySQLDescribeService;
    }

    public Future<?> async(TableInfoDTO tableInfo, List<String> columns, ResultCallback resultCallback) {
        return executor.submit(() -> {
            try {
                run(tableInfo, columns, resultCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void run(TableInfoDTO tableInfo, List<String> columns, ResultCallback resultCallback) throws SQLException {
        List<MySQLFieldDTO> fields = mySQLDescribeService.getFields(tableInfo.getServer(), tableInfo.getDatabase(), tableInfo.getTable());
        columns = fields.stream().map(MySQLFieldDTO::getField).collect(Collectors.toList());

        Connection conn = mySQLConnectionPool.getConnection(tableInfo.getServer());
        Statement st = conn.createStatement();

        String sql = String.format("SELECT %s FROM %s;", MySQLStringUtil.columnListToString(columns), tableInfo.getDatabase() + "." + tableInfo.getTable());
        ResultSet result = st.executeQuery(sql);

        while (result.next()) {
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                String column = columns.get(i);
                data.put(column, result.getObject(i + 1));
            }

            resultCallback.send(data);
        }
    }

    public interface ResultCallback {
        void send(Map<String, Object> result);
    }
}
