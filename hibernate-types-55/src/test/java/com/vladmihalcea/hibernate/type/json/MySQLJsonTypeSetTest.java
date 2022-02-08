package com.vladmihalcea.hibernate.type.json;

import com.vladmihalcea.hibernate.type.model.BaseEntity;
import com.vladmihalcea.hibernate.util.AbstractMySQLIntegrationTest;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author Sergei Poznanski
 */
public class MySQLJsonTypeSetTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
              User.class
        };
    }

    private User _user;

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            User user = new User();

            user.setId(1L);
            user.setPhones(new HashSet<>(asList("7654321", "1234567")));
            user.setRoles(EnumSet.of(Role.ADMIN, Role.USER));
            user.setChildren(new HashSet<>(asList(
                  new Child("Jane", 2, new HashSet<>(asList("toy1", "toy2"))),
                  new Child("John", 1, new HashSet<>(asList("toy3", "toy4")))
            )));

            entityManager.persist(user);
            _user = user;
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            User user = entityManager.find(User.class, _user.getId());

            QueryCountHolder.clear();

            user.setPhones(new HashSet<>(asList("1592637", "9518473")));
            user.setRoles(EnumSet.of(Role.USER, Role.DEV));
            user.setChildren(new HashSet<>(asList(
                  new Child("Jinny", 1, new HashSet<>(asList("toy5", "toy6"))),
                  new Child("Jenny", 2, new HashSet<>(asList("toy7", "toy8")))
            )));
        });

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getTotal());
        assertEquals(1, queryCount.getUpdate());

        doInJPA(entityManager -> {
            User user = entityManager.find(User.class, _user.getId());
            assertEquals(new HashSet<>(asList("9518473", "1592637")), user.getPhones());
            assertEquals(EnumSet.of(Role.DEV, Role.USER), user.getRoles());
            assertEquals(new HashSet<>(asList(
                  new Child("Jenny", 2, new HashSet<>(asList("toy8", "toy7"))),
                  new Child("Jinny", 1, new HashSet<>(asList("toy6", "toy5")))
            )), user.getChildren());
            assertEquals(Integer.valueOf(1), user.getVersion());
        });
    }

    @Test
    public void testLoad() {
        QueryCountHolder.clear();

        doInJPA(entityManager -> {
            User user = entityManager.find(User.class, _user.getId());
            assertEquals(new HashSet<>(asList("1234567", "7654321")), user.getPhones());
            assertEquals(EnumSet.of(Role.USER, Role.ADMIN), user.getRoles());
            assertEquals(new HashSet<>(asList(
                new Child("John", 1, new HashSet<>(asList("toy4", "toy3"))),
                new Child("Jane", 2, new HashSet<>(asList("toy2", "toy1")))
            )), user.getChildren());
            assertEquals(Integer.valueOf(0), user.getVersion());
        });

        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        assertEquals(1, queryCount.getTotal());
        assertEquals(1, queryCount.getSelect());
        assertEquals(0, queryCount.getUpdate());
    }

    @Entity
    @Table(name = "users")
    @DynamicUpdate
    public static class User extends BaseEntity {

        @Type(type = "json")
        @Column(nullable = false, columnDefinition = "json")
        private Set<String> phones;

        @Type(type = "json")
        @Column(nullable = false, columnDefinition = "json")
        private EnumSet<Role> roles;

        @Type(type = "json")
        @Column(nullable = false, columnDefinition = "json")
        private Set<Child> children;

        public Set<String> getPhones() {
            return phones;
        }

        public void setPhones(Set<String> phones) {
            this.phones = phones;
        }

        public EnumSet<Role> getRoles() {
            return roles;
        }

        public void setRoles(EnumSet<Role> roles) {
            this.roles = roles;
        }

        public Set<Child> getChildren() {
            return children;
        }

        public void setChildren(final Set<Child> children) {
            this.children = children;
        }
    }

    public enum Role {
        USER, ADMIN, DEV
    }

    public static class Child implements Serializable {

        private final String name;
        private final Integer age;
        private final Set<String> toys;

        @ConstructorProperties({"name", "age", "toys"})
        public Child(String name, Integer age, final Set<String> toys) {
            this.name = Objects.requireNonNull(name);
            this.age = Objects.requireNonNull(age);
            this.toys = Objects.requireNonNull(toys);
        }

        public String getName() {
            return name;
        }

        public Integer getAge() {
            return age;
        }

        public Set<String> getToys() {
            return toys;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Child child = (Child) o;
            if (!name.equals(child.name)) return false;
            if (!age.equals(child.age)) return false;
            return toys.equals(child.toys);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + age.hashCode();
            result = 31 * result + toys.hashCode();
            return result;
        }
    }
}
