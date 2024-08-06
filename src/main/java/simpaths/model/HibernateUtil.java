package simpaths.model;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Map;


/**
 *
 * CLASS TO MANAGE HIBERNATE SESSION FACTORY
 *
 */
public class HibernateUtil {

    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;
    private EntityManagerFactory entityManagerFactory;


    // new database tables
    public HibernateUtil(String persistenceUnitName) {
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // existing database tables
    public HibernateUtil(String persistenceUnitName, Map propertyMap) {
        try {
            entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, propertyMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {return entityManagerFactory;}

    public SessionFactory getSessionFactory() {

        if (sessionFactory == null) {

            try {
                registry = new StandardServiceRegistryBuilder().configure().build();
                MetadataSources sources = new MetadataSources(registry);
                Metadata metadata = sources.getMetadataBuilder().build();
                sessionFactory = metadata.getSessionFactoryBuilder().build();
            } catch (Exception e) {
                e.printStackTrace();
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
        }
        return sessionFactory;
    }

    public void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }
}