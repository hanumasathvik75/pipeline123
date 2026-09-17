package tools.pipeline_runner;

import com.ignitionai.phase6.Phase6Fixtures;
import com.ignitionai.phase6.VehicleHealthAssessment;
import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import com.ignitionai.phase7.app.DealershipAppMVP;

public class Phase7DemoRunner {
    public static void main(String[] args) {
        System.out.println("Starting Phase 7 End-to-End Demo...\n");
        
        System.out.println("[Step 1] Loading Phase 5 degradation/event-risk fixture...");
        System.out.println("   (Simulated load of phase5-contract degraded state data)");
        
        System.out.println("[Step 2] Translating to Phase 6 health-score fixture...");
        VehicleHealthAssessment ha = Phase6Fixtures.getDegradedVehicle();
        
        System.out.println("[Step 3] Generating Certificate Snapshot...");
        CertificateSnapshot snapshot = new CertificateSnapshot(ha);
        
        System.out.println("[Step 4 & 5] Generating LaTeX certificate and loading Dealership Application View...\n");
        DealershipAppMVP app = new DealershipAppMVP();
        app.loadAndDisplaySnapshot(snapshot);
        
        System.out.println("\n[Demo] Exporting LaTeX Certificate to Output Directory...");
        app.exportCertificate(snapshot, "out/certificates");
        
        System.out.println("\n[Demo] Demonstrating Historical Comparison View...");
        app.compareAssessments(
            new CertificateSnapshot(Phase6Fixtures.getHistoricalComparison1()),
            new CertificateSnapshot(Phase6Fixtures.getHistoricalComparison2())
        );
        
        System.out.println("Phase 7 End-to-End Demo complete.\n");
    }
}
