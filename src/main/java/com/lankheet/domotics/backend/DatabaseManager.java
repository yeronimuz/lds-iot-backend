/**
 * MIT License
 * 
 * Copyright (c) 2017 Lankheet Software and System Solutions
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.lankheet.domotics.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lankheet.domotics.backend.config.DatabaseConfig;
import com.lankheet.domotics.backend.dao.DaoListener;
import com.lankheet.iot.datatypes.entities.Measurement;
import com.lankheet.iot.datatypes.entities.Sensor;
import io.dropwizard.lifecycle.Managed;

/**
 * The database manager saves the received measurements in the data store
 *
 */
public class DatabaseManager implements Managed, DaoListener {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String PERSISTENCE_UNIT = "meas-pu";

    private static final int MEASUREMENTS_PER_24HOUR = 6 * 60 * 60 * 24;
    private DatabaseConfig dbConfig;
    private EntityManagerFactory emf;
    private EntityManager em;

    public DatabaseManager(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public void start() throws Exception {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("javax.persistence.jdbc.driver", dbConfig.getDriver());
        properties.put("javax.persistence.jdbc.url", dbConfig.getUrl());
        properties.put("javax.persistence.jdbc.user", dbConfig.getUserName());
        properties.put("javax.persistence.jdbc.password", dbConfig.getPassword());

        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
        em = emf.createEntityManager();
    }

    @Override
    public void stop() throws Exception {
        em.close();
        emf.close();
    }

    @Override
    public void saveNewMeasurement(Measurement measurement) {
        List<Sensor> sensorList;
        Sensor sensor = measurement.getSensor();
        String query = "SELECT s FROM sensors s WHERE s.macAddress = :mac AND s.sensorType = :type";
        sensorList = em.createQuery(query)
                .setParameter("mac", sensor.getMacAddress())
                .setParameter("type", sensor.getType())
                .getResultList();
        LOG.debug("Sensors in db: {}", sensorList.size());
        LOG.info("Storing: " + measurement);
        em.getTransaction().begin();
        if (!sensorList.isEmpty()) {
            sensor = sensorList.get(0);
            measurement.setSensor(sensor);
        } else {
            em.persist(sensor);
        }

        // TODO: Set reference when sensor already exists;
        em.persist(measurement);
        em.getTransaction().commit();
    }

    @Override
    public List<Measurement> getMeasurementsBySensor(int sensorId) {
        List<Measurement> measurementsList = null;
        String query = "SELECT e, e.sensor.id FROM measurements e WHERE e.sensor.id = " + sensorId + " ORDER BY e.id ASC";
        measurementsList = em.createQuery(query).setMaxResults(MEASUREMENTS_PER_24HOUR).getResultList();
        return measurementsList;
    }

    @Override
    public List<Measurement> getMeasurementsBySensorAndType(int sensorId, int type) {
        return em.createQuery("SELECT e FROM measurements e WHERE e.id = " + sensorId + " AND e.type = " + type)
                .getResultList();
    }
}
