package io.ascend.flockr.admin.rest;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.rule.RuleAction;
import io.ascend.flockr.admin.domain.rule.RuleMeta;
import io.ascend.flockr.admin.domain.rule.RuleType;
import io.ascend.flockr.admin.domain.rule.SourceInfoEnriched;
import io.ascend.flockr.admin.io.ResponseEntity;
import io.ascend.flockr.admin.io.request.CreateAudienceRequest;
import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerAction;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.io.response.*;
import io.ascend.flockr.admin.service.AudienceService;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Test;

/**
 * Unit tests for {@link AudienceController}.
 *
 * <p>These tests verify that the controller correctly delegates to the service layer and handles
 * responses appropriately.
 */
public class AudienceControllerTest {

  private static final String PROJECT_ID = "test-project-key";
  private static final String ACTOR_EMAIL = "actor@example.com";
  private static final Long AUDIENCE_ID = 42L;
  private static final Long RULE_ID = 100L;

  // ========================================
  // createAudience Tests
  // ========================================

  @Test
  public void createAudience_success_returnsAudienceId() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    CreateAudienceRequest request = new CreateAudienceRequest();
    request.setName("Test Audience");
    request.setDescription("Test Description");
    request.setType("CONDITIONAL");
    request.setExpireDate(1735689600000L);
    request.setSinkIds(List.of(1L, 2L));

    Long expectedAudienceId = 123L;
    when(audienceService.createAudience(eq(PROJECT_ID), eq(request), eq(ACTOR_EMAIL)))
        .thenReturn(Single.just(expectedAudienceId));

