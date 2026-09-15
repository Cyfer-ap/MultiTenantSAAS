package com.chacha.multitenantsaas.whiteboards;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiteboardNodeRepository extends JpaRepository<WhiteboardNode, UUID> {

    List<WhiteboardNode> findByTenantIdAndProjectIdAndBoardIdOrderByZIndexAscNodeKeyAsc(
            UUID tenantId, UUID projectId, UUID boardId);

    Optional<WhiteboardNode> findByTenantIdAndProjectIdAndBoardIdAndNodeKey(
            UUID tenantId, UUID projectId, UUID boardId, String nodeKey);

    void deleteByTenantIdAndProjectIdAndBoardId(UUID tenantId, UUID projectId, UUID boardId);
}
