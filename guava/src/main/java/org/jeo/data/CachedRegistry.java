package org.jeo.data;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.jeo.feature.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public class CachedRegistry implements Registry {

    static Logger LOG = LoggerFactory.getLogger(CachedRegistry.class);

    Registry reg;
    LoadingCache<String, Workspace> wsCache;

    public CachedRegistry(Registry reg) {
        this(reg, 20);
    }

    public CachedRegistry(Registry reg, final int cacheSize) {
        this.reg = reg;
        wsCache = CacheBuilder.newBuilder().maximumSize(cacheSize)
            .removalListener(new RemovalListener<String, Workspace>() {
                @Override
                public void onRemoval(RemovalNotification<String, Workspace> n) {
                    n.getValue().dispose();
                }
            }).build(new CacheLoader<String, Workspace>() {
                @Override
                public Workspace load(String key) throws Exception {
                    return new CachedWorkspace(CachedRegistry.this.reg.get(key), cacheSize);
                }
            });
    }

    @Override
    public Workspace get(String key) {
        try {
            return wsCache.get(key);
        } catch (ExecutionException e) {
            LOG.warn("Error loading workspace from cache", e);
            return reg.get(key);
        }
    }

    @Override
    public void dispose() {
        reg.dispose();
    }

    static class CachedWorkspace implements Workspace {

        Workspace ws;
        LoadingCache<String,Layer> layerCache;

        CachedWorkspace(Workspace ws, int cacheSize) {
            this.ws = ws;
            layerCache = CacheBuilder.newBuilder().maximumSize(cacheSize)
                .build(new CacheLoader<String, Layer>() {
                    @Override
                    public Layer load(String key) throws Exception {
                        return CachedWorkspace.this.ws.get(key);
                    }
            });
        }

        @Override
        public Vector create(Schema schema) throws IOException {
            return ws.create(schema);
        }

        @Override
        public Iterator<String> layers() throws IOException {
            return ws.layers();
        }

        @Override
        public Layer get(String layer) throws IOException {
            try {
                return layerCache.get(layer);
            } catch (ExecutionException e) {
                LOG.warn("Error loading layer from cache", e);
                return ws.get(layer);
            }
        }
    
        @Override
        public void dispose() {
            ws.dispose();
        }
    }
}