package com.chacha.multitenantsaas.whiteboards;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiteboardEdgeRepository extends JpaRepository<WhiteboardEdge, UUID> {

    List<WhiteboardEdge>
            findByTenantIdAndProjectIdAndBoardIdOrderBySourceNodeKeyAscTargetNodeKeyAsc(
                    UUID tenantId, UUID projectId, UUID boardId);

    void deleteByTenantIdAndProjectIdAndBoardId(UUID tenantId, UUID projectId, UUID boardId);
}
