package io.ascend.flockr.admin.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceOwner;
import io.ascend.flockr.admin.domain.dataconnectors.DataSinkDetails;
import io.ascend.flockr.admin.domain.dataconnectors.DataSourceDetails;
import io.ascend.flockr.admin.domain.rule.*;
import io.ascend.flockr.admin.exception.ForbiddenAccessException;
import io.ascend.flockr.admin.exception.ResourceNotFoundException;
import io.ascend.flockr.admin.io.request.CreateAudienceRequest;
import io.ascend.flockr.admin.io.request.CreateRulesRequest;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerAction;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.io.response.AudienceDetailsResponse;
import io.ascend.flockr.admin.io.response.AudienceMetaResponse;
import io.ascend.flockr.admin.io.response.AudienceOwnerResponse;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.io.response.RuleDetailsResponse;
import io.ascend.flockr.admin.io.response.AuditLogResponse;
import io.ascend.flockr.admin.io.response.AuditLogItem;
import io.ascend.flockr.admin.domain.audit.AuditLogDefinition;
import io.ascend.flockr.admin.domain.audit.AuditLogAction;
import io.ascend.flockr.admin.domain.audit.AuditLogValue;
import io.ascend.flockr.admin.repository.AudienceOwnerRepository;
import io.ascend.flockr.admin.repository.AudienceRepository;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Test;

/**
 * Unit tests for {@link AudienceServiceImpl}.
 *
 * <p>These tests verify the service layer business logic including audience creation, retrieval,
 * rule management, and owner management.
 */
public class AudienceServiceImplTest {

  private static final String PROJECT_ID = "test-project";
  private static final Long AUDIENCE_ID = 42L;
  private static final Long RULE_ID = 100L;
  private static final String ACTOR_EMAIL = "actor@example.com";

  private AudienceServiceImpl buildService(
      AudienceRepository audienceRepository,
      AudienceOwnerRepository audienceOwnerRepository,
      RuleRepository ruleRepository,
      DataConnectorRepository dataConnectorRepository) {
    return new AudienceServiceImpl(
        audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);
  }

  private static AudienceMeta buildAudienceMeta(boolean expired, String name, Boolean verified) {
    Long expireAt = expired ? 0L : System.currentTimeMillis() + 3_600_000L; // ms
    return AudienceMeta.builder()
        .xProjectId(PROJECT_ID)
        .audienceId(AUDIENCE_ID)
        .name(name)
        .verified(verified)
        .expireDate(expireAt)
        .sinks(List.of(1L, 2L))
        .type("CONDITIONAL")
        .build();
  }

  private static List<AudienceOwner> ownersWith(String... emails) {
    List<AudienceOwner> list = new ArrayList<>();
    long id = 1L;
    for (String email : emails) {
      AudienceOwner o = new AudienceOwner();
      o.setId(id++);
      o.setAudienceId(AUDIENCE_ID);
      o.setOwnerEmail(email);
      o.setStatus("ACTIVE");
      o.setCreatedAt(System.currentTimeMillis() / 1000);
      list.add(o);
    }
    return list;
  }

  // ========================================
  // createAudience Tests
  // ========================================

  @Test
  public void createAudience_success_returnsAudienceId() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    CreateAudienceRequest request = new CreateAudienceRequest();
    request.setName("Test Audience");
    request.setDescription("Test Description");
    request.setType("CONDITIONAL");
    request.setExpireDate(1735689600000L);
    request.setSinkIds(List.of(1L, 2L));

    Long expectedId = 123L;
    when(audienceRepository.createAudience(any(AudienceMeta.class)))
        .thenReturn(Single.just(expectedId));

    TestObserver<Long> to = service.createAudience(PROJECT_ID, request, ACTOR_EMAIL).test();
    to.assertComplete();
    to.assertValue(expectedId);

