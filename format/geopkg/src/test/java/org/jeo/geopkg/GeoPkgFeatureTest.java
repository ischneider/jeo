package org.jeo.geopkg;

import static org.jeo.Tests.unzip;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jeo.data.Cursor;
import org.jeo.data.Query;
import org.jeo.feature.BasicFeature;
import org.jeo.feature.Feature;
import org.jeo.feature.Features;
import org.jeo.feature.Schema;
import org.jeo.feature.SchemaBuilder;
import org.jeo.geom.Geom;
import org.jeo.geopkg.Entry.DataType;
import org.jeo.sql.DbOP;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

public class GeoPkgFeatureTest extends GeoPkgTestSupport {

    GeoPkgWorkspace geopkg;

    @Before
    public void setUp() throws Exception {
        File dir = unzip(getClass().getResourceAsStream("states.geopackage.zip"), newTmpDir());
        geopkg = GeoPackage.open(new File(dir, "states.geopackage"));
    }

    @After
    public void tearDown() throws Exception {
        geopkg.close();
        FileUtils.deleteQuietly(geopkg.getFile().getParentFile());
    }

    @Test
    public void testFeatureEntry() throws Exception {
        List<FeatureEntry> entries = geopkg.features();
        assertEquals(1, entries.size());

        FeatureEntry entry = entries.get(0);
        assertEquals("states", entry.getTableName());
    }

    @Test
    public void testCreateSchema() throws Exception {
        FeatureEntry entry = geopkg.feature("states");
        assertNotNull(entry);

        Schema schema = geopkg.createSchema(entry);
        assertEquals("states", schema.getName());

        assertNotNull(schema.geometry());
        assertEquals(MultiPolygon.class, schema.geometry().getType());

        assertNotNull(schema.field("STATE_NAME"));
    }

    @Test
    public void testCount() throws Exception {
        FeatureEntry entry = geopkg.feature("states");
        assertEquals(49, geopkg.count(entry, new Query()));
    }

    @Test
    public void testRead() throws Exception {
        FeatureEntry entry = geopkg.feature("states");
        Cursor<Feature> c = geopkg.cursor(entry, new Query());
        
        assertNotNull(c);
        for (int i = 0; i < 49; i++) {
            assertTrue(c.hasNext());

            Feature f = c.next();
            assertNotNull(f);

            assertTrue(f.geometry() instanceof MultiPolygon);
            assertNotNull(f.get("STATE_NAME"));
        }

        assertFalse(c.hasNext());
        assertNull(c.next());
        c.close();
    }

    @Test
    public void testReadWithFilter() throws Exception {
        FeatureEntry entry = geopkg.feature("states");
        Cursor<Feature> c = geopkg.cursor(entry, new Query().filter("STATE_NAME = 'Texas'"));
        
        assertNotNull(c);
        assertTrue(c.hasNext());
        assertNotNull(c.next());
        assertFalse(c.hasNext());
        c.close();
    }

    @Test
    public void testAdd() throws Exception {
        FeatureEntry entry = geopkg.feature("states");
        Schema schema = geopkg.schema(entry);

        Geometry g = Geom.point(0,0).buffer(1);
        Feature f = new BasicFeature(null, schema);
        f.put(schema.geometry().getName(), g);
        f.put("STATE_NAME", "JEOLAND");
        geopkg.add(entry, f);

        assertEquals(50, geopkg.count(entry, new Query()));

        Cursor<Feature> c = geopkg.cursor(entry, new Query().bounds(g.getEnvelopeInternal()));
        assertTrue(c.hasNext());

        assertEquals("JEOLAND", c.next().get("STATE_NAME"));
        c.close();
    }

    @Test
    public void testCreate() throws Exception {
        Schema schema = new SchemaBuilder("widgets").field("geometry", Point.class)
            .field("name", String.class).field("cost", Double.class).schema();

        FeatureEntry entry = new FeatureEntry();
        entry.setSrid(4326);
        entry.setBounds(new Envelope(-180, 180, -90, 90));
        geopkg.create(entry, schema);

        geopkg.add(entry, Features.create(null, schema, Geom.point(1,2), "anvil", 10.99));

        Cursor<Feature> c = geopkg.cursor(entry, new Query());
        try {
            assertTrue(c.hasNext());
    
            Feature f = c.next();
            assertEquals(((Point)f.geometry()).getX(), 1f, 0.1);
            assertEquals(((Point)f.geometry()).getY(), 2f, 0.1);
            assertEquals("anvil", f.get("name"));
            assertEquals(10.99, ((Number)f.get("cost")).doubleValue(), 0.1);
        }
        finally {
            c.close();
        }

        //test re-loading the entry
        entry = geopkg.feature("widgets");
        assertNotNull(entry);

        assertEquals(Geom.Type.POINT, entry.getGeometryType());

        geopkg.run(new DbOP<Object>() {
            @Override
            protected Object doRun(Connection cx) throws Exception {
                ResultSet rs = open(open(cx.createStatement()).executeQuery(
                    "SELECT data_type FROM geopackage_contents WHERE table_name = 'widgets'"));
                
                assertTrue(rs.next());
                assertEquals(DataType.Feature.value(), rs.getString(1));
                return null;
            }
        });
    }
}
