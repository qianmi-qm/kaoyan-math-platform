package com.kaoyan.platform.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 防止 mapper XML 出现非法 XML（例如注释里出现 "--"）导致应用启动失败。
 * 纯算法单测不启动 Spring 容器，所以这里用标准 XML 解析器单独守一道门。
 */
class MapperXmlWellFormedTest {

    @Test
    @DisplayName("所有 mapper XML 都能被标准 XML 解析器解析")
    void allMapperXmlAreWellFormed() throws Exception {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml");
        assertTrue(resources.length > 0, "没有找到 mapper XML");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                factory.newDocumentBuilder().parse(in);
            }
        }
    }
}
