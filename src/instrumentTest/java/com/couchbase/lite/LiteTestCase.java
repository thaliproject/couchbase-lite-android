package com.couchbase.lite;

import junit.framework.TestCase;

import com.couchbase.lite.internal.Body;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.router.*;
import com.couchbase.lite.router.Router;
import com.couchbase.lite.support.FileDirUtils;
import com.couchbase.lite.util.Log;

import junit.framework.Assert;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class LiteTestCase extends TestCase {

    public static final String TAG = "LiteTestCase";

    private static boolean initializedUrlHandler = false;

    protected ObjectMapper mapper = new ObjectMapper();

    protected Manager manager = null;
    protected Database database = null;
    protected String DEFAULT_TEST_DB = "cblite-test";

    @Override
    protected void setUp() throws Exception {
        Log.v(TAG, "setUp");
        super.setUp();

        //for some reason a traditional static initializer causes junit to die
        if(!initializedUrlHandler) {
            URLStreamHandlerFactory.registerSelfIgnoreError();
            initializedUrlHandler = true;
        }

        loadCustomProperties();
        startCBLite();
        startDatabase();
    }

    protected InputStream getAsset(String name) {
        return this.getClass().getResourceAsStream("/assets/" + name);
    }

    protected File getRootDirectory() {
        String rootDirectoryPath = System.getProperty("user.dir");
        File rootDirectory = new File(rootDirectoryPath);
        rootDirectory = new File(rootDirectory, "data/data/com.couchbase.cblite.test/files");

        return rootDirectory;
    }

    protected String getServerPath() {
        String filesDir = getRootDirectory().getAbsolutePath();
        return filesDir;
    }

    protected void startCBLite() throws IOException {
        String serverPath = getServerPath();
        File serverPathFile = new File(serverPath);
        FileDirUtils.deleteRecursive(serverPathFile);
        serverPathFile.mkdir();
        manager = new Manager(new File(getRootDirectory(), "test"), Manager.DEFAULT_OPTIONS);
    }

    protected void stopCBLite() {
        if(manager != null) {
            manager.close();
        }
    }

    protected Database startDatabase() throws CouchbaseLiteException {
        database = ensureEmptyDatabase(DEFAULT_TEST_DB);
        return database;
    }

    protected void stopDatabse() {
        if(database != null) {
            database.close();
        }
    }

    protected Database ensureEmptyDatabase(String dbName) throws CouchbaseLiteException {
        Database db = manager.getExistingDatabase(dbName);
        if(db != null) {
            db.delete();
        }
        db = manager.getDatabase(dbName);
        return db;
    }

    protected void loadCustomProperties() throws IOException {

        Properties systemProperties = System.getProperties();
        InputStream mainProperties = getAsset("test.properties");
        if(mainProperties != null) {
            systemProperties.load(mainProperties);
        }
        try {
            InputStream localProperties = getAsset("local-test.properties");
            if(localProperties != null) {
                systemProperties.load(localProperties);
            }
        } catch (IOException e) {
            Log.w(TAG, "Error trying to read from local-test.properties, does this file exist?");
        }
    }

    protected String getReplicationProtocol() {
        return System.getProperty("replicationProtocol");
    }

    protected String getReplicationServer() {
        return System.getProperty("replicationServer");
    }

    protected int getReplicationPort() {
        return Integer.parseInt(System.getProperty("replicationPort"));
    }

    protected String getReplicationAdminUser() {
        return System.getProperty("replicationAdminUser");
    }

    protected String getReplicationAdminPassword() {
        return System.getProperty("replicationAdminPassword");
    }

    protected String getReplicationDatabase() {
        return System.getProperty("replicationDatabase");
    }

    protected URL getReplicationURL()  {
        try {
            if(getReplicationAdminUser() != null && getReplicationAdminUser().trim().length() > 0) {
                return new URL(String.format("%s://%s:%s@%s:%d/%s", getReplicationProtocol(), getReplicationAdminUser(), getReplicationAdminPassword(), getReplicationServer(), getReplicationPort(), getReplicationDatabase()));
            } else {
                return new URL(String.format("%s://%s:%d/%s", getReplicationProtocol(), getReplicationServer(), getReplicationPort(), getReplicationDatabase()));
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected URL getReplicationURLWithoutCredentials() throws MalformedURLException {
        return new URL(String.format("%s://%s:%d/%s", getReplicationProtocol(), getReplicationServer(), getReplicationPort(), getReplicationDatabase()));
    }

    @Override
    protected void tearDown() throws Exception {
        Log.v(TAG, "tearDown");
        super.tearDown();
        stopDatabse();
        stopCBLite();
    }

    protected Map<String,Object> userProperties(Map<String,Object> properties) {
        Map<String,Object> result = new HashMap<String,Object>();

        for (String key : properties.keySet()) {
            if(!key.startsWith("_")) {
                result.put(key, properties.get(key));
            }
        }

        return result;
    }

    public Map<String, Object> getReplicationAuthParsedJson() throws IOException {
        String authJson = "{\n" +
                "    \"facebook\" : {\n" +
                "        \"email\" : \"jchris@couchbase.com\"\n" +
                "     }\n" +
                "   }\n";
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> authProperties  = mapper.readValue(authJson,
                new TypeReference<HashMap<String,Object>>(){});
        return authProperties;

    }

    public Map<String, Object> getPushReplicationParsedJson() throws IOException {

        Map<String,Object> authProperties = getReplicationAuthParsedJson();

        Map<String,Object> targetProperties = new HashMap<String,Object>();
        targetProperties.put("url", getReplicationURL().toExternalForm());
        targetProperties.put("auth", authProperties);

        Map<String,Object> properties = new HashMap<String,Object>();
        properties.put("source", DEFAULT_TEST_DB);
        properties.put("target", targetProperties);
        return properties;
    }

    public Map<String, Object> getPullReplicationParsedJson() throws IOException {

        Map<String,Object> authProperties = getReplicationAuthParsedJson();

        Map<String,Object> sourceProperties = new HashMap<String,Object>();
        sourceProperties.put("url", getReplicationURL().toExternalForm());
        sourceProperties.put("auth", authProperties);

        Map<String,Object> properties = new HashMap<String,Object>();
        properties.put("source", sourceProperties);
        properties.put("target", DEFAULT_TEST_DB);
        return properties;
    }


    protected URLConnection sendRequest(String method, String path, Map<String, String> headers, Object bodyObj) {
        try {
            URL url = new URL("cblite://" + path);
            URLConnection conn = (URLConnection)url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(method);
            if(headers != null) {
                for (String header : headers.keySet()) {
                    conn.setRequestProperty(header, headers.get(header));
                }
            }
            Map<String, List<String>> allProperties = conn.getRequestProperties();
            if(bodyObj != null) {
                conn.setDoInput(true);
                ByteArrayInputStream bais = new ByteArrayInputStream(mapper.writeValueAsBytes(bodyObj));
                conn.setRequestInputStream(bais);
            }

            Router router = new com.couchbase.lite.router.Router(manager, conn);
            router.start();
            return conn;
        } catch (MalformedURLException e) {
            fail();
        } catch(IOException e) {
            fail();
        }
        return null;
    }

    protected Object parseJSONResponse(URLConnection conn) {
        Object result = null;
        Body responseBody = conn.getResponseBody();
        if(responseBody != null) {
            byte[] json = responseBody.getJson();
            String jsonString = null;
            if(json != null) {
                jsonString = new String(json);
                try {
                    result = mapper.readValue(jsonString, Object.class);
                } catch (Exception e) {
                    fail();
                }
            }
        }
        return result;
    }

    protected Object sendBody(String method, String path, Object bodyObj, int expectedStatus, Object expectedResult) {
        URLConnection conn = sendRequest(method, path, null, bodyObj);
        Object result = parseJSONResponse(conn);
        Log.v(TAG, String.format("%s %s --> %d", method, path, conn.getResponseCode()));
        Assert.assertEquals(expectedStatus, conn.getResponseCode());
        if(expectedResult != null) {
            Assert.assertEquals(expectedResult, result);
        }
        return result;
    }

    protected Object send(String method, String path, int expectedStatus, Object expectedResult) {
        return sendBody(method, path, null, expectedStatus, expectedResult);
    }

    public static void createDocuments(final Database db, final int n) {
        //TODO should be changed to use db.runInTransaction
        for (int i=0; i<n; i++) {
            Map<String,Object> properties = new HashMap<String,Object>();
            properties.put("testName", "testDatabase");
            properties.put("sequence", i);
            createDocumentWithProperties(db, properties);
        }
    };

    static void createDocumentsAsync(final Database db, final int n) {
        db.runAsync(new AsyncTask() {
            @Override
            public void run(Database database) {
                db.beginTransaction();
                createDocuments(db, n);
                db.endTransaction(true);
            }
        });

    };


    public static Document createDocumentWithProperties(Database db, Map<String,Object>  properties) {
        Document  doc = db.createDocument();
        Assert.assertNotNull(doc);
        Assert.assertNull(doc.getCurrentRevisionId());
        Assert.assertNull(doc.getCurrentRevision());
        Assert.assertNotNull("Document has no ID", doc.getId()); // 'untitled' docs are no longer untitled (8/10/12)
        try{
            doc.putProperties(properties);
        } catch( Exception e){
            Log.e(TAG, "Error creating document", e);
            assertTrue("can't create new document in db:" + db.getName() + " with properties:" + properties.toString(), false);
        }
        Assert.assertNotNull(doc.getId());
        Assert.assertNotNull(doc.getCurrentRevisionId());
        Assert.assertNotNull(doc.getUserProperties());

        // this won't work until the weakref hashmap is implemented which stores all docs
        // Assert.assertEquals(db.getDocument(doc.getId()), doc);

        Assert.assertEquals(db.getDocument(doc.getId()).getId(), doc.getId());

        return doc;
    }

    public void runReplication(Replication replication) {

        CountDownLatch replicationDoneSignal = new CountDownLatch(1);


        ReplicationFinishedObserver replicationFinishedObserver = new ReplicationFinishedObserver(replicationDoneSignal);
        replication.addChangeListener(replicationFinishedObserver);

        replication.start();

        CountDownLatch replicationDoneSignalPolling = replicationWatcherThread(replication);

        Log.d(TAG, "Waiting for replicator to finish");
        try {
            boolean success = replicationDoneSignal.await(300, TimeUnit.SECONDS);
            assertTrue(success);

            success = replicationDoneSignalPolling.await(300, TimeUnit.SECONDS);
            assertTrue(success);

            Log.d(TAG, "replicator finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        replication.removeChangeListener(replicationFinishedObserver);



    }

    public CountDownLatch replicationWatcherThread(final Replication replication) {

        final CountDownLatch doneSignal = new CountDownLatch(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean started = false;
                boolean done = false;
                while (!done) {

                    if (replication.isRunning()) {
                        started = true;
                    }
                    final boolean statusIsDone = (replication.getStatus() == Replication.ReplicationStatus.REPLICATION_STOPPED ||
                            replication.getStatus() == Replication.ReplicationStatus.REPLICATION_IDLE);
                    if (started && statusIsDone) {
                        done = true;
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                doneSignal.countDown();

            }
        }).start();
        return doneSignal;

    }

    public static class ReplicationFinishedObserver implements Replication.ChangeListener {

        public boolean replicationFinished = false;
        private CountDownLatch doneSignal;

        public ReplicationFinishedObserver(CountDownLatch doneSignal) {
            this.doneSignal = doneSignal;
        }

        @Override
        public void changed(Replication.ChangeEvent event) {
            Replication replicator = event.getSource();
            Log.d(TAG, replicator + " changed.  " + replicator.getCompletedChangesCount() + " / " + replicator.getChangesCount());
            Assert.assertTrue(replicator.getCompletedChangesCount() <= replicator.getChangesCount());
            if (replicator.getCompletedChangesCount() > replicator.getChangesCount()) {
                throw new RuntimeException("replicator.getCompletedChangesCount() > replicator.getChangesCount()");
            }
            if (!replicator.isRunning()) {
                replicationFinished = true;
                String msg = String.format("ReplicationFinishedObserver.changed called, set replicationFinished to: %b", replicationFinished);
                Log.d(TAG, msg);
                doneSignal.countDown();
            }
            else {
                String msg = String.format("ReplicationFinishedObserver.changed called, but replicator still running, so ignore it");
                Log.d(TAG, msg);
            }
        }

        boolean isReplicationFinished() {
            return replicationFinished;
        }

    }

    public static class ReplicationRunningObserver implements Replication.ChangeListener {

        private CountDownLatch doneSignal;

        public ReplicationRunningObserver(CountDownLatch doneSignal) {
            this.doneSignal = doneSignal;
        }

        @Override
        public void changed(Replication.ChangeEvent event) {
            Replication replicator = event.getSource();
            if (replicator.isRunning()) {
                doneSignal.countDown();
            }
        }

    }

    public static class ReplicationIdleObserver implements Replication.ChangeListener {

        private CountDownLatch doneSignal;

        public ReplicationIdleObserver(CountDownLatch doneSignal) {
            this.doneSignal = doneSignal;
        }

        @Override
        public void changed(Replication.ChangeEvent event) {
            Replication replicator = event.getSource();
            if (replicator.getStatus() == Replication.ReplicationStatus.REPLICATION_IDLE) {
                doneSignal.countDown();
            }
        }

    }

    public static class ReplicationErrorObserver implements Replication.ChangeListener {

        private CountDownLatch doneSignal;

        public ReplicationErrorObserver(CountDownLatch doneSignal) {
            this.doneSignal = doneSignal;
        }

        @Override
        public void changed(Replication.ChangeEvent event) {
            Replication replicator = event.getSource();
            if (replicator.getLastError() != null) {
                doneSignal.countDown();
            }
        }

    }


}
