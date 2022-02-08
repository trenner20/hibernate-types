package com.vladmihalcea.hibernate.type.search;

import com.vladmihalcea.hibernate.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 * @author Philip Riecks
 */
public class PostgreSQLTSVectorTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Book.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Book book = new Book();
            book.setId(1L);
            book.setIsbn("978-9730228236");
            book.setFts(
                "This book is a journey into Java data access performance tuning. From connection management, to batch" +
                    " updates, fetch sizes and concurrency control mechanisms, it unravels the inner workings of" +
                    " the most common Java data access frameworks."
            );

            entityManager.persist(book);
        });

        doInJPA(entityManager -> {
            Book book = entityManager.find(Book.class, 1L);

            assertTrue(book.getFts().contains("Java"));
            assertTrue(book.getFts().contains("concurrency"));
            assertTrue(book.getFts().contains("book"));
        });
    }

    @Entity(name = "Book")
    @Table(name = "book")
    @TypeDef(name = "tsvector", typeClass = PostgreSQLTSVectorType.class)
    public static class Book {

        @Id
        private Long id;

        @NaturalId
        private String isbn;

        @Type(type = "tsvector")
        @Column(columnDefinition = "tsvector")
        private String fts;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getIsbn() {
            return isbn;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public String getFts() {
            return fts;
        }

        public void setFts(String fts) {
            this.fts = fts;
        }
    }
}
