package com.scmcloud.decision.experiment;

import lombok.Data;
import java.util.Map;

@Data
public class Variant {
    private String id;
    private String name;
    private int trafficPercent;
    private String weightProfileId;
    private Map<String, Object> overrides;
}
