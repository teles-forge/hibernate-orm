# Current auditor upstream contribution plan

> Fork-only working note for `prototype/current-auditor`.
> Remove this file before submitting an upstream Hibernate ORM pull request.

This file records the upstream workflow, the proposed Jira content, the reviewer-risk audit, and the checks that must be completed before a PR is opened.

## Current upstream context

Quarkus discussion:

https://github.com/quarkusio/quarkus/issues/54724

Original standalone implementation:

https://github.com/TelesNascimento/quarkus-panache-audit

Hibernate ORM prototype:

https://github.com/teles-forge/hibernate-orm/tree/prototype/current-auditor

The Quarkus discussion identified the remaining design problem as obtaining the current user without making Hibernate ORM depend on Quarkus Security. The current prototype deliberately models an auditor instead of a current user. Hibernate does not know where the auditor comes from.

## Jira account / access

Hibernate ORM requires a Jira issue for code contributions.

Project:

Hibernate ORM

Project key:

HHH

Issue tracker:

https://hibernate.atlassian.net/jira/software/c/projects/HHH/issues

If the Create button does not work even though Atlassian appears logged in:

1. Open https://hibernate.atlassian.net/jira/for-you
2. Open the account menu in the top-right.
3. Click Log in from the Hibernate Jira site itself.
4. Complete email verification if requested.
5. Accept the prompt to join the Hibernate Jira site.
6. Refresh and try Create again.
7. If the account is new and still cannot create issues, retry after account activation has completed.

This is a known Jira Cloud onboarding problem reported by Hibernate contributors in 2025-2026. Do not create a GitHub PR only to work around a missing Jira ticket unless a Hibernate maintainer explicitly asks for that.

## Jira issue: fields

The Jira UI can change. Use these values for the fields that are present and do not invent values for optional fields that Hibernate maintainers normally control.

### Project

Hibernate ORM (HHH)

### Issue type

Recommended: New Feature

Fallback if a maintainer specifically asks for it: Improvement

Do not use Bug. This is new behavior, not a regression.

### Summary

Add @CreatedBy and @LastModifiedBy auditor value generation

### Description

Hibernate ORM already provides @CreationTimestamp and @UpdateTimestamp for generated audit timestamps, but there is no built-in equivalent for recording the actor responsible for creating or modifying an entity.

This originally came up in Quarkus while discussing @CreatedBy / @LastModifiedBy support:

https://github.com/quarkusio/quarkus/issues/54724

The existing prototype is not Panache-specific. It is based on Hibernate ORM's BeforeExecutionGenerator mechanism and the remaining problem is intentionally kept independent from any security framework.

The proposed model is:

* @CreatedBy generates an auditor value on INSERT.
* @LastModifiedBy generates an auditor value on INSERT and UPDATE.
* A small generic CurrentAuditorResolver<T> supplies the auditor for the current persistence operation.
* Hibernate ORM does not define what a "current user" is and does not depend on CDI, Quarkus Security, or another authentication API.
* The application decides where the auditor comes from, for example an authenticated user, a service account, or a scheduled job.
* The auditor type is not restricted to String.
* Using the annotations without configuring a resolver fails during mapping/bootstrap.
* A configured resolver may return null to indicate that there is no auditor for that operation; the prototype currently leaves the existing property value unchanged in that case.
* A non-null auditor replaces the current generated-property value.
* Resolver implementations configured by class/class name are acquired through Hibernate's managed bean registry so their lifecycle is shared instead of creating one resolver per audited property.

The implementation uses @ValueGenerationType and BeforeExecutionGenerator, following the same value-generation infrastructure already used by Hibernate's built-in generators.

Prototype:

https://github.com/teles-forge/hibernate-orm/tree/prototype/current-auditor

Open design points I would like feedback on before finalizing the API:

* whether CurrentAuditorResolver is the preferred name/package for the SPI;
* whether returning null should leave the current value unchanged;
* whether missing resolver configuration should remain a bootstrap mapping error.

### Priority

Leave the Jira default unless a maintainer changes it.

### Assignee

Unassigned.

Do not assign a Hibernate maintainer manually.

