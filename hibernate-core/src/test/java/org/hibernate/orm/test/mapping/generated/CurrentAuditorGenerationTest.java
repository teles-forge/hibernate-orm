/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.annotations.CreatedBy;
import org.hibernate.annotations.LastModifiedBy;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.context.spi.CurrentAuditorResolver;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		CurrentAuditorGenerationTest.AuditedEntity.class,
		CurrentAuditorGenerationTest.InheritedAuditedEntity.class
})
@ServiceRegistry(settings = @Setting(
		name = StateManagementSettings.CURRENT_AUDITOR_RESOLVER,
		value = "org.hibernate.orm.test.mapping.generated.CurrentAuditorGenerationTest$TestCurrentAuditorResolver"
))
@SessionFactory
public class CurrentAuditorGenerationTest {
	private static final ThreadLocal<String> CURRENT_AUDITOR = new ThreadLocal<>();

	@AfterEach
	void clearCurrentAuditor() {
		CURRENT_AUDITOR.remove();
	}

	@Test
	void createdByAndLastModifiedBy(SessionFactoryScope scope) {
		CURRENT_AUDITOR.set( "alice" );
		scope.inTransaction( session -> {
			final var entity = new AuditedEntity( 1L, "initial" );
			session.persist( entity );
		} );

		CURRENT_AUDITOR.set( "bob" );
		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 1L );
			assertThat( entity.createdBy ).isEqualTo( "alice" );
			assertThat( entity.lastModifiedBy ).isEqualTo( "alice" );
			entity.name = "updated";
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 1L );
			assertThat( entity.createdBy ).isEqualTo( "alice" );
			assertThat( entity.lastModifiedBy ).isEqualTo( "bob" );
		} );
	}

	@Test
	void nullAuditorLeavesCurrentValueUnchanged(SessionFactoryScope scope) {
		CURRENT_AUDITOR.remove();

		scope.inTransaction( session -> {
			final var entity = new AuditedEntity( 2L, "initial" );
			entity.createdBy = "manual-created";
			entity.lastModifiedBy = "manual-modified";
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 2L );
			entity.name = "updated";
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 2L );
			assertThat( entity.createdBy ).isEqualTo( "manual-created" );
			assertThat( entity.lastModifiedBy ).isEqualTo( "manual-modified" );
		} );
	}

	@Test
	void auditorOverridesAssignedValues(SessionFactoryScope scope) {
		CURRENT_AUDITOR.set( "alice" );

		scope.inTransaction( session -> {
			final var entity = new AuditedEntity( 3L, "initial" );
			entity.createdBy = "manual-created";
			entity.lastModifiedBy = "manual-modified";
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 3L );
			assertThat( entity.createdBy ).isEqualTo( "alice" );
			assertThat( entity.lastModifiedBy ).isEqualTo( "alice" );
		} );
	}

	@Test
	void noOpFlushDoesNotModifyAuditor(SessionFactoryScope scope) {
		CURRENT_AUDITOR.set( "alice" );
		scope.inTransaction( session -> session.persist( new AuditedEntity( 4L, "initial" ) ) );

		CURRENT_AUDITOR.set( "bob" );
		scope.inTransaction( session -> session.find( AuditedEntity.class, 4L ) );

		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 4L );
			assertThat( entity.createdBy ).isEqualTo( "alice" );
			assertThat( entity.lastModifiedBy ).isEqualTo( "alice" );
		} );
	}

	@Test
	void statelessSessionUsesCurrentAuditor(SessionFactoryScope scope) {
		CURRENT_AUDITOR.set( "alice" );
		scope.inStatelessTransaction( session -> session.insert( new AuditedEntity( 5L, "initial" ) ) );

		CURRENT_AUDITOR.set( "bob" );
		scope.inStatelessTransaction( session -> {
			final var entity = session.get( AuditedEntity.class, 5L );
			entity.name = "updated";
			session.update( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 5L );
			assertThat( entity.createdBy ).isEqualTo( "alice" );
			assertThat( entity.lastModifiedBy ).isEqualTo( "bob" );
		} );
	}

	@Test
	void mappedSuperclassFieldsAreAudited(SessionFactoryScope scope) {
		CURRENT_AUDITOR.set( "alice" );
		scope.inTransaction( session -> session.persist( new InheritedAuditedEntity( 6L, "initial" ) ) );

		CURRENT_AUDITOR.set( "bob" );
		scope.inTransaction( session -> {
			final var entity = session.find( InheritedAuditedEntity.class, 6L );
			entity.name = "updated";
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( InheritedAuditedEntity.class, 6L );
			assertThat( entity.createdBy ).isEqualTo( "alice" );
			assertThat( entity.lastModifiedBy ).isEqualTo( "bob" );
		} );
	}

	public static class TestCurrentAuditorResolver implements CurrentAuditorResolver<String> {
		@Override
		public String resolveCurrentAuditor() {
			return CURRENT_AUDITOR.get();
		}
	}

	@Entity(name = "AuditedEntity")
	public static class AuditedEntity {
		@Id
		private Long id;

		private String name;

		@CreatedBy
		private String createdBy;

		@LastModifiedBy
		private String lastModifiedBy;

		public AuditedEntity() {
		}

		public AuditedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@MappedSuperclass
	public abstract static class AuditedBase {
		@CreatedBy
		String createdBy;

		@LastModifiedBy
		String lastModifiedBy;
	}

	@Entity(name = "InheritedAuditedEntity")
	public static class InheritedAuditedEntity extends AuditedBase {
		@Id
		private Long id;

		private String name;

		public InheritedAuditedEntity() {
		}

		public InheritedAuditedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
