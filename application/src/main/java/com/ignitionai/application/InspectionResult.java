package com.ignitionai.application;

import com.ignitionai.obdinput.schema.ObdMessage;
import com.ignitionai.obd.AnalyticalObservation;
import com.ignitionai.phase4.AnomalyEpisode;
import com.ignitionai.phase5.DegradationOutput;
import com.ignitionai.phase7.snapshot.CertificateSnapshot;
import java.util.*;

/** Result of one vehicle/session; shared by native clients. */
public final class InspectionResult {
    public final List<ObdMessage> messages;
    public final List<AnomalyEpisode> episodes;
    public final List<DegradationOutput> degradation;
    public final List<AnalyticalObservation> observations;
    public final List<String> notes;
    public final CertificateSnapshot certificate;
    public InspectionResult(List<ObdMessage> messages, List<AnomalyEpisode> episodes,
            List<DegradationOutput> degradation, List<AnalyticalObservation> observations,
            List<String> notes, CertificateSnapshot certificate) {
        this.messages = List.copyOf(messages); this.episodes = List.copyOf(episodes);
        this.degradation = List.copyOf(degradation); this.observations = List.copyOf(observations);
        this.notes = List.copyOf(notes); this.certificate = certificate;
    }
}
