package io.ascend.flockr.admin.service.impl;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceOwner;
import io.ascend.flockr.admin.domain.audit.AuditLogAction;
import io.ascend.flockr.admin.domain.audit.AuditLogDefinition;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerAction;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.io.response.AuditLogResponse;
import io.ascend.flockr.admin.io.response.PaginatedResponse;
import io.ascend.flockr.admin.repository.AudienceOwnerRepository;
import io.ascend.flockr.admin.repository.AudienceRepository;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class AudienceServiceImplTest {

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
        .tenantId("t1")
        .projectId("p1")
        .audienceId(42L)
        .name(name)
        .verified(verified)
        .expireDate(expireAt)
        .build();
  }

  private static List<AudienceOwner> ownersWith(String... emails) {
    List<AudienceOwner> list = new ArrayList<>();
    for (String email : emails) {
      AudienceOwner o = new AudienceOwner();
      o.setOwnerEmail(email);
      o.setStatus("ACTIVE");
      list.add(o);
    }
    return list;
  }

  @Test
  public void updateAudienceOwner_add_succeeds() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String tenantId = "t1";
    String projectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "target@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor)));
    when(audienceOwnerRepository.addOwner(
            eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(true));

    TestObserver<Void> to =
        service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertComplete();

    verify(audienceOwnerRepository)
        .addOwner(eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor));
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

    String tenantId = "t1";
    String projectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "target@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.REMOVE);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor, target)));
    when(audienceOwnerRepository.removeOwner(
            eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(true));

    TestObserver<Void> to =
        service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertComplete();

    verify(audienceOwnerRepository)
        .removeOwner(eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor));
  }

  @Test
  public void updateAudienceOwner_unauthorizedActor_throwsForbidden() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String tenantId = "t1";
    String projectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("someone@example.com");

    when(audienceRepository.getAudienceById(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith("different.owner@example.com")));

    TestObserver<Void> to =
        service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
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

    String tenantId = "t1";
    String projectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("someone@example.com");

    when(audienceRepository.getAudienceById(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(true, "aud", false)));

    TestObserver<Void> to =
        service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  @Test
  public void updateAudienceOwner_remove_returnsFalse_emitsError() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String tenantId = "t1";
    String projectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";
    String target = "target@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.REMOVE);
    request.setEmail(target);

    when(audienceRepository.getAudienceById(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(false, "aud", false)));
    when(audienceOwnerRepository.findOwners(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(ownersWith(actor, target)));
    when(audienceOwnerRepository.removeOwner(
            eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(false));

    TestObserver<Void> to =
        service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  // ---------- Audit Log Tests ----------

  @Test
  public void getAudienceAuditLog_withoutPagination_groupsByDateAndHeuristicHasMore() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    Long audienceId = 123L;
    int pageSize = 2;
    int pageNum = 0;
    boolean withPagination = false;

    // Create three logs: two on the same day (day0), one on previous day (day1)
    long nowSec = System.currentTimeMillis() / 1000;
    long day0 = nowSec; // same day bucket
    long day0_2 = nowSec - 60; // same day bucket
    long day1 = nowSec - 24 * 3600; // previous day bucket

    AuditLogDefinition l1 =
        new AuditLogDefinition(
            1L,
            audienceId,
            null,
            AuditLogAction.OWNER_ADDED,
            null,
            "u1@example.com",
            day0,
            "n1",
            null,
            null);
    AuditLogDefinition l2 =
        new AuditLogDefinition(
            2L,
            audienceId,
            null,
            AuditLogAction.OWNER_REMOVED,
            null,
            "u2@example.com",
            day0_2,
            "n2",
            null,
            null);
    AuditLogDefinition l3 =
        new AuditLogDefinition(
            3L,
            audienceId,
            null,
            AuditLogAction.AUDIENCE_CREATED,
            null,
            "u3@example.com",
            day1,
            "n3",
            null,
            null);

    // Repo returns most-recent-first limited by pageSize=2 -> l1,l2
    when(audienceOwnerRepository.findByAudienceId(eq(audienceId), eq(pageSize), eq(0)))
        .thenReturn(Single.just(List.of(l1, l2)));

    TestObserver<PaginatedResponse<AuditLogResponse>>
        to =
            service.getAudienceAuditLog(audienceId, pageSize, pageNum, withPagination).test();
    to.assertNoErrors();
    var resp = to.values().get(0);

    // Expect 1 date group (both items same day)
    Assert.assertEquals(1, resp.data().size());
    Assert.assertEquals(pageNum, resp.pageInfo().page());
    Assert.assertEquals(pageSize, resp.pageInfo().pageSize());
    // Heuristic hasMore is based on number of groups, not raw rows.
    // Here groups == 1 and pageSize == 2 -> hasMore should be false.
    Assert.assertFalse(resp.pageInfo().hasMore());
    // Items preserve encounter order
    Assert.assertEquals(2, resp.data().get(0).items().size());
    Assert.assertEquals("u1@example.com", resp.data().get(0).items().get(0).performedBy());
    Assert.assertEquals("u2@example.com", resp.data().get(0).items().get(1).performedBy());
  }

  @Test
  public void getAudienceAuditLog_withPagination_computesHasMoreFromTotalCount() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(
            audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    Long audienceId = 123L;
    int pageSize = 2;
    int pageNum = 0;
    boolean withPagination = true;

    long nowSec = System.currentTimeMillis() / 1000;
    AuditLogDefinition l1 =
        new AuditLogDefinition(
            1L,
            audienceId,
            null,
            AuditLogAction.OWNER_ADDED,
            null,
            "u1@example.com",
            nowSec,
            "n1",
            null,
            null);
    AuditLogDefinition l2 =
        new AuditLogDefinition(
            2L,
            audienceId,
            null,
            AuditLogAction.OWNER_REMOVED,
            null,
            "u2@example.com",
            nowSec - 60,
            "n2",
            null,
            null);

    when(audienceOwnerRepository.findByAudienceId(eq(audienceId), eq(pageSize), eq(0)))
        .thenReturn(Single.just(List.of(l1, l2)));
    when(audienceOwnerRepository.findCountByAudienceId(eq(audienceId)))
        .thenReturn(Single.just(5)); // total > (page+1)*pageSize -> hasMore

    TestObserver<PaginatedResponse<AuditLogResponse>>
        to =
            service.getAudienceAuditLog(audienceId, pageSize, pageNum, withPagination).test();
    to.assertNoErrors();
    var resp = to.values().get(0);

    Assert.assertEquals(pageNum, resp.pageInfo().page());
    Assert.assertEquals(pageSize, resp.pageInfo().pageSize());
    Assert.assertTrue(resp.pageInfo().hasMore()); // (0+1)*2 < 5
    // One date group as both are same day bucket
    Assert.assertEquals(1, resp.data().size());
  }
}
