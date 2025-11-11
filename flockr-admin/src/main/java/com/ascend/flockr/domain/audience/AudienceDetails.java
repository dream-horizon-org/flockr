package com.ascend.flockr.domain.audience;

import java.util.List;

public record AudienceDetails(AudienceMeta audienceMeta, List<RuleMeta> ruleMetas) {}
