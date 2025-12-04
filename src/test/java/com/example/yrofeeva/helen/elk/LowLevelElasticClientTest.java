package com.example.yrofeeva.helen.elk;

import com.example.yrofeeva.helen.elk.client.ElasticClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LowLevelElasticClientTest extends AbstractElasticClientTest {

    @Autowired
    public LowLevelElasticClientTest(ElasticClient lowLevelElasticClient) {
        super(lowLevelElasticClient);
    }
}
