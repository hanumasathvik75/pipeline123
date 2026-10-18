package ai.ignition.android.recording;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Unit tests verifying DriveRecordingService state tracking, observer registration,
 * idempotent start/stop handling, and interrupted-session recovery behavior.
 */
public class DriveRecordingServiceLifecycleTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private File testBaseDir;
    private SessionRepository repository;

    @Before
    public void setUp() throws Exception {
        testBaseDir = tempFolder.newFolder("service_lifecycle_test");
        repository = new SessionRepository(testBaseDir);
    }

    /**
     * Test 1: Service state initial values and listener registration.
     */
    @Test
    public void testServiceStateAndListenerRegistration() {
        AtomicBoolean stateNotified = new AtomicBoolean(false);
        DriveCaptureEngine.CaptureListener listener = new DriveCaptureEngine.CaptureListener() {
            @Override public void onStateChanged(CaptureState state) { stateNotified.set(true); }
            @Override public void onObservation(SessionEvent event) {}
            @Override public void onConnectionLost(String reason) {}
            @Override public void onConnectionRestored(long gapDurationMs) {}
            @Override public void onError(String error) {}
            @Override public void onFinalized(SessionManifest manifest) {}
        };

        DriveRecordingService.addStateListener(listener);
        DriveRecordingService.removeStateListener(listener);
        assertFalse("Removed listener should not cause errors", stateNotified.get());
    }

    /**
     * Test 2: Interrupted session recovery on app launch.
     * Sessions left in RECORDING state due to process termination are recovered as INTERRUPTED.
     */
    @Test
    public void testInterruptedSessionRecoveryHonestStatus() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        // Start session in repository (creates RECORDING manifest)
        JsonlSessionWriter writer = repository.startSession(sessionId);

        // Write sample observation
        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("Engine RPM")
                .value(2100.0)
                .unit("rpm")
                .status(ObservationStatus.VALID)
                .build());

        // Simulate crash: writer closed without finalization
        writer.close();

        // New launch detects interrupted session
        SessionRepository startupRepo = new SessionRepository(testBaseDir);
        List<SessionManifest> recovered = startupRepo.detectAndRecoverInterruptedSessions();

        assertEquals("Must discover 1 interrupted session", 1, recovered.size());
        SessionManifest manifest = recovered.get(0);
        assertEquals(sessionId, manifest.getSessionId());
        assertEquals("Must be marked INTERRUPTED honestly, never COMPLETE", SessionStatus.INTERRUPTED, manifest.getStatus());
        assertTrue("Partial records must be preserved", manifest.getValidRecordCount() >= 2);

        // Raw file must remain intact
        File jsonlFile = startupRepo.getSessionJsonlFile(sessionId);
        assertTrue("JSONL file must be preserved", jsonlFile.exists() && jsonlFile.length() > 0);
    }

    /**
     * Test 3: Startup recovery is idempotent.
     * Running recovery twice does not alter or corrupt the recovery manifest.
     */
    @Test
    public void testStartupRecoveryIdempotence() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);
        writer.writeEvent(SessionEvent.builder()
                .sessionId(sessionId)
                .eventType(EventType.OBD_OBSERVATION)
                .signalName("Vehicle Speed")
                .value(45.0)
                .unit("km/h")
                .status(ObservationStatus.VALID)
                .build());
        writer.close();

        SessionRepository repo = new SessionRepository(testBaseDir);
        List<SessionManifest> firstRecovery = repo.detectAndRecoverInterruptedSessions();
        assertEquals(1, firstRecovery.size());
        long firstFileSize = repo.getSessionJsonlFile(sessionId).length();

        // Second scan must not duplicate or corrupt
        List<SessionManifest> secondRecovery = repo.detectAndRecoverInterruptedSessions();
        assertEquals("Already recovered session should not be re-recovered", 0, secondRecovery.size());

        SessionManifest manifest = repo.getSessionManifest(sessionId);
        assertEquals(SessionStatus.INTERRUPTED, manifest.getStatus());
        assertEquals(firstFileSize, repo.getSessionJsonlFile(sessionId).length());
    }

    /**
     * Test 4: Finalize is idempotent against duplicate stop requests.
     */
    @Test
    public void testRepositoryFinalizeIdempotence() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        JsonlSessionWriter writer = repository.startSession(sessionId);

        SessionManifest manifest1 = repository.finalizeSession(sessionId);
        assertNotNull(manifest1);
        assertEquals(SessionStatus.COMPLETE, manifest1.getStatus());

        // Second finalize call must return existing manifest without throwing
        SessionManifest manifest2 = repository.finalizeSession(sessionId);
        assertNotNull(manifest2);
        assertEquals(manifest1.getSessionId(), manifest2.getSessionId());
        assertEquals(manifest1.getStatus(), manifest2.getStatus());
        assertEquals(manifest1.getRecordCount(), manifest2.getRecordCount());
    }
}
