package com.vladmihalcea.hibernate.type.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import com.vladmihalcea.hibernate.type.model.BaseEntity;
import com.vladmihalcea.hibernate.type.model.Location;
import com.vladmihalcea.hibernate.type.model.Ticket;
import com.vladmihalcea.hibernate.util.AbstractMySQLIntegrationTest;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class EhcacheMySQLJsonBinaryTypeTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Event.class,
        };
    }

    @Override
    protected String[] packages() {
        return new String[]{
            Event.class.getPackage().getName()
        };
    }

    protected void additionalProperties(Properties properties) {
        properties.setProperty("hibernate.cache.use_second_level_cache", "true");
        properties.setProperty("hibernate.cache.use_query_cache", "true");
        properties.setProperty("hibernate.cache.region.factory_class", "ehcache");
    }

    private Event _event;

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Event nullEvent = new Event();
            nullEvent.setId(0L);
            entityManager.persist(nullEvent);

            Location location = new Location();
            location.setCountry("Romania");
            location.setCity("Cluj-Napoca");

            Event event = new Event();
            event.setId(1L);

            event.setProperties(
                JacksonUtil.toJsonNode(
                    "{" +
                        "   \"title\": \"High-Performance Java Persistence\"," +
                        "   \"author\": \"Vlad Mihalcea\"," +
                        "   \"publisher\": \"Amazon\"," +
                        "   \"price\": 44.99" +
                        "}"
                )
            );
            entityManager.persist(event);

            Ticket ticket = new Ticket();
            ticket.setPrice(12.34d);
            ticket.setRegistrationCode("ABC123");

            _event = event;
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            QueryCountHolder.clear();

            Event event = entityManager.find(Event.class, _event.getId());
            assertNotNull(event.getProperties());

            QueryCount queryCount = QueryCountHolder.getGrandTotal();
            assertEquals(0, queryCount.getTotal());

            List<String> properties = entityManager.createNativeQuery(
                "select CAST(e.properties AS CHAR(1000)) " +
                "from event e " +
                "where JSON_EXTRACT(e.properties, \"$.price\") > 1 ")
            .getResultList();

            assertEquals(1, properties.size());
            JsonNode jsonNode = JacksonUtil.toJsonNode(properties.get(0));
            assertEquals("High-Performance Java Persistence", jsonNode.get("title").asText());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    @Cacheable(true)
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Event extends BaseEntity {

        @Type(type = "json-node")
        @Column(columnDefinition = "json")
        private JsonNode properties;

        public JsonNode getProperties() {
            return properties;
        }

        public void setProperties(JsonNode properties) {
            this.properties = properties;
        }
    }
}
