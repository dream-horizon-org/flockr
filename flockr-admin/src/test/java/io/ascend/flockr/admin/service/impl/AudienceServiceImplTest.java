package io.ascend.flockr.admin.service.impl;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dream11.rest.exception.RestException;
import io.ascend.flockr.admin.domain.audience.AudienceMeta;
import io.ascend.flockr.admin.domain.audience.AudienceOwner;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerAction;
import io.ascend.flockr.admin.io.request.UpdateAudienceOwnerRequest;
import io.ascend.flockr.admin.repository.AudienceOwnerRepository;
import io.ascend.flockr.admin.repository.AudienceRepository;
import io.ascend.flockr.admin.repository.DataConnectorRepository;
import io.ascend.flockr.admin.repository.RuleRepository;
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
        .xProjectId("p1")
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
    to.assertError(IllegalAccessException.class);
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
}
