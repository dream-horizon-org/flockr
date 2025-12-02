package io.ascend.flockr.admin.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerAction;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.io.response.AuditLogResponse;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.service.AudienceService;
import io.reactivex.rxjava3.core.Completable;

import java.util.List;
import java.util.concurrent.CompletionStage;

import io.reactivex.rxjava3.core.Single;
import org.junit.Assert;
import org.junit.Test;

public class AudienceControllerTest {

  @Test
  public void updateAudienceOwner_returnsSuccessAndDelegatesToService() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    String tenantId = "t1";
    String projectId = "p1";
    Long audienceId = 42L;
    String actingEmail = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("target@example.com");

    when(audienceService.updateAudienceOwner(
            eq(tenantId), eq(projectId), eq(audienceId), eq(actingEmail), eq(request)))
        .thenReturn(Completable.complete());

    // Act
    CompletionStage<ResponseEntity.Success<String>> stage =
        controller.updateAudienceOwner(tenantId, projectId, audienceId, actingEmail, request);
    ResponseEntity.Success<String> response = stage.toCompletableFuture().join();

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals("audience owner's updated successfully", response.data());
    verify(audienceService)
        .updateAudienceOwner(
            eq(tenantId), eq(projectId), eq(audienceId), eq(actingEmail), eq(request));
  }

  @Test
  public void auditLog_returnsSuccessAndDelegatesToService() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    Long audienceId = 42L;
    int pageSize = 10;
    int pageNum = 0;
    boolean withPagination = true;

    PaginatedResponse<AuditLogResponse>
        payload =
            new PaginatedResponse<>(
                new PaginatedResponse.PageInfo(
                    pageNum, pageSize, false),
                java.util.List.of(
                    new AuditLogResponse(
                        "2025-11-30", List.of())));

    when(audienceService.getAudienceAuditLog(audienceId, pageSize, pageNum, withPagination))
        .thenReturn(Single.just(payload));

    // Act
    CompletionStage<
            ResponseEntity.Success<
                PaginatedResponse<
                    AuditLogResponse>>>
        stage = controller.auditLog(audienceId, pageSize, pageNum, withPagination);
    var response = stage.toCompletableFuture().join();

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(payload, response.data());
    verify(audienceService).getAudienceAuditLog(audienceId, pageSize, pageNum, withPagination);
  }
}
