/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.soul.spring.boot.starter.sync.data.http;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.dromara.soul.sync.data.api.PluginDataSubscriber;
import org.dromara.soul.sync.data.http.HttpSyncDataService;
import org.dromara.soul.sync.data.http.config.HttpConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.test.context.junit4.SpringRunner;
import wiremock.org.apache.http.HttpHeaders;
import wiremock.org.apache.http.entity.ContentType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Test cases for {@link HttpSyncDataConfiguration}.
 *
 * @author strawberry-crisis
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                HttpSyncDataConfiguration.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "soul.sync.http.url=http://localhost:18848",
                "soul.sync.http.delayTime=3",
                "soul.sync.http.connectionTimeout=5"
        })
@EnableAutoConfiguration
@MockBean({PluginDataSubscriber.class, ServletWebServerFactory.class})
public class HttpClientPluginConfigurationTest {

    @Autowired
    private HttpConfig httpConfig;

    @Autowired
    private HttpSyncDataService httpSyncDataService;

    @BeforeClass
    public static void setupWireMock() throws Exception {
        WireMockServer wireMockServer = new WireMockServer(options().port(18848));

        wireMockServer.stubFor(get(urlPathEqualTo("/configs/fetch"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(mockConfigsFetchResponseJson())
                        .withStatus(200))
        );
        wireMockServer.stubFor(post(urlPathEqualTo("/configs/listener"))
                .willReturn(aResponse()
                        .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                        .withBody(mockConfigsListenResponseJson())
                        .withStatus(200))
        );

        wireMockServer.start();
    }

    @Test
    public void testHttpSyncDataService() {
        Assert.assertNotNull(httpSyncDataService);
    }

    @Test
    public void testHttpConfig() {
        Assert.assertEquals("http://localhost:18848", httpConfig.getUrl());
        Assert.assertEquals(Integer.valueOf(3), httpConfig.getDelayTime());
        Assert.assertEquals(Integer.valueOf(5), httpConfig.getConnectionTimeout());
    }

    // mock configs listen api response
    private static String mockConfigsListenResponseJson() {
        return "{\"code\":200,\"message\":\"success\",\"data\":[\"PLUGIN\"]}";
    }

    // mock configs fetch api response
    private static String mockConfigsFetchResponseJson() throws Exception {
        return new String(Files.readAllBytes(
                Paths.get(Objects.requireNonNull(HttpClientPluginConfigurationTest.class.getClassLoader()
                        .getResource("mock_configs_fetch_response.json")).toURI())));
    }
}
