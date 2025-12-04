package io.ascend.flockr.admin.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerAction;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.service.AudienceService;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.CompletionStage;
import org.junit.Assert;
import org.junit.Test;

public class AudienceControllerTest {

  @Test
  public void updateAudienceOwner_returnsSuccessAndDelegatesToService() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    String projectId = "p1";
    Long audienceId = 42L;
    String actingEmail = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("target@example.com");

    when(audienceService.updateAudienceOwner(
            eq(projectId), eq(audienceId), eq(actingEmail), eq(request)))
        .thenReturn(Single.just(Boolean.TRUE));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> stage =
        controller.updateAudienceOwner(projectId, audienceId, actingEmail, request);
    ResponseEntity.Success<Boolean> response = stage.toCompletableFuture().join();

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(Boolean.TRUE, response.data());
    verify(audienceService)
        .updateAudienceOwner(eq(projectId), eq(audienceId), eq(actingEmail), eq(request));
  }
}
