package org.jeo.postgis;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jeo.data.VectorDriver;
import org.jeo.feature.Schema;
import org.jeo.util.Key;
import org.jeo.util.Messages;
import org.jeo.util.Password;

public class PostGIS implements VectorDriver<PostGISWorkspace>{

    /**
     * Database key.
     */
    public static final Key<String> DB = new Key<String>("db", String.class);

    /**
     * Host key, defaults to 'localhost'.
     */
    public static final Key<String> HOST = new Key<String>("host", String.class, "localhost");

    /**
     * Port key, defaults to 5432.
     */
    public static final Key<Integer> PORT = new Key<Integer>("port", Integer.class, 5432);

    /**
     * User key, defaults to current user, obtained via <tt>System.getProperty("user.name")</tt>
     */
    public static final Key<String> USER = 
            new Key<String>("user", String.class, System.getProperty("user.name"));

    /**
     * Password key. 
     */
    public static final Key<Password> PASSWD = new Key<Password>("passwd", Password.class);

    public static PostGISWorkspace open(PostGISOpts opts) throws IOException {
        return new PostGISWorkspace(opts);
    }

    @Override
    public String getName() {
        return "PostGIS";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("pg");
    }

    @Override
    public Class<PostGISWorkspace> getType() {
        return PostGISWorkspace.class;
    }

    @Override
    public List<Key<? extends Object>> getKeys() {
        return (List) Arrays.asList(DB, HOST, PORT, USER, PASSWD);
    }

    @Override
    public boolean canOpen(Map<?, Object> opts, Messages msgs) {
        if (!DB.has(opts)) {
            Messages.of(msgs).report("No " + DB + " option specified");
            return false;
        }

        return true;
    }

    @Override
    public PostGISWorkspace open(Map<?, Object> opts) throws IOException {
        return new PostGISWorkspace(PostGISOpts.fromMap(opts));
    }

    @Override
    public boolean canCreate(Map<?, Object> opts, Messages msgs) {
        Messages.of(msgs).report("Creation not suported");
        return false;
    }

    @Override
    public PostGISWorkspace create(Map<?, Object> opts, Schema schema) throws IOException {
        throw new UnsupportedOperationException();
    }
}
