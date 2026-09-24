/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.CreatedBy;
import org.hibernate.annotations.LastModifiedBy;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.context.spi.CurrentAuditorResolver;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = CurrentAuditorGenerationTest.AuditedEntity.class)
@ServiceRegistry(settings = @Setting(
		name = StateManagementSettings.CURRENT_AUDITOR_RESOLVER,
		value = "org.hibernate.orm.test.mapping.generated.CurrentAuditorGenerationTest$TestCurrentAuditorResolver"
))
@SessionFactory
public class CurrentAuditorGenerationTest {
	private static final ThreadLocal<String> CURRENT_AUDITOR = new ThreadLocal<>();

	@Test
	void createdByAndLastModifiedBy(SessionFactoryScope scope) {
		try {
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
		finally {
			CURRENT_AUDITOR.remove();
		}
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

	public static class TestCurrentAuditorResolver implements CurrentAuditorResolver<String> {
		@Override
		public String resolveCurrentAuditor(SharedSessionContract session) {
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
}
