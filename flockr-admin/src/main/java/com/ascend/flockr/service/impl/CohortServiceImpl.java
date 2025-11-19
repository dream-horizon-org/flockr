package com.ascend.flockr.service.impl;

import com.ascend.flockr.domain.cohort.Cohort;
import com.ascend.flockr.domain.cohort.CohortOwner;
import com.ascend.flockr.io.request.UpdateCohortOwnerAction;
import com.ascend.flockr.io.request.UpdateCohortOwnerRequest;
import com.ascend.flockr.repository.CohortRepository;
import com.ascend.flockr.service.CohortService;
import com.google.inject.Inject;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CohortServiceImpl implements CohortService {

  private final CohortRepository cohortRepository;

    /**
     * Adds or removes a cohort owner.
     *
     * <p>Preconditions:
     * - The cohort must exist and must not be expired.
     * - The acting user (from {@code userEmail}) must already be an owner of the cohort.
     *
     * <p>Behavior:
     * - When {@code action == add}, inserts the target owner if not already present.
     * - When {@code action == remove}, marks the target owner as removed, preventing removal of the last owner.
     *
     * <p>Postconditions:
     * - Returns a completed {@link io.reactivex.rxjava3.core.Completable} on success.
     * - Emits an {@link IllegalStateException} if the cohort is expired or the user is unauthorized.
     * - Emits an {@link IllegalStateException} if the update results in no changes.
     *
     * @param cohortId the identifier of the cohort to update
     * @param userEmail the acting user's email (must already be a cohort owner)
     * @param req the request containing the action (add/remove) and the target owner email
     * @return a {@link io.reactivex.rxjava3.core.Completable} that completes on success or errors on failure
     */
  @Override
  public Completable updateCohortOwner(Long cohortId, String userEmail, UpdateCohortOwnerRequest req) {
    return cohortRepository
        .findById(cohortId)
        .flatMap(
            (Cohort cohort) -> {
              if (Boolean.TRUE.equals(cohort.getExpired())) {
                return Single.error(new IllegalStateException("cohort expired"));
              }
              return cohortRepository
                  .findOwners(cohortId)
                  .flatMap(
                      owners -> {
                          // Checking if logged-in user has permission
                        boolean authorized =
                            owners.stream().anyMatch(o -> o.getOwner().equals(userEmail));
                        if (!authorized) {
                          return Single.error(
                              new IllegalStateException("user not authorized to update cohort"));
                        }
                        if (req.getAction() == UpdateCohortOwnerAction.add) {
                          List<String> verifiers = List.of();
                          return validateAndAddOwner(
                              owners,
                              cohortId,
                              req,
                              userEmail,
                              cohort.getName(),
                              cohort.getVerified(),
                              verifiers);
                        }
                        return validateAndRemoveOwner(owners, cohortId, req, userEmail);
                      });
            })
        .flatMapCompletable(
            ok ->
                ok
                    ? Completable.complete()
                    : Completable.error(new IllegalStateException("owner update failed")));
  }

    /**
     * Validates and adds a new owner to the cohort.
     *
     * <p>Validations:
     * - No duplicate owners.
     * - Optional verifier checks (if enabled via configuration).
     *
     * @param existingOwners current owners
     * @param cohortId cohort identifier
     * @param request request containing target owner email
     * @param performedBy acting user's email (recorded as {@code added_by})
     * @param cohortName cohort display name (for audit/logging)
     * @param isVerified whether the cohort is verified (may enforce stricter rules)
     * @param verifiers optional list of allowed verifier emails
     * @return a {@link io.reactivex.rxjava3.core.Single} emitting {@code true} if an insert occurred
     */

  private Single<Boolean> validateAndAddOwner(
      List<CohortOwner> existingOwners,
      Long cohortId,
      UpdateCohortOwnerRequest request,
      String performedBy,
      String cohortName,
      Boolean isVerified,
      List<String> verifiers) {

    return cohortRepository.addOwner(cohortId, request.getEmail(), performedBy);
  }

    /**
     * Validates and removes an existing owner from the cohort.
     *
     * <p>Validations:
     * - Target owner must exist.
     * - Must not remove the last remaining owner.
     *
     * @param existingOwners current owners
     * @param cohortId cohort identifier
     * @param request request containing target owner email
     * @param performedBy acting user's email (recorded as {@code removed_by})
     * @return a {@link io.reactivex.rxjava3.core.Single} emitting {@code true} if an update occurred
     */

  private Single<Boolean> validateAndRemoveOwner(
      List<CohortOwner> existingOwners,
      Long cohortId,
      UpdateCohortOwnerRequest request,
      String performedBy) {

    return cohortRepository.removeOwner(cohortId, request.getEmail(), performedBy);
  }
}
