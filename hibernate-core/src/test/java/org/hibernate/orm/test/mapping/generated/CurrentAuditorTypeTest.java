/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.PropertyValueException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = {
		CurrentAuditorTypeTest.AuditedEntity.class,
		CurrentAuditorTypeTest.WrongTypeAuditedEntity.class
})
@ServiceRegistry(settings = @Setting(
		name = StateManagementSettings.CURRENT_AUDITOR_RESOLVER,
		value = "org.hibernate.orm.test.mapping.generated.CurrentAuditorTypeTest$LongCurrentAuditorResolver"
))
@SessionFactory
public class CurrentAuditorTypeTest {
	private static final ThreadLocal<Long> CURRENT_AUDITOR = new ThreadLocal<>();

	@AfterEach
	void clearCurrentAuditor() {
		CURRENT_AUDITOR.remove();
	}

	@Test
	void supportsNonStringAuditorType(SessionFactoryScope scope) {
		CURRENT_AUDITOR.set( 101L );
		scope.inTransaction( session -> session.persist( new AuditedEntity( 1L, "initial" ) ) );

		CURRENT_AUDITOR.set( 202L );
		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 1L );
			entity.name = "updated";
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( AuditedEntity.class, 1L );
			assertThat( entity.createdBy ).isEqualTo( 101L );
			assertThat( entity.lastModifiedBy ).isEqualTo( 202L );
		} );
	}

	@Test
	void rejectsAuditorValueWithWrongPropertyType(SessionFactoryScope scope) {
		CURRENT_AUDITOR.set( 101L );

		assertThatThrownBy(
				() -> scope.inTransaction( session -> session.persist( new WrongTypeAuditedEntity( 2L ) ) )
		)
				.isInstanceOf( PropertyValueException.class )
				.hasMessageContaining( CurrentAuditorResolver.class.getSimpleName() )
				.hasMessageContaining( "java.lang.Long" )
				.hasMessageContaining( "java.lang.String" );
	}

	public static class LongCurrentAuditorResolver implements CurrentAuditorResolver<Long> {
		@Override
		public Long resolveCurrentAuditor() {
			return CURRENT_AUDITOR.get();
		}
	}

	@Entity(name = "LongAuditedEntity")
	public static class AuditedEntity {
		@Id
		private Long id;

		private String name;

		@CreatedBy
		private Long createdBy;

		@LastModifiedBy
		private Long lastModifiedBy;

		public AuditedEntity() {
		}

		public AuditedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "WrongTypeAuditedEntity")
	public static class WrongTypeAuditedEntity {
		@Id
		private Long id;

		@CreatedBy
		private String createdBy;

		public WrongTypeAuditedEntity() {
		}

		public WrongTypeAuditedEntity(Long id) {
			this.id = id;
		}
	}
}
