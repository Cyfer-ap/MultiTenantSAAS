package com.chacha.multitenantsaas.taskcollaboration.external;

import java.util.List;
import java.util.UUID;

public interface ExternalTaskCommentPort {

    ExternalTaskCommentSnapshot createComment(ExternalTaskCommentCommand command);

    List<ExternalTaskCommentSnapshot> listGrantComments(
            UUID tenantId, UUID projectId, UUID taskId, UUID grantId, int limit);
}
