package com.ascend.flockr.service.impl;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascend.flockr.domain.audience.AudienceMeta;
import com.ascend.flockr.domain.audienceOwner.AudienceOwner;
import com.ascend.flockr.io.request.UpdateAudienceOwnerAction;
import com.ascend.flockr.io.request.UpdateAudienceOwnerRequest;
import com.ascend.flockr.repository.AudienceOwnerRepository;
import com.ascend.flockr.repository.AudienceRepository;
import com.ascend.flockr.repository.DataConnectorRepository;
import com.ascend.flockr.repository.RuleRepository;
import com.dream11.rest.exception.RestException;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.List;
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
      o.setOwner(email);
      o.setIsRemoved(false);
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
        buildService(audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

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
    when(audienceOwnerRepository.addOwner(eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(true));

    TestObserver<Void> to = service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
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
        buildService(audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

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
    when(audienceOwnerRepository.removeOwner(eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(true));

    TestObserver<Void> to = service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
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
        buildService(audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

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

    TestObserver<Void> to = service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  @Test
  public void updateAudienceOwner_expiredAudience_throwsBadRequest() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

    String tenantId = "t1";
    String projectId = "p1";
    Long audienceId = 42L;
    String actor = "actor@example.com";

    UpdateAudienceOwnerRequest request = new UpdateAudienceOwnerRequest();
    request.setAction(UpdateAudienceOwnerAction.ADD);
    request.setEmail("someone@example.com");

    when(audienceRepository.getAudienceById(eq(tenantId), eq(projectId), eq(audienceId)))
        .thenReturn(Single.just(buildAudienceMeta(true, "aud", false)));

    TestObserver<Void> to = service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }

  @Test
  public void updateAudienceOwner_remove_returnsFalse_emitsError() {
    AudienceRepository audienceRepository = mock(AudienceRepository.class);
    AudienceOwnerRepository audienceOwnerRepository = mock(AudienceOwnerRepository.class);
    RuleRepository ruleRepository = mock(RuleRepository.class);
    DataConnectorRepository dataConnectorRepository = mock(DataConnectorRepository.class);
    AudienceServiceImpl service =
        buildService(audienceRepository, audienceOwnerRepository, ruleRepository, dataConnectorRepository);

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
    when(audienceOwnerRepository.removeOwner(eq(tenantId), eq(projectId), eq(audienceId), eq(target), eq(actor)))
        .thenReturn(Single.just(false));

    TestObserver<Void> to = service.updateAudienceOwner(tenantId, projectId, audienceId, actor, request).test();
    to.assertError(RestException.class);
  }
}