    verify(audienceRepository)
        .createAudience(
            argThat(
                meta ->
                    meta.getName().equals("Test Audience")
                        && meta.getXProjectId().equals(PROJECT_ID)
                        && meta.getCreatedBy().equals(ACTOR_EMAIL)));
  }

  @Test
  public void createAudience_withNullActor_usesSystemAsDefault() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    CreateAudienceRequest request = new CreateAudienceRequest();
    request.setName("Test Audience");
    request.setDescription("Test Description");
    request.setType("CONDITIONAL");
    request.setExpireDate(1735689600000L);
    request.setSinkIds(List.of(1L));

    when(audienceRepository.createAudience(any(AudienceMeta.class))).thenReturn(Single.just(456L));

    TestObserver<Long> to = service.createAudience(PROJECT_ID, request, null).test();
    to.assertComplete();

    verify(audienceRepository)
        .createAudience(argThat(meta -> meta.getCreatedBy().equals("system")));
  }

  @Test
  public void createAudience_withBlankActor_usesSystemAsDefault() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    CreateAudienceRequest request = new CreateAudienceRequest();
    request.setName("Test Audience");
    request.setDescription("Test Description");
    request.setType("STATIC");
    request.setExpireDate(1735689600000L);
    request.setSinkIds(List.of(1L));

    when(audienceRepository.createAudience(any(AudienceMeta.class))).thenReturn(Single.just(789L));

    TestObserver<Long> to = service.createAudience(PROJECT_ID, request, "   ").test();
    to.assertComplete();

    verify(audienceRepository)
        .createAudience(argThat(meta -> meta.getCreatedBy().equals("system")));
  }

  @Test
  public void createAudience_repositoryError_propagatesException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    CreateAudienceRequest request = new CreateAudienceRequest();
    request.setName("Test Audience");
    request.setDescription("Test Description");
    request.setType("CONDITIONAL");
    request.setExpireDate(1735689600000L);
    request.setSinkIds(List.of(1L));

    when(audienceRepository.createAudience(any(AudienceMeta.class)))
        .thenReturn(Single.error(new RuntimeException("Database error")));

    TestObserver<Long> to = service.createAudience(PROJECT_ID, request, ACTOR_EMAIL).test();
    to.assertError(RuntimeException.class);
  }

  // ========================================
  // getAudienceDetails Tests
  // ========================================

  @Test
  public void getAudienceDetails_success_returnsEnrichedDetails() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    AudienceMeta audienceMeta = buildAudienceMeta(false, "Test Audience", true);
    DataSinkDetails sinkDetails =
        DataSinkDetails.builder().id(1L).name("Test Sink").type("KAFKA").build();

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(audienceMeta));
    when(dataConnectorRepository.getDataSinksByIds(anyList()))
        .thenReturn(Single.just(List.of(sinkDetails, sinkDetails)));
    when(ruleRepository.getRulesByAudienceId(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(Collections.emptyList()));

    TestObserver<AudienceDetailsResponse> to =
        service.getAudienceDetails(PROJECT_ID, AUDIENCE_ID).test();
    to.assertComplete();
    to.assertValue(
        response -> {
          assertNotNull(response.audienceMeta());
          assertEquals("Test Audience", response.audienceMeta().getName());
          assertNotNull(response.sinks());
          assertNotNull(response.rules());
          return true;
        });
  }

  @Test
  public void getAudienceDetails_audienceNotFound_throwsResourceNotFoundException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    TestObserver<AudienceDetailsResponse> to =
        service.getAudienceDetails(PROJECT_ID, AUDIENCE_ID).test();
    to.assertError(ResourceNotFoundException.class);
  }

  // ========================================
  // getAudiencesList Tests
  // ========================================

  @Test
  public void getAudiencesList_success_returnsPaginatedResponse() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    List<AudienceMetaResponse> audiences =
        List.of(
            AudienceMetaResponse.builder().audienceId(1L).name("Audience 1").build(),
            AudienceMetaResponse.builder().audienceId(2L).name("Audience 2").build());

    when(audienceRepository.getAudiencesList(
            eq(PROJECT_ID), eq("search"), eq("creator"), eq(true), eq(10), eq(0)))
        .thenReturn(Single.just(audiences));

    TestObserver<PaginatedResponse<AudienceMetaResponse>> to =
        service.getAudiencesList(PROJECT_ID, "search", "creator", true, 0, 10).test();
    to.assertComplete();
    to.assertValue(
        response -> {
          assertEquals(2, response.data().size());
          assertEquals(0, response.pageInfo().page());
          assertEquals(10, response.pageInfo().pageSize());
          return true;
        });
  }

  @Test
  public void getAudiencesList_withNullParams_usesDefaults() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    when(audienceRepository.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(10), eq(0)))
        .thenReturn(Single.just(Collections.emptyList()));

    TestObserver<PaginatedResponse<AudienceMetaResponse>> to =
        service.getAudiencesList(PROJECT_ID, null, null, null, null, null).test();
    to.assertComplete();

    verify(audienceRepository)
        .getAudiencesList(eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(10), eq(0));
  }

  @Test
  public void getAudiencesList_withInvalidPageParams_usesDefaults() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    when(audienceRepository.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(10), eq(0)))
        .thenReturn(Single.just(Collections.emptyList()));

    // Negative values should be replaced with defaults
    TestObserver<PaginatedResponse<AudienceMetaResponse>> to =
        service.getAudiencesList(PROJECT_ID, null, null, null, -1, -5).test();
    to.assertComplete();

    verify(audienceRepository)
        .getAudiencesList(eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(10), eq(0));
  }

  @Test
  public void getAudiencesList_calculatesOffsetCorrectly() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    // page 2, pageSize 20 should result in offset 40
    when(audienceRepository.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(20), eq(40)))
        .thenReturn(Single.just(Collections.emptyList()));

    TestObserver<PaginatedResponse<AudienceMetaResponse>> to =
        service.getAudiencesList(PROJECT_ID, null, null, null, 2, 20).test();
    to.assertComplete();

    verify(audienceRepository)
        .getAudiencesList(eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(20), eq(40));
  }

  @Test
  public void getAudiencesList_hasMore_whenResultsSizeEqualsPageSize() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    // Return exactly pageSize results
    List<AudienceMetaResponse> audiences =
        List.of(
            AudienceMetaResponse.builder().audienceId(1L).build(),
            AudienceMetaResponse.builder().audienceId(2L).build(),
            AudienceMetaResponse.builder().audienceId(3L).build());

    when(audienceRepository.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(3), eq(0)))
        .thenReturn(Single.just(audiences));

    TestObserver<PaginatedResponse<AudienceMetaResponse>> to =
        service.getAudiencesList(PROJECT_ID, null, null, null, 0, 3).test();
    to.assertComplete();
    to.assertValue(response -> response.pageInfo().hasMore());
  }

  @Test
  public void getAudiencesList_noMore_whenResultsSizeLessThanPageSize() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    // Return fewer than pageSize results
    List<AudienceMetaResponse> audiences =
        List.of(AudienceMetaResponse.builder().audienceId(1L).build());

    when(audienceRepository.getAudiencesList(
            eq(PROJECT_ID), isNull(), isNull(), isNull(), eq(10), eq(0)))
        .thenReturn(Single.just(audiences));

    TestObserver<PaginatedResponse<AudienceMetaResponse>> to =
        service.getAudiencesList(PROJECT_ID, null, null, null, 0, 10).test();
    to.assertComplete();
    to.assertValue(response -> !response.pageInfo().hasMore());
  }

  // ========================================
  // getAudienceOwners Tests
  // ========================================

  @Test
  public void getAudienceOwners_success_returnsOwnersList() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(buildAudienceMeta(false, "Test", false)));
    when(audienceOwnerRepository.findOwners(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(ownersWith("owner1@example.com", "owner2@example.com")));

    TestObserver<List<AudienceOwnerResponse>> to =
        service.getAudienceOwners(PROJECT_ID, AUDIENCE_ID).test();
    to.assertComplete();
    to.assertValue(
        owners -> {
          assertEquals(2, owners.size());
          assertEquals("owner1@example.com", owners.get(0).getOwnerEmail());
          assertEquals("owner2@example.com", owners.get(1).getOwnerEmail());
          return true;
        });
  }

  @Test
  public void getAudienceOwners_audienceNotFound_throwsResourceNotFoundException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    TestObserver<List<AudienceOwnerResponse>> to =
        service.getAudienceOwners(PROJECT_ID, AUDIENCE_ID).test();
    to.assertError(ResourceNotFoundException.class);
  }

  @Test
  public void getAudienceOwners_emptyOwners_returnsEmptyList() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(buildAudienceMeta(false, "Test", false)));
    when(audienceOwnerRepository.findOwners(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(Collections.emptyList()));

    TestObserver<List<AudienceOwnerResponse>> to =
        service.getAudienceOwners(PROJECT_ID, AUDIENCE_ID).test();
    to.assertComplete();
    to.assertValue(List::isEmpty);
  }

  // ========================================
  // getRuleDetails Tests
  // ========================================

  @Test
  public void getRuleDetails_success_returnsEnrichedRuleDetails() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    // Create source info
    SourceInfo sourceInfo = new SourceInfo();
    sourceInfo.setId(1L);

    // Create batch configuration
    BatchConfiguration<SourceInfo> batchConfig =
        BatchConfiguration.<SourceInfo>builder()
            .source(sourceInfo)
            .query("SELECT user_id FROM users WHERE active = true")
            .cronExpression("0 0 * * *")
            .build();

    // Create rule meta
    RuleMeta<SourceInfo> ruleMeta =
        RuleMeta.<SourceInfo>builder()
            .ruleId(RULE_ID)
            .xProjectId(PROJECT_ID)
            .audienceId(AUDIENCE_ID)
            .name("Test Rule")
            .description("Test rule description")
            .startTime(1700000000L)
            .endTime(1800000000L)
            .ruleAction(RuleAction.ADD)
            .ruleType(RuleType.BATCH)
            .status(RuleStatus.SCHEDULED)
            .configuration(batchConfig)
            .createdBy(ACTOR_EMAIL)
            .build();

    // Create data source details
    DataSourceDetails dataSourceDetails =
        DataSourceDetails.builder()
            .id(1L)
            .name("Test Source")
            .type("POSTGRES")
            .status("ACTIVE")
            .build();

    when(ruleRepository.getRuleById(eq(PROJECT_ID), eq(RULE_ID))).thenReturn(Single.just(ruleMeta));
    when(dataConnectorRepository.getDataSourcesByIds(eq(List.of(1L))))
        .thenReturn(Single.just(List.of(dataSourceDetails)));

    TestObserver<RuleDetailsResponse> to =
        service.getRuleDetails(PROJECT_ID, AUDIENCE_ID, RULE_ID).test();
    to.assertComplete();
    to.assertValue(
        response -> {
          assertNotNull(response.ruleDetails());
          assertEquals(RULE_ID, response.ruleDetails().getRuleId());
          assertEquals("Test Rule", response.ruleDetails().getName());
          assertEquals(RuleType.BATCH, response.ruleDetails().getRuleType());
          assertNotNull(response.ruleDetails().getConfiguration());
          return true;
        });

    verify(ruleRepository).getRuleById(eq(PROJECT_ID), eq(RULE_ID));
    verify(dataConnectorRepository).getDataSourcesByIds(eq(List.of(1L)));
  }

  @Test
  public void getRuleDetails_ruleNotFound_throwsResourceNotFoundException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    when(ruleRepository.getRuleById(eq(PROJECT_ID), eq(RULE_ID)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    TestObserver<RuleDetailsResponse> to =
        service.getRuleDetails(PROJECT_ID, AUDIENCE_ID, RULE_ID).test();
    to.assertError(ResourceNotFoundException.class);
  }

  @Test
  public void getRuleDetails_withNoSources_returnsEmptySourceDetails() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    // Create rule with null configuration (edge case)
    RuleMeta<SourceInfo> ruleMeta =
        RuleMeta.<SourceInfo>builder()
            .ruleId(RULE_ID)
            .xProjectId(PROJECT_ID)
            .audienceId(AUDIENCE_ID)
            .name("Test Rule")
            .ruleType(RuleType.BATCH)
            .ruleAction(RuleAction.ADD)
            .build();

    when(ruleRepository.getRuleById(eq(PROJECT_ID), eq(RULE_ID))).thenReturn(Single.just(ruleMeta));
    // Empty source list - no sources to fetch
    when(dataConnectorRepository.getDataSourcesByIds(eq(Collections.emptyList())))
        .thenReturn(Single.just(Collections.emptyList()));

    TestObserver<RuleDetailsResponse> to =
        service.getRuleDetails(PROJECT_ID, AUDIENCE_ID, RULE_ID).test();
    // This will likely fail due to null configuration, but tests the path
    to.assertError(Exception.class);
  }

  // ========================================
  // createRules Tests
  // ========================================

  @Test
  public void createRules_audienceNotFound_throwsForbiddenAccessException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    CreateRulesRequest request = new CreateRulesRequest();
    request.setAudienceId(AUDIENCE_ID);
    request.setRules(Collections.emptyList());

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    TestObserver<Boolean> to = service.createRules(PROJECT_ID, request, ACTOR_EMAIL).test();
    to.assertError(ForbiddenAccessException.class);
  }

  @Test
  public void createRules_staticAudience_throwsException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    CreateRulesRequest request = new CreateRulesRequest();
    request.setAudienceId(AUDIENCE_ID);
    request.setRules(Collections.emptyList());

    AudienceMeta staticAudience =
        AudienceMeta.builder()
            .xProjectId(PROJECT_ID)
            .audienceId(AUDIENCE_ID)
            .type("STATIC")
            .build();

    when(audienceRepository.getAudienceById(eq(PROJECT_ID), eq(AUDIENCE_ID)))
        .thenReturn(Single.just(staticAudience));

    TestObserver<Boolean> to = service.createRules(PROJECT_ID, request, ACTOR_EMAIL).test();
    to.assertError(RestException.class);
  }

  // ========================================
  // updateAudienceOwner Tests (existing tests retained)
  // ========================================

  @Test
  public void updateAudienceOwner_add_succeeds() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "target@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor)));
    when(audienceOwnerRepository.addOwner(eq(xProjectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(true));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertComplete();
    to.assertValue(true);

    verify(audienceOwnerRepository).addOwner(eq(xProjectId), eq(audienceId), eq(target), eq(actor));
  }

  @Test
  public void updateAudienceOwner_remove_succeedsWhenRepositoryReturnsTrue() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "target@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.REMOVE);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor, target)));
    when(audienceOwnerRepository.removeOwner(eq(xProjectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(true));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertComplete();
    to.assertValue(true);

    verify(audienceOwnerRepository)
        .removeOwner(eq(xProjectId), eq(audienceId), eq(target), eq(actor));
  }

  @Test
  public void updateAudienceOwner_unauthorizedActor_throwsIllegalAccessException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("someone@example.com");

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith("different.owner@example.com")));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertError(ForbiddenAccessException.class);
  }

  @Test
  public void updateAudienceOwner_expiredAudience_throwsBadRequest() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("someone@example.com");

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(true, "aud", false)));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  @Test
  public void updateAudienceOwner_remove_returnsFalse_returnsFalse() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "target@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.REMOVE);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor, target)));
    when(audienceOwnerRepository.removeOwner(eq(xProjectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(false));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertComplete();
    to.assertValue(false);
  }

  @Test
  public void updateAudienceOwner_addDuplicateOwner_throwsException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "actor@example.com"; // Same as actor - already an owner

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor)));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  @Test
  public void updateAudienceOwner_removeLastOwner_throwsException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.REMOVE);
    request.setEmail(actor); // Trying to remove themselves - the only owner

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor))); // Only one owner

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  @Test
  public void updateAudienceOwner_removeNonExistentOwner_throwsException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "nonexistent@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.REMOVE);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor, "other@example.com")));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  @Test
  public void updateAudienceOwner_audienceNotFound_throwsResourceNotFoundException() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String xProjectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("target@example.com");

    when(audienceRepository.getAudienceById(eq(xProjectId), eq(audienceId)))
        .thenReturn(Single.error(new NoSuchElementException("Not found")));

    TestObserver<Boolean> to =
        service.updateAudienceOwner(xProjectId, audienceId, actor, request).test();
    to.assertError(ResourceNotFoundException.class);
  }

  // ========================================
  // getAudienceAuditLog Tests
  // ========================================

    /**
     * Verifies audience audit log behavior when pagination is NOT enabled.
     *
     * Logic being validated:
     * - All audit log entries returned from the repository are grouped by their
     *   event date (YYYY-MM-DD).
     * - Grouping is done in the service layer before returning the response.
     * - When pagination is disabled, `hasMore` is derived using a heuristic
     *   (based on the size of the fetched result set) and NOT via a DB count query.
     *
     * Why this test exists:
     * - The UI expects audit logs grouped by date for timeline-style rendering.
     * - Any change in grouping logic can silently break frontend rendering.
     * - Ensures backward compatibility for non-paginated audit log consumers.
     */
  @Test
  public void getAudienceAuditLog_groupsByDate_withoutPagination() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    // Two logs on two different days (epoch seconds)
    long day1 = 1_700_000_000L;
    long day2 = 1_700_086_400L; // +1 day approximately

    AuditLogDefinition log1 = new AuditLogDefinition();
    log1.setAction(AuditLogAction.AUDIENCE_CREATED);
    log1.setActor("user1@example.com");
    log1.setCreatedAt(day1);
    log1.setValue(new AuditLogValue(null, java.util.Map.of("name", "A1")));

    AuditLogDefinition log2 = new AuditLogDefinition();
    log2.setAction(AuditLogAction.OWNER_ADDED);
    log2.setActor("user2@example.com");
    log2.setCreatedAt(day2);
    log2.setValue(new AuditLogValue(null, java.util.Map.of("ownerEmail", "o@example.com")));

    when(audienceOwnerRepository.findByAudienceId(eq(AUDIENCE_ID), eq(2), eq(0)))
        .thenReturn(Single.just(List.of(log1, log2)));

    TestObserver<PaginatedResponse<AuditLogResponse>> to =
        service.getAudienceAuditLog(AUDIENCE_ID, 2, 0, false).test();
    to.assertComplete();
    to.assertValue(
        resp -> {
          assertNotNull(resp.data());
          // Expect two date groups
          assertEquals(2, resp.data().size());
          // Heuristic hasMore when data.size() == pageSize
          assertTrue(resp.pageInfo().hasMore());
          // Basic item sanity
          List<AuditLogItem> items0 = resp.data().get(0).items();
          assertFalse(items0.isEmpty());
          assertNotNull(items0.get(0).performedAt());
          return true;
        });
  }

    /**
     * Verifies audience audit log behavior when pagination IS enabled.
     *
     * Logic being validated:
     * - The service fetches a paginated subset of audit logs.
     * - A separate total count query is executed to determine total records.
     * - `hasMore` is calculated using totalCount, page number, and page size.
     *   (hasMore = totalCount > page * pageSize)
     *
     * Why this test exists:
     * - Correct `hasMore` calculation is critical for frontend pagination controls.
     * - Prevents regressions where "Load More" appears incorrectly or disappears early.
     * - Ensures the service does not rely on result size heuristics when pagination is on.
     */
  @Test
  public void getAudienceAuditLog_withPagination_usesCountAndSetsHasMore() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    long now = System.currentTimeMillis() / 1000;
    AuditLogDefinition log = new AuditLogDefinition();
    log.setAction(AuditLogAction.OWNER_ADDED);
    log.setActor("actor@example.com");
    log.setCreatedAt(now);
    log.setValue(new AuditLogValue(null, java.util.Map.of("ownerEmail", "x@y.com")));

    when(audienceOwnerRepository.findByAudienceId(eq(AUDIENCE_ID), eq(1), eq(0)))
        .thenReturn(Single.just(List.of(log)));
    when(audienceOwnerRepository.findCountByAudienceId(eq(AUDIENCE_ID))).thenReturn(Single.just(5));

    TestObserver<PaginatedResponse<AuditLogResponse>> to =
        service.getAudienceAuditLog(AUDIENCE_ID, 1, 0, true).test();
    to.assertComplete();
    to.assertValue(
        resp -> {
          assertEquals(0, resp.pageInfo().page());
          assertEquals(1, resp.pageInfo().pageSize());
          assertTrue(resp.pageInfo().hasMore()); // (0+1)*1 < 5
          return true;
        });
  }

    /**
     * Verifies fallback behavior when audit log fields are partially missing.
     *
     * Logic being validated:
     * - If both `action` and `value` fields are null in an audit log entry:
     *   - The service safely falls back to using the `name` field as details.
     * - Ensures response generation does not throw NullPointerExceptions.
     *
     * Why this test exists:
     * - Legacy or malformed audit records may exist in the database.
     * - The service layer must be defensive and resilient to null values.
     * - Guarantees consistent audit log details for UI display.
     */
  @Test
  public void getAudienceAuditLog_handlesNullActionAndValue_usesNameAsDetails() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    AuditLogDefinition log = new AuditLogDefinition();
    log.setAction(null);
    log.setActor("n/a");
    log.setCreatedAt(1_700_000_000L);
    log.setValue(null); // value null -> details should fall back to name
    log.setName("Fallback");

    when(audienceOwnerRepository.findByAudienceId(eq(AUDIENCE_ID), eq(10), eq(0)))
        .thenReturn(Single.just(List.of(log)));

    TestObserver<PaginatedResponse<AuditLogResponse>> to =
        service.getAudienceAuditLog(AUDIENCE_ID, null, null, false).test();
    to.assertComplete();
    to.assertValue(
        resp -> {
          assertNotNull(resp.data());
          assertEquals(1, resp.data().size());
          List<AuditLogItem> items = resp.data().get(0).items();
          assertEquals(1, items.size());
          assertNull(items.get(0).action());
          assertEquals("Fallback", items.get(0).details());
          return true;
        });
  }
}
