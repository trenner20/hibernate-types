package com.vladmihalcea.hibernate.type.json.generic;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import com.vladmihalcea.hibernate.util.AbstractPostgreSQLIntegrationTest;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import jakarta.persistence.*;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class GenericPostgreSQLJsonStringTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Book()
                    .setIsbn("978-9730228236")
                    .setProperties(
                        "{" +
                            "   \"title\": \"High-Performance Java Persistence\"," +
                            "   \"author\": \"Vlad Mihalcea\"," +
                            "   \"publisher\": \"Amazon\"," +
                            "   \"price\": 44.99" +
                            "}"
                    )
            );
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = entityManager.unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            QueryCountHolder.clear();

            book.setProperties(
                "{" +
                    "   \"title\": \"High-Performance Java Persistence\"," +
                    "   \"author\": \"Vlad Mihalcea\"," +
                    "   \"publisher\": \"Amazon\"," +
                    "   \"price\": 44.99," +
                    "   \"url\": \"https://www.amazon.com/High-Performance-Java-Persistence-Vlad-Mihalcea/dp/973022823X/\"" +
                    "}"
            );
        });

        doInJPA(entityManager -> {
            JsonNode properties = (JsonNode) entityManager
                .createNativeQuery(
                    "SELECT " +
                    "  properties AS properties " +
                    "FROM book " +
                    "WHERE " +
                    "  isbn = :isbn")
                .setParameter("isbn", "978-9730228236")
                .unwrap(NativeQuery.class)
                .addScalar("properties", new JsonType(JsonNode.class))
                .getSingleResult();

            assertEquals("High-Performance Java Persistence", properties.get("title").asText());
        });
    }

    @Test
    public void testNullValue() {
        doInJPA(entityManager -> {
            Book book = entityManager.unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            QueryCountHolder.clear();

            book.setProperties(null);
        });

        doInJPA(entityManager -> {
            Book book = entityManager.unwrap(Session.class)
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            assertNull(book.getProperties());
        });
    }

    @Test
    public void testLoad() {
        QueryCountHolder.clear();

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            Book book = session
                .bySimpleNaturalId(Book.class)
                .load("978-9730228236");

            assertTrue(book.getProperties().contains("\"price\": 44.99"));
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @TypeDef(name = "json", typeClass = JsonType.class)
    public static class Book {

        @Id
        @GeneratedValue
        private Long id;

        @NaturalId
        private String isbn;

        @Type(type = "json")
        @Column(columnDefinition = "jsonb")
        private String properties;

        public String getIsbn() {
            return isbn;
        }

        public Book setIsbn(String isbn) {
            this.isbn = isbn;
            return this;
        }

        public String getProperties() {
            return properties;
        }

        public Book setProperties(String properties) {
            this.properties = properties;
            return this;
        }
    }
}