### Affects Version/s

Leave empty. This is not a regression in an existing release.

### Fix Version/s

Leave empty. Maintainers/release managers should choose this.

The prototype currently targets main, which is Hibernate ORM 8.1.0-SNAPSHOT, but that does not mean the Jira ticket should claim a fix version.

### Component/s

Leave empty unless the Jira form requires a value or a maintainer tells us which component to use.

Do not guess a component just to fill the field.

### Labels

Leave empty unless the Jira form or maintainer asks for a label.

### Environment

Not applicable / leave empty.

### Links

Add the Quarkus issue and prototype URL in the description. If Jira exposes an external-link field, those same links may be added there too.

## After Jira creates HHH-XXXXX

Replace HHH-XXXXX below with the real key.

Branch convention:

HHH-XXXXX-current-auditor

Final commit message:

HHH-XXXXX Add current auditor value generation

The final commit must be signed off for the DCO using the contributor's configured Git identity.

Every test class added for the feature should reference the ticket with:

@JiraKey("HHH-XXXXX")

Do not leave a fake HHH number in source before the real ticket exists.

## Public API / SPI intended by the prototype

Application API:

* org.hibernate.annotations.CreatedBy
* org.hibernate.annotations.LastModifiedBy

Integration SPI:

* org.hibernate.context.spi.CurrentAuditorResolver<T>

Internal implementation:

* org.hibernate.generator.internal.CurrentAuditorGeneration
* generated annotation-model implementations under org.hibernate.boot.models.annotations.internal

Configuration:

hibernate.audit.current_auditor_resolver

The resolver SPI is incubating and classified for IMPLEMENT and SUPPLY. The generator is internal.

## Semantics currently covered

* CreatedBy: INSERT only.
* LastModifiedBy: INSERT and UPDATE.
* CreatedBy is not changed on later updates.
* LastModifiedBy is not changed by a flush that produces no entity update.
* A non-null auditor overrides an explicitly assigned generated-property value.
* A null auditor leaves the current property value unchanged.
* Missing resolver with either audit annotation is a bootstrap MappingException.
* No resolver is required when no audit annotations are used.
* Resolver configuration accepts an instance, implementation Class, or implementation class name.
* Class-based resolver lifecycle is managed/reused through ManagedBeanRegistry.
* String is not special; a Long auditor is tested.
* A resolver value incompatible with the annotated property's Java type fails with a PropertyValueException.
* Field access is covered.
* Property/getter access is covered.
* Mapped-superclass audit fields are covered.
* Stateful Session is covered.
* StatelessSession is covered.
* Merge of a detached entity is covered.

## Deliberate scope limits

Do not claim association-valued auditing such as @ManyToOne User createdBy.

Hibernate's generated-property model documented in the user guide currently applies to basic and version attributes. The upstream proposal should not silently expand generator semantics to associations.

Do not add a Hibernate concept of current user.

Do not add SecurityIdentity, Spring Security, CDI-specific APIs, or fallback magic values such as "system" or "anonymous" to Hibernate core.

Do not add a large fluent/bootstrap API unless maintainers ask for it. The configuration setting is enough for the initial proposal.

## Reviewer-risk audit

### Yoann Rodière

Likely concerns:

* API is larger than required.
* Quarkus-specific assumptions leaked upstream.
* public contract exposes information that no current use case needs.
* an existing Hibernate mechanism should be reused instead.

Current mitigation:

* one-method resolver;
* no Session parameter in the resolver;
* no Quarkus dependency;
* generic auditor type;
* @ValueGenerationType / BeforeExecutionGenerator reuse;
* incubating SPI.

### Steve Ebersole

Likely concerns:

* resolver ownership/lifecycle;
* bootstrap path vs runtime path;
* configuration variants not really exercised;
* tests pass without constructing or using the real SessionFactory;
* responsibility split is unclear.

Current mitigation:

* class/class-name resolvers are acquired through ManagedBeanRegistry using a bootstrap-safe managed bean;
* an instance setting is used directly;
* configuration tests actually build a SessionFactory;
* instance, Class, and class-name settings are covered;
* lifecycle test verifies one managed resolver instance is reused across multiple generated properties;
* runtime generation is tested through Session, StatelessSession, and merge.

