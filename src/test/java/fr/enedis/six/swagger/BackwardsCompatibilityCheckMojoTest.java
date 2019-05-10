package fr.enedis.six.swagger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

/**
 * Tests the backwards compatibility check mojo.
 */
public class BackwardsCompatibilityCheckMojoTest extends AbstractMojoTestCase {

    private final String testDir = "/src/test/resources/unit/";
    private BackwardsCompatibilityCheckMojo myMojo;

    protected void setUp() throws Exception {
        super.setUp(); // required

        setupMojo();
    }

    protected void tearDown() throws Exception {
        super.tearDown(); // required

        File lockFile = getTestFile(testDir + "swagger/test.lock");
        lockFile.delete();
        File testFile = getTestFile(testDir + "swagger/test.json");
        testFile.delete();
    }

    /**
     * Tests that the .lock file is initialized from the .json file when the plugin is first ran
     */
    @Test
    public void testInit() throws Exception {
        writeTestFile("init.json");

        myMojo.execute();
        checkLockFileExists();
    }

    /**
     * Tests that backwards compatibility check fails when breaking change is made.
     */
    @Test
    public void testShouldFailCompatibilityCheckBreakingChange() throws Exception {
        writeTestFile("init.json");
        myMojo.execute();

        writeTestFile("bad.json");

        runMojo(true);
    }

    /**
     * Tests that backwards compatibility check fails when .json file is deleted.
     */
    @Test
    public void testShouldFailCompatibilityCheckJsonFileDeleted() throws Exception {
        writeTestFile("init.json");
        myMojo.execute();

        File testFile = getTestFile(testDir + "swagger/test.json");
        testFile.delete();

        runMojo(true);
    }

    /**
     * Tests that backwards compatibility check passes when breaking change is forced.
     */
    @Test
    public void testShouldPassCompatibilityCheckForceChange() throws Exception {
        writeTestFile("init.json");
        myMojo.execute();

        writeTestFile("bad.json");
        File lockFile = getTestFile(testDir + "swagger/test.lock");
        lockFile.delete();

        runMojo(false);
    }

    /**
     * Tests that backwards compatibility check passes when non-breaking change is made.
     */
    @Test
    public void testShouldPassCompatibilityCheckNonBreakingChange() throws Exception {
        writeTestFile("init.json");

        myMojo.execute();
        writeTestFile("good.json");

        runMojo(false);
    }

    private void setupMojo() throws Exception {
        File pom = getTestFile(testDir + "project-to-test/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        myMojo = (BackwardsCompatibilityCheckMojo) lookupMojo("backwards-compatibility-check", pom);
        assertNotNull(myMojo);

        Model m = new Model();
        Build b = new Build();
        b.setDirectory(System.getProperty("user.dir") + testDir);
        m.setBuild(b);
    }

    private void writeTestFile(String filename) throws Exception {
        File testFile = getTestFile(testDir + "swagger/test.json");
        if (testFile.exists()) {
            testFile.delete();
        }
        testFile.getParentFile().mkdirs();
        testFile.createNewFile();
        File jsonFile = getTestFile(testDir + "testSwaggers/" + filename);
        try (InputStream is = new FileInputStream(jsonFile);
            OutputStream os = new FileOutputStream(testFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private void runMojo(boolean shouldFail) throws MojoExecutionException {
        try {
            myMojo.execute();
            if (shouldFail) {
                fail();
            }
        } catch (MojoFailureException ex) {
            if (shouldFail) {
                assertEquals("Backwards compatibility check failed for group test", ex.getMessage());
            } else {
                fail();
            }
        }
    }

    private void checkLockFileExists() {
        File lockFile = getTestFile(testDir + "swagger/test.lock");
        assertTrue(lockFile.exists());
    }
}