    // Act
    CompletionStage<ResponseEntity.Success<Long>> result =
        controller.createAudience(PROJECT_ID, ACTOR_EMAIL, request);
    ResponseEntity.Success<Long> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(expectedAudienceId, response.data());
    verify(audienceService).createAudience(eq(PROJECT_ID), eq(request), eq(ACTOR_EMAIL));
  }

  @Test
  public void createAudience_withDefaultActor_usesSystemAsActor() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    CreateAudienceRequest request = new CreateAudienceRequest();
    request.setName("Test Audience");
    request.setDescription("Test Description");
    request.setType("STATIC");
    request.setExpireDate(1735689600000L);
    request.setSinkIds(List.of(1L));

    Long expectedAudienceId = 456L;
    when(audienceService.createAudience(eq(PROJECT_ID), eq(request), eq("system")))
        .thenReturn(Single.just(expectedAudienceId));

    // Act
    CompletionStage<ResponseEntity.Success<Long>> result =
        controller.createAudience(PROJECT_ID, "system", request);
    ResponseEntity.Success<Long> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(expectedAudienceId, response.data());
    verify(audienceService).createAudience(eq(PROJECT_ID), eq(request), eq("system"));
  }

  @Test
  public void createAudience_serviceThrowsException_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    CreateAudienceRequest request = new CreateAudienceRequest();
    request.setName("Test Audience");
    request.setDescription("Test Description");
    request.setType("CONDITIONAL");
    request.setExpireDate(1735689600000L);
    request.setSinkIds(List.of(1L));

    RuntimeException expectedException = new RuntimeException("Service error");
    when(audienceService.createAudience(eq(PROJECT_ID), eq(request), eq(ACTOR_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<Long>> result =
        controller.createAudience(PROJECT_ID, ACTOR_EMAIL, request);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof RuntimeException);
      assertTrue(e.getCause().getMessage().contains("Service error"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // getAudienceDetails Tests
  // ========================================

  @Test
  public void getAudienceDetails_success_returnsAudienceDetails() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    AudienceMeta audienceMeta =
        AudienceMeta.builder()
            .audienceId(AUDIENCE_ID)
            .name("Test Audience")
            .description("Test Description")
            .type("CONDITIONAL")
            .verified(true)
            .userCount(1000L)
            .build();

    AudienceDetailsResponse expectedResponse =
        new AudienceDetailsResponse(audienceMeta, Collections.emptyList(), Collections.emptyList());

    when(audienceService.getAudienceDetails(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<AudienceDetailsResponse>> result =
        controller.getAudienceDetails(PROJECT_ID, AUDIENCE_ID);
    ResponseEntity.Success<AudienceDetailsResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(AUDIENCE_ID, response.data().audienceMeta().getAudienceId());
    assertEquals("Test Audience", response.data().audienceMeta().getName());
    verify(audienceService).getAudienceDetails(eq(PROJECT_ID), eq(AUDIENCE_ID));
  }

  @Test
  public void getAudienceDetails_serviceThrowsException_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    RuntimeException expectedException = new RuntimeException("Audience not found");
    when(audienceService.getAudienceDetails(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<AudienceDetailsResponse>> result =
        controller.getAudienceDetails(PROJECT_ID, AUDIENCE_ID);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Audience not found"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // createRules Tests
  // ========================================

  @Test
  public void createRules_success_returnsTrue() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    CreateRulesRequest request = new CreateRulesRequest();
    request.setRules(Collections.emptyList());

    when(audienceService.createRules(
            eq(PROJECT_ID), any(CreateRulesRequest.class), eq(ACTOR_EMAIL)))
        .thenReturn(Single.just(true));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> result =
        controller.createRules(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, request);
    ResponseEntity.Success<Boolean> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertTrue(response.data());
    verify(audienceService)
        .createRules(
            eq(PROJECT_ID),
            argThat(req -> req.getAudienceId().equals(AUDIENCE_ID)),
            eq(ACTOR_EMAIL));
  }

  @Test
  public void createRules_setsAudienceIdFromPathParam() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    CreateRulesRequest request = new CreateRulesRequest();
    request.setRules(Collections.emptyList());

    when(audienceService.createRules(
            eq(PROJECT_ID), any(CreateRulesRequest.class), eq(ACTOR_EMAIL)))
        .thenReturn(Single.just(true));

    // Act
    controller.createRules(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, request);

    // Assert - verify the audienceId was set on the request
    assertEquals(AUDIENCE_ID, request.getAudienceId());
  }

  @Test
  public void createRules_serviceThrowsException_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    CreateRulesRequest request = new CreateRulesRequest();
    request.setRules(Collections.emptyList());

    RuntimeException expectedException = new RuntimeException("Invalid rule configuration");
    when(audienceService.createRules(
            eq(PROJECT_ID), any(CreateRulesRequest.class), eq(ACTOR_EMAIL)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<Boolean>> result =
        controller.createRules(PROJECT_ID, ACTOR_EMAIL, AUDIENCE_ID, request);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Invalid rule configuration"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // getRuleDetails Tests
  // ========================================

  @Test
  public void getRuleDetails_success_returnsRuleDetails() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    RuleMeta<SourceInfoEnriched> ruleMeta =
        RuleMeta.<SourceInfoEnriched>builder()
            .ruleId(RULE_ID)
            .name("Test Rule")
            .description("Test Rule Description")
            .ruleType(RuleType.BATCH)
            .ruleAction(RuleAction.ADD)
            .build();

    RuleDetailsResponse expectedResponse = new RuleDetailsResponse(ruleMeta);

    when(audienceService.getRuleDetails(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(RULE_ID)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<RuleDetailsResponse>> result =
        controller.getRuleDetails(PROJECT_ID, AUDIENCE_ID, RULE_ID);
    ResponseEntity.Success<RuleDetailsResponse> response = result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(RULE_ID, response.data().ruleDetails().getRuleId());
    assertEquals("Test Rule", response.data().ruleDetails().getName());
    verify(audienceService).getRuleDetails(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(RULE_ID));
  }

  @Test
  public void getRuleDetails_ruleNotFound_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    RuntimeException expectedException = new RuntimeException("Rule not found");
    when(audienceService.getRuleDetails(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(RULE_ID)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<RuleDetailsResponse>> result =
        controller.getRuleDetails(PROJECT_ID, AUDIENCE_ID, RULE_ID);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Rule not found"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // getAudiencesList Tests
  // ========================================

  @Test
  public void getAudiencesList_withAllFilters_returnsFilteredList() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    String nameSearch = "marketing";
    String createdBy = "user@example.com";
    Boolean verified = true;
    int pageSize = 20;
    int page = 1;

    AudienceMetaResponse audienceResponse =
        AudienceMetaResponse.builder()
            .audienceId(1L)
            .name("Marketing Audience")
            .description("Marketing users")
            .type("CONDITIONAL")
            .verified(true)
            .ruleCount(5L)
            .userCount(1000L)
            .createdBy(createdBy)
            .build();

    PaginatedResponse<AudienceMetaResponse> expectedResponse =
        new PaginatedResponse<>(
            new PaginatedResponse.PageInfo(page, pageSize, false), List.of(audienceResponse));

    when(audienceService.getAudiencesList(
            eq(PROJECT_ID), eq(nameSearch), eq(createdBy), eq(verified), eq(page), eq(pageSize)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>>> result =
        controller.getAudiencesList(PROJECT_ID, nameSearch, createdBy, verified, pageSize, page);
    ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(1, response.data().data().size());
    assertEquals("Marketing Audience", response.data().data().get(0).getName());
    assertEquals(page, response.data().pageInfo().page());
    assertEquals(pageSize, response.data().pageInfo().pageSize());
    assertFalse(response.data().pageInfo().hasMore());
    verify(audienceService)
        .getAudiencesList(
            eq(PROJECT_ID), eq(nameSearch), eq(createdBy), eq(verified), eq(page), eq(pageSize));
  }

  @Test
  public void getAudiencesList_withDefaultParams_usesDefaults() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    int defaultPageSize = 10;
    int defaultPage = 0;

    PaginatedResponse<AudienceMetaResponse> expectedResponse =
        new PaginatedResponse<>(
            new PaginatedResponse.PageInfo(defaultPage, defaultPageSize, true),
            Collections.emptyList());

    when(audienceService.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(defaultPage), eq(defaultPageSize)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>>> result =
        controller.getAudiencesList(PROJECT_ID, null, null, null, defaultPageSize, defaultPage);
    ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertTrue(response.data().data().isEmpty());
    verify(audienceService)
        .getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(defaultPage), eq(defaultPageSize));
  }

  @Test
  public void getAudiencesList_withPagination_returnsCorrectPage() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    int pageSize = 5;
    int page = 2;

    List<AudienceMetaResponse> audiences =
        List.of(
            AudienceMetaResponse.builder().audienceId(11L).name("Audience 11").build(),
            AudienceMetaResponse.builder().audienceId(12L).name("Audience 12").build(),
            AudienceMetaResponse.builder().audienceId(13L).name("Audience 13").build());

    PaginatedResponse<AudienceMetaResponse> expectedResponse =
        new PaginatedResponse<>(new PaginatedResponse.PageInfo(page, pageSize, true), audiences);

    when(audienceService.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(page), eq(pageSize)))
        .thenReturn(Single.just(expectedResponse));

    // Act
    CompletionStage<ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>>> result =
        controller.getAudiencesList(PROJECT_ID, null, null, null, pageSize, page);
    ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(3, response.data().data().size());
    assertTrue(response.data().pageInfo().hasMore());
  }

  @Test
  public void getAudiencesList_serviceThrowsException_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    RuntimeException expectedException = new RuntimeException("Database error");
    when(audienceService.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(0), eq(10)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<PaginatedResponse<AudienceMetaResponse>>> result =
        controller.getAudiencesList(PROJECT_ID, null, null, null, 10, 0);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Database error"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // getAudienceOwners Tests
  // ========================================

  @Test
  public void getAudienceOwners_success_returnsOwnersList() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    List<AudienceOwnerResponse> expectedOwners =
        List.of(
            AudienceOwnerResponse.builder()
                .id(1L)
                .audienceId(AUDIENCE_ID)
                .ownerEmail("owner1@example.com")
                .status("ACTIVE")
                .createdAt(1700000000L)
                .build(),
            AudienceOwnerResponse.builder()
                .id(2L)
                .audienceId(AUDIENCE_ID)
                .ownerEmail("owner2@example.com")
                .status("ACTIVE")
                .createdAt(1700001000L)
                .build());

    when(audienceService.getAudienceOwners(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(expectedOwners));

    // Act
    CompletionStage<ResponseEntity.Success<List<AudienceOwnerResponse>>> result =
        controller.getAudienceOwners(PROJECT_ID, AUDIENCE_ID);
    ResponseEntity.Success<List<AudienceOwnerResponse>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertEquals(2, response.data().size());
    assertEquals("owner1@example.com", response.data().get(0).getOwnerEmail());
    assertEquals("owner2@example.com", response.data().get(1).getOwnerEmail());
    verify(audienceService).getAudienceOwners(eq(PROJECT_ID), eq(AUDIENCE_ID));
  }

  @Test
  public void getAudienceOwners_emptyList_returnsEmptyList() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    when(audienceService.getAudienceOwners(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(Collections.emptyList()));

    // Act
    CompletionStage<ResponseEntity.Success<List<AudienceOwnerResponse>>> result =
        controller.getAudienceOwners(PROJECT_ID, AUDIENCE_ID);
    ResponseEntity.Success<List<AudienceOwnerResponse>> response =
        result.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertNotNull(response.data());
    assertTrue(response.data().isEmpty());
  }

  @Test
  public void getAudienceOwners_audienceNotFound_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    RuntimeException expectedException = new RuntimeException("Audience not found");
    when(audienceService.getAudienceOwners(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<List<AudienceOwnerResponse>>> result =
        controller.getAudienceOwners(PROJECT_ID, AUDIENCE_ID);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Audience not found"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  // ========================================
  // updateAudienceOwner Tests
  // ========================================

  @Test
  public void updateAudienceOwner_addAction_returnsSuccessAndDelegatesToService() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("target@example.com");

    when(audienceService.updateAudienceOwner(
            eq(PROJECT_ID), eq(AUDIENCE_ID), eq(ACTOR_EMAIL), eq(request)))
        .thenReturn(Single.just(Boolean.TRUE));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> stage =
        controller.updateAudienceOwner(PROJECT_ID, AUDIENCE_ID, ACTOR_EMAIL, request);
    ResponseEntity.Success<Boolean> response = stage.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(Boolean.TRUE, response.data());
    verify(audienceService)
        .updateAudienceOwner(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(ACTOR_EMAIL), eq(request));
  }

  @Test
  public void updateAudienceOwner_removeAction_returnsSuccessAndDelegatesToService() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.REMOVE);
    request.setEmail("target@example.com");

    when(audienceService.updateAudienceOwner(
            eq(PROJECT_ID), eq(AUDIENCE_ID), eq(ACTOR_EMAIL), eq(request)))
        .thenReturn(Single.just(Boolean.TRUE));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> stage =
        controller.updateAudienceOwner(PROJECT_ID, AUDIENCE_ID, ACTOR_EMAIL, request);
    ResponseEntity.Success<Boolean> response = stage.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertEquals(Boolean.TRUE, response.data());
    verify(audienceService)
        .updateAudienceOwner(eq(PROJECT_ID), eq(AUDIENCE_ID), eq(ACTOR_EMAIL), eq(request));
  }

  @Test
  public void updateAudienceOwner_withDefaultEmail_usesSystem() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("newowner@example.com");

    when(audienceService.updateAudienceOwner(
            eq(PROJECT_ID), eq(AUDIENCE_ID), eq("system"), eq(request)))
        .thenReturn(Single.just(Boolean.TRUE));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> stage =
        controller.updateAudienceOwner(PROJECT_ID, AUDIENCE_ID, "system", request);
    ResponseEntity.Success<Boolean> response = stage.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertTrue(response.data());
    verify(audienceService)
        .updateAudienceOwner(eq(PROJECT_ID), eq(AUDIENCE_ID), eq("system"), eq(request));
  }

  @Test
  public void updateAudienceOwner_serviceReturnsFalse_returnsFalse() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("target@example.com");

    when(audienceService.updateAudienceOwner(
            eq(PROJECT_ID), eq(AUDIENCE_ID), eq(ACTOR_EMAIL), eq(request)))
        .thenReturn(Single.just(Boolean.FALSE));

    // Act
    CompletionStage<ResponseEntity.Success<Boolean>> stage =
        controller.updateAudienceOwner(PROJECT_ID, AUDIENCE_ID, ACTOR_EMAIL, request);
    ResponseEntity.Success<Boolean> response = stage.toCompletableFuture().join();

    // Assert
    assertNotNull(response);
    assertFalse(response.data());
  }

  @Test
  public void updateAudienceOwner_unauthorizedUser_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("target@example.com");

    RuntimeException expectedException =
        new RuntimeException("User is not authorized to update audience owners");
    when(audienceService.updateAudienceOwner(
            eq(PROJECT_ID), eq(AUDIENCE_ID), eq(ACTOR_EMAIL), eq(request)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<Boolean>> result =
        controller.updateAudienceOwner(PROJECT_ID, AUDIENCE_ID, ACTOR_EMAIL, request);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(
          e.getCause().getMessage().contains("User is not authorized to update audience owners"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }

  @Test
  public void updateAudienceOwner_expiredAudience_propagatesException() {
    // Arrange
    AudienceService audienceService = mock(AudienceService.class);
    AudienceController controller = new AudienceController(audienceService);

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("target@example.com");

    RuntimeException expectedException = new RuntimeException("Audience has expired");
    when(audienceService.updateAudienceOwner(
            eq(PROJECT_ID), eq(AUDIENCE_ID), eq(ACTOR_EMAIL), eq(request)))
        .thenReturn(Single.error(expectedException));

    // Act & Assert
    CompletionStage<ResponseEntity.Success<Boolean>> result =
        controller.updateAudienceOwner(PROJECT_ID, AUDIENCE_ID, ACTOR_EMAIL, request);

    try {
      result.toCompletableFuture().get();
      fail("Expected exception to be thrown");
    } catch (ExecutionException e) {
      assertTrue(e.getCause().getMessage().contains("Audience has expired"));
    } catch (InterruptedException e) {
      fail("Unexpected InterruptedException");
    }
  }
}
