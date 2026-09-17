package com.ignitionai.evidence;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EvidenceStore {
    private final List<EvidenceRecord> log = new ArrayList<>();
    
    public void addEvidence(EvidenceRecord record) {
        log.add(record);
    }
    
    public List<EvidenceRecord> getEvidenceForEpisode(String episodeId) {
        return log.stream()
            .filter(e -> e.getEpisodeId().equals(episodeId))
            .collect(Collectors.toList());
    }
}
