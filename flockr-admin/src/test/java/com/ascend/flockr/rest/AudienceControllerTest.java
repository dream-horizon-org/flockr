package com.ascend.flockr.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascend.flockr.io.ResponseEntity;
import com.ascend.flockr.io.request.UpdateAudienceOwnerAction;
import com.ascend.flockr.io.request.UpdateAudienceOwnerRequest;
import com.ascend.flockr.service.AudienceService;
import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.CompletionStage;
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

    when(audienceService.updateAudienceOwner(eq(tenantId), eq(projectId), eq(audienceId), eq(actingEmail), eq(request)))
        .thenReturn(Completable.complete());

    // Act
    CompletionStage<ResponseEntity.Success<String>> stage =
        controller.updateAudienceOwner(tenantId, projectId, audienceId, actingEmail, request);
    ResponseEntity.Success<String> response = stage.toCompletableFuture().join();

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals("audience owner's updated successfully", response.data());
    verify(audienceService)
        .updateAudienceOwner(eq(tenantId), eq(projectId), eq(audienceId), eq(actingEmail), eq(request));
  }
}


