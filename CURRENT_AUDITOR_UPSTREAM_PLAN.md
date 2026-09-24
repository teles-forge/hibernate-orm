# Current auditor upstream contribution plan

This file is a working checklist for the current-auditor prototype. It is not intended to be part of the final Hibernate ORM pull request.

## 1. Create the Hibernate Jira issue first

Hibernate ORM's contribution guide requires a corresponding Jira issue for code contributions. The Jira key must prefix the final commit message, and Hibernate's automation links the pull request back to that issue.

Project:
Hibernate ORM

Project key:
HHH

Issue type:
Use **New Feature** if that option is available for the HHH project. If the create screen only offers **Improvement** for this kind of work, use Improvement instead. Do not use Bug: this is new functionality.

Summary:
Add @CreatedBy and @LastModifiedBy value generation

Suggested description:

Hibernate ORM already supports creation/update timestamps and custom value generation through `@ValueGenerationType` / `BeforeExecutionGenerator`, but there is no built-in equivalent for recording the actor responsible for an insert or update.

The original use case came from Quarkus, where an external extension currently resolves the authenticated actor from Quarkus Security. During discussion in Quarkus it became clear that the ORM-level contract should not depend on a security framework or even model a "current user" specifically.

The proposed direction is to let Hibernate ask for an application-provided current auditor value:

- `@CreatedBy` generates the auditor on INSERT.
- `@LastModifiedBy` generates the auditor on INSERT and UPDATE.
- `CurrentAuditorResolver<T>` supplies the auditor for the current persistence operation.
- Hibernate makes no assumption about the source of that value. It may come from a security context, service account, scheduled job, or other application-specific context.
- Returning `null` means no auditor is available for that operation and leaves the existing property value unchanged.
- Using the annotations without configuring a resolver is treated as a configuration error.

This is implemented using Hibernate's existing before-execution value-generation infrastructure rather than a new persistence lifecycle mechanism.

Quarkus discussion:
https://github.com/quarkusio/quarkus/issues/54724

Prototype:
https://github.com/teles-forge/hibernate-orm/tree/prototype/current-auditor

The prototype currently covers regular Session operations, StatelessSession, merge, field and property access, mapped-superclass attributes, generic auditor value types, configuration validation, missing-auditor behavior, and resolver lifecycle through Hibernate's managed bean registry.

Open design points I would like maintainer feedback on before treating the API as final:

1. Whether `CurrentAuditorResolver` is the preferred name/package for the SPI.
2. Whether a missing configured resolver should fail during bootstrap, as in the prototype.
3. Whether `null` from a configured resolver should preserve the current property value.
4. Whether the configuration property is sufficient as the supply point or a bootstrap builder method is also desired.

Expected result:
Hibernate ORM can populate actor/auditor properties without knowing anything about authentication, CDI, Quarkus Security, Spring Security, or any other identity source.

Components:
If Jira requires a component and there is a Hibernate Core / core component, select it. Do not select Envers: this feature is value generation on ordinary entity state and is independent of Envers.

Affects Version/s:
Leave empty for a new feature unless the Jira form requires a value.

Fix Version/s:
Leave empty. Maintainers normally decide target release/backport.

Priority:
Leave the default unless a maintainer explicitly asks for another priority.

Assignee:
Leave unassigned.

Labels:
Do not invent labels. Leave empty unless a maintainer asks for one.

Environment:
Not applicable for the feature request. Leave empty unless Jira requires text.

Links:
Include the Quarkus issue and prototype branch in the description instead of creating unrelated issue links.

## 2. After Jira creates HHH-XXXXX

Rename or recreate the working topic branch using the Jira key, for example:

`HHH-XXXXX-created-by-last-modified-by`

The project says including the Jira key in the branch name is conventional but not mandatory.

Final commit message should start with the Jira key. Recommended form:

`HHH-XXXXX Add current auditor value generation`

Before submission, squash the prototype history into logical commit(s). For this change, one focused commit is preferred unless maintainers request a split.

Add `@JiraKey("HHH-XXXXX")` to the focused regression/feature tests if that is consistent with the final test organization.

Do not open the PR until the Jira issue exists and the prototype has passed the validation checklist below.

## 3. Final PR description

Keep the description small. The repository's PR template already includes the required dual-license statement and it must not be removed.

Suggested body before the license block:

Adds built-in current-auditor value generation for ordinary entity attributes.

- `@CreatedBy` generates on insert.
- `@LastModifiedBy` generates on insert and update.
- `CurrentAuditorResolver<T>` keeps identity resolution outside Hibernate ORM.
- Supports application-supplied resolver instances/classes and managed bean lookup.
- Includes configuration, lifecycle, Session/StatelessSession, merge, inheritance, access strategy and type coverage.
- Documents the feature in the User Guide and What's New.

Jira: HHH-XXXXX
Related Quarkus discussion: https://github.com/quarkusio/quarkus/issues/54724

Do not add long architecture essays to the PR body. Put rationale in the Jira issue and let the code/tests show behavior.

## 4. Documentation expected for this feature

Hibernate's CONTRIBUTING.md says applicable documentation must be updated. Since this adds public annotations and a public SPI, documentation is applicable.

The prototype should contain:

- Javadocs on `@CreatedBy`, `@LastModifiedBy`, `CurrentAuditorResolver`, and the setting.
- User Guide coverage beside the other generated-property annotations.
- A short entry in `whats-new.adoc` for Hibernate ORM 8.1.
- No migration-guide entry is required for a purely additive feature unless the maintainers ask for one or the implementation changes existing behavior.
- SPI classification must be valid because the resolver is an integration contract.
- The SPI docs/lifecycle contract should state thread-safety and ownership expectations where relevant.

