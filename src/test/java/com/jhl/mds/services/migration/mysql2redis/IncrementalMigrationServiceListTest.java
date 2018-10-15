package com.jhl.mds.services.migration.mysql2redis;

import com.jhl.mds.BaseTest;
import com.jhl.mds.TableTemplate;
import com.jhl.mds.consts.RedisKeyType;
import com.jhl.mds.dto.MySQLServerDTO;
import com.jhl.mds.dto.RedisServerDTO;
import com.jhl.mds.dto.SimpleFieldMappingDTO;
import com.jhl.mds.dto.TableInfoDTO;
import com.jhl.mds.dto.migration.MySQL2RedisMigrationDTO;
import com.jhl.mds.services.redis.RedisConnectionPool;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Arrays;

public class IncrementalMigrationServiceListTest extends BaseTest {

    @Autowired
    private IncrementalMigrationService incrementalMigrationService;

    @Autowired
    private RedisConnectionPool redisConnectionPool;

    @Test
    public void insertTest() throws Exception {
        String sourceTable = prepareTable(TableTemplate.TEMPLATE_SIMPLE);

        MySQLServerDTO serverDTO = new MySQLServerDTO(0, "test", "localhost", "3307", "root", "root");

        String keyPrefix = "key_name_" + rand.nextInt(70000) + "_";

        RedisServerDTO redisServerDTO = new RedisServerDTO(0, "", "localhost", "6379", "", "");
        MySQL2RedisMigrationDTO dto = MySQL2RedisMigrationDTO.builder()
                .taskId(randomTaskId())
                .source(new TableInfoDTO(serverDTO, "mds", sourceTable))
                .target(redisServerDTO)
                .redisKeyType(RedisKeyType.LIST)
                .mapping(Arrays.asList(
                        new SimpleFieldMappingDTO("'" + keyPrefix + "'", "key"),
                        new SimpleFieldMappingDTO("json(_row)", "value")
                ))
                .build();

        incrementalMigrationService.run(dto);

        Thread.sleep(500);

        for (int i = 0; i < 100; i++) {
            getStatement().execute("INSERT INTO mds." + sourceTable + "(`random_number`) VALUES (1)");
        }

        Thread.sleep(3000);

        Jedis jedis = redisConnectionPool.getConnection(redisServerDTO);

        Assert.assertEquals(100L, jedis.llen(keyPrefix).longValue());

        jedis.flushAll();
    }
}