### Christian Beikov

Likely concerns:

* Jira linkage;
* formatting/Spotless;
* missing focused tests;
* poor failure messages;
* configuration permutations.

Before PR:

* create Jira;
* add @JiraKey to tests;
* run formatChecks;
* keep explicit bootstrap/type-mismatch tests.

### Gavin King

Likely concerns:

* semantics encoded at the wrong layer;
* behavior that only works accidentally;
* unnecessary convenience/API;
* unclear annotation naming or behavior.

Current mitigation:

* no Security/user semantics in the generator;
* timing follows Generator event sets;
* no interpretation of @Column insertable/updatable flags;
* null/no-auditor behavior is explicit and documented instead of accidental;
* wrong auditor type fails explicitly.

## Annotation model registration

New Hibernate annotations are not complete just because the Java annotation exists.

The branch must also contain:

* CreatedByAnnotation
* LastModifiedByAnnotation
* CREATED_BY descriptor in HibernateAnnotations
* LAST_MODIFIED_BY descriptor in HibernateAnnotations

This mirrors existing built-in annotations such as CreationTimestamp and prevents the new annotations from being absent from Hibernate's annotation model.

## Documentation required

This is a public feature, so documentation is part of the change.

Current branch updates:

* documentation/src/main/asciidoc/userguide/chapters/domain/basic_types.adoc
* documentation/src/main/asciidoc/introduction/Advanced.adoc
* Javadocs on the two annotations, resolver, and setting

The user guide must explain:

* INSERT vs INSERT+UPDATE timing;
* how the resolver is configured;
* Hibernate does not define a current user;
* null-auditor semantics;
* missing-resolver semantics;
* type requirements;
* generated-property scope.

Do not add release-note or migration-guide text unless maintainers request it. This is a new feature with no migration requirement.

## Required verification before PR

Run from a clean checkout after rebasing on current upstream main.

Focused tests:

./gradlew :hibernate-core:test --tests "org.hibernate.orm.test.mapping.generated.CurrentAuditor*"

Formatting / style:

./gradlew spotlessApply
./gradlew formatChecks

Public API / SPI classification:

./gradlew :documentation:generateClassificationMetadata
./gradlew :documentation:validateClassifications
./gradlew :documentation:validateSpi
./gradlew :documentation:validateMigrationCompatibility

Documentation:

./gradlew :documentation:buildDocs

Full repository verification requested by CONTRIBUTING.md:

./gradlew clean build

Do not claim any of these passed until they were actually run.

## Final-history cleanup before PR

1. Rebase on the latest upstream main. Do not merge main into the topic branch.
2. Create/use an HHH-keyed topic branch.
3. Add the real @JiraKey("HHH-XXXXX") to the tests.
4. Remove this CURRENT_AUDITOR_CONTRIBUTION_PLAN.md file.
5. Squash prototype/checkpoint commits into the final logical commit unless a maintainer asks for a different split.
6. Make the final commit start with the Jira key.
7. Include the DCO Signed-off-by trailer.
8. Re-run focused tests, formatChecks, classification validation, docs, and clean build.
9. Inspect the final diff for prototype notes or unrelated files.
10. Only then open a PR, and only after the upstream direction is confirmed.

## PR description when the time comes

Do not open the PR yet.

Keep the future PR description short and technical. Suggested content after HHH-XXXXX exists:

Implements HHH-XXXXX.

Adds @CreatedBy and @LastModifiedBy value generation backed by a generic CurrentAuditorResolver.

The ORM side does not define a current user or depend on a security framework. Applications provide the auditor source. @CreatedBy runs on insert and @LastModifiedBy runs on insert/update.

Includes bootstrap/configuration, lifecycle, type, stateful/stateless, property-access, inheritance, merge and null-auditor coverage, plus user-guide documentation.

Related Quarkus discussion:
https://github.com/quarkusio/quarkus/issues/54724

Keep the license text from Hibernate's pull request template unchanged.