## 5. Test matrix before PR

Required behavior:

- CreatedBy is assigned on INSERT.
- CreatedBy is not regenerated on UPDATE.
- LastModifiedBy is assigned on INSERT.
- LastModifiedBy is regenerated on UPDATE.
- Resolver value overrides a manually assigned audit value when generation occurs.
- A configured resolver returning null leaves the current value unchanged.
- No dirty change does not regenerate LastModifiedBy.
- Merge uses the current auditor for the generated update.
- StatelessSession insert/update uses the generator.
- Field access works.
- Property/getter access works.
- Mapped-superclass audit attributes work.
- Non-String auditor types work.
- Wrong runtime auditor type fails with a useful property error.
- Resolver may be supplied as an instance.
- Resolver may be supplied as a Class.
- Resolver may be supplied as a class name.
- Class/class-name resolution goes through Hibernate's managed bean registry instead of unmanaged reflective instances.
- Resolver class is not repeatedly instantiated for every audited attribute.
- Using audit annotations without a configured resolver fails clearly.
- No resolver is required when there are no audit annotations.
- Resolver state is cleaned between tests.
- Embeddable audit attributes should be covered before final PR if supported by the existing value-generation infrastructure.
- An exception thrown by the resolver should propagate without being silently swallowed.

Potential follow-up only if maintainers request it:

- association-valued auditors such as `@ManyToOne User createdBy`;
- a dedicated SessionFactoryBuilder supply method;
- framework-specific adapters such as Quarkus SecurityIdentity;
- reactive-specific integration.

Do not expand into those areas in the first PR without a concrete requirement.

## 6. Reviewer-risk checklist based on recent Hibernate ORM reviews

### Steve Ebersole

Likely focus:
- lifecycle and ownership;
- whether configuration is resolved at the correct layer;
- whether tests construct and exercise the real SessionFactory/runtime path;
- missing bootstrap override/configuration paths;
- blurred SessionFactoryOptions responsibilities.

Current mitigation:
- class/class-name resolver acquisition uses ManagedBeanRegistry bootstrap-safe managed beans;
- tests exercise actual SessionFactory persistence, not only metadata construction;
- lifecycle expectations are documented.

Still verify before PR:
- managed bean acquisition really gives one reusable lifecycle-managed bean under fallback and CDI containers;
- no new SessionFactoryOptions API is needed for a supported supply path.

### Christian Beikov

Likely focus:
- test completeness;
- Jira linkage;
- commit key;
- `spotless` / formatting;
- concrete type behavior and edge cases.

Current mitigation:
- focused configuration/type tests;
- wrong-type diagnostics;
- generic Long auditor test.

Before PR:
- Jira exists;
- tests use `@JiraKey`;
- run formatting and focused tests.

### Gavin King

Likely focus:
- semantic responsibility at the right abstraction layer;
- avoid high-level annotations changing behavior through unrelated low-level mapping flags;
- avoid application policy in Hibernate;
- naming and API clarity;
- remove unnecessary convenience logic.

Current mitigation:
- Hibernate does not know about "user" or authentication;
- no `system`, `anonymous`, or framework-specific fallback;
- resolver policy stays with application;
- annotations use existing value-generation infrastructure.

Do not add behavior based on `@Column(insertable/updatable)` or security concepts unless maintainers request it.

### Yoann Rodiere

Likely focus:
- smallest possible public/SPI surface;
- whether this belongs upstream at all;
- API/SPI classification;
- Quarkus/Jandex/build-time constraints;
- realistic tests rather than synthetic coverage.

Current mitigation:
- small resolver contract;
- generator implementation internal;
- SPI classified as implementable/suppliable;
- no Quarkus dependency;
- no session argument on the resolver because there is no demonstrated need for one.

Before PR:
- run classification/SPI validation;
- avoid adding builder APIs, adapters, helper layers or extra abstractions without maintainer direction.

## 7. Commands to run before claiming the branch is ready

Formatting:

`./gradlew spotlessApply`
`./gradlew formatChecks`

Focused tests:

`./gradlew :hibernate-core:test --tests org.hibernate.orm.test.mapping.generated.CurrentAuditorGenerationTest`
`./gradlew :hibernate-core:test --tests org.hibernate.orm.test.mapping.generated.CurrentAuditorConfigurationTest`
`./gradlew :hibernate-core:test --tests org.hibernate.orm.test.mapping.generated.CurrentAuditorTypeTest`

Documentation build/check as appropriate:

`./gradlew :documentation:build`

Classification/SPI validation because a new SPI is introduced:

`./gradlew :documentation:generateClassificationMetadata`
`./gradlew :documentation:validateClassifications`
`./gradlew :documentation:validateSpi`

Full requirement from CONTRIBUTING.md before submission:

`./gradlew clean build`

Do not state that any of these passed until they have actually been executed successfully.

## 8. Final cleanup before PR

- Rebase on current upstream `main`.
- Replace all HHH-XXXXX placeholders with the real Jira key.
- Add `@JiraKey` where appropriate.
- Squash the exploratory history.
- Remove this file from the final PR diff.
- Verify User Guide and What's New render.
- Verify no Quarkus/CDI/SecurityIdentity dependency leaked into Hibernate Core.
- Verify no unrelated files are changed.
- Verify branch has only this Jira's work.
- Do not open the PR until the final diff is reviewed once more.
