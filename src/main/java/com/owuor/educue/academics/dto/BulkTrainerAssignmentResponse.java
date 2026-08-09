package com.owuor.educue.academics.dto;

import java.util.List;
import java.util.UUID;

public record BulkTrainerAssignmentResponse(int created, int skipped, List<UUID> skippedPlacementUuids) { }
