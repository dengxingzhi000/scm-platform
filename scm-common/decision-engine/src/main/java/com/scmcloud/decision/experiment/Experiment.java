package com.scmcloud.decision.experiment;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Experiment {
    private String id;
    private String engineType;
    private String name;
    private ExperimentStatus status;
    private List<Variant> variants;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public enum ExperimentStatus { DRAFT, RUNNING, PAUSED, COMPLETED }
}
