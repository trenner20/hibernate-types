package com.vladmihalcea.hibernate.type.json;

import com.vladmihalcea.hibernate.type.json.internal.JacksonUtil;
import com.vladmihalcea.hibernate.type.model.BaseEntity;
import com.vladmihalcea.hibernate.type.model.Location;
import com.vladmihalcea.hibernate.type.model.Ticket;
import com.vladmihalcea.hibernate.util.AbstractPostgreSQLIntegrationTest;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.hibernate.annotations.Type;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLJsonBinaryTypeTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Event.class,
            Participant.class
        };
    }

    private Event _event;

    private Participant _participant;

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
            event.setLocation(location);
            entityManager.persist(event);

            Ticket ticket = new Ticket();
            ticket.setPrice(12.34d);
            ticket.setRegistrationCode("ABC123");

            Participant participant = new Participant();
            participant.setId(1L);
            participant.setTicket(ticket);
            participant.setEvent(event);
            participant.setMetaData(JacksonUtil.toString(location));

            entityManager.persist(participant);

            _event = event;
            _participant = participant;
        });
    }

    @Test
    public void testLoad() {
        QueryCountHolder.clear();

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, _event.getId());
            assertEquals("Romania", event.getLocation().getCountry());
            assertEquals("Cluj-Napoca", event.getLocation().getCity());
        });

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getTotal());
        assertEquals(1, queryCount.getSelect());
        assertEquals(0, queryCount.getUpdate());
    }

    @Test
    public void test() {

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, _event.getId());
            assertEquals("Cluj-Napoca", event.getLocation().getCity());

            Participant participant = entityManager.find(Participant.class, _participant.getId());
            assertEquals("ABC123", participant.getTicket().getRegistrationCode());

            List<String> participants = entityManager.createNativeQuery(
                "select jsonb_pretty(p.ticket) " +
                    "from participant p " +
                    "where p.ticket ->> 'price' > :price")
                .setParameter("price", "10")
                .getResultList();

            List<String> countries = entityManager.createNativeQuery(
                "select p.metadata ->> 'country' " +
                    "from participant p ")
                .getResultList();

            event.getLocation().setCity("Constanța");
            assertEquals(Integer.valueOf(0), event.getVersion());
            entityManager.flush();
            assertEquals(Integer.valueOf(1), event.getVersion());

            assertEquals(1, participants.size());
            assertEquals(1, countries.size());
            assertNotNull(countries.get(0));
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, _event.getId());
            event.getLocation().setCity(null);
            assertEquals(Integer.valueOf(1), event.getVersion());
            entityManager.flush();
            assertEquals(Integer.valueOf(2), event.getVersion());
        });

        doInJPA(entityManager -> {
            Event event = entityManager.find(Event.class, _event.getId());
            event.setLocation(null);
            assertEquals(Integer.valueOf(2), event.getVersion());
            entityManager.flush();
            assertEquals(Integer.valueOf(3), event.getVersion());
        });
    }

    @Entity(name = "Event")
    @Table(name = "event")
    public static class Event extends BaseEntity {

        @Type(type = "jsonb")
        @Column(columnDefinition = "jsonb")
        private Location location;

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }

    @Entity(name = "Participant")
    @Table(name = "participant")
    public static class Participant extends BaseEntity {

        @Type(type = "jsonb")
        @Column(columnDefinition = "jsonb")
        private Ticket ticket;

        @ManyToOne
        private Event event;

        @Type(type = "jsonb")
        @Column(columnDefinition = "jsonb")
        private String metaData;

        public Ticket getTicket() {
            return ticket;
        }

        public void setTicket(Ticket ticket) {
            this.ticket = ticket;
        }

        public Event getEvent() {
            return event;
        }

        public void setEvent(Event event) {
            this.event = event;
        }

        public String getMetaData() {
            return metaData;
        }

        public void setMetaData(String metaData) {
            this.metaData = metaData;
        }
    }
}
