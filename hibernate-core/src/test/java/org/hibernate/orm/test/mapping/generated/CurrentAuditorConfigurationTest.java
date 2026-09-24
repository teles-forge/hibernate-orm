/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import org.hibernate.MappingException;
import org.hibernate.annotations.CreatedBy;
import org.hibernate.boot.MetadataSources;
import org.hibernate.context.spi.CurrentAuditorResolver;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.StateManagementSettings.CURRENT_AUDITOR_RESOLVER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@BaseUnitTest
public class CurrentAuditorConfigurationTest {

	@Test
	void rejectsAuditorAnnotationWithoutResolver() {
		final var exception = assertThrows(
				MappingException.class,
				() -> buildSessionFactory( AuditedEntity.class, null )
		);

		assertThat( exception.getMessage() )
				.contains( CurrentAuditorResolver.class.getSimpleName() )
				.contains( CURRENT_AUDITOR_RESOLVER );
	}

	@Test
	void resolverMayBeConfiguredAsInstance() {
		assertDoesNotThrow( () -> buildSessionFactory( AuditedEntity.class, new TestCurrentAuditorResolver() ) );
	}

	@Test
	void resolverMayBeConfiguredAsClass() {
		assertDoesNotThrow( () -> buildSessionFactory( AuditedEntity.class, TestCurrentAuditorResolver.class ) );
	}

	@Test
	void resolverMayBeConfiguredAsClassName() {
		assertDoesNotThrow( () -> buildSessionFactory( AuditedEntity.class, TestCurrentAuditorResolver.class.getName() ) );
	}

	@Test
	void resolverIsNotRequiredWithoutAuditorAnnotations() {
		assertDoesNotThrow( () -> buildSessionFactory( PlainEntity.class, null ) );
	}

	private static void buildSessionFactory(Class<?> annotatedClass, Object resolver) {
		final var builder = ServiceRegistryUtil.serviceRegistryBuilder();
		if ( resolver != null ) {
			builder.applySetting( CURRENT_AUDITOR_RESOLVER, resolver );
		}
		try ( var serviceRegistry = builder.build() ) {
			try ( var sessionFactory = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( annotatedClass )
					.buildMetadata()
					.buildSessionFactory() ) {
				assertThat( sessionFactory ).isOpen();
			}
		}
	}

	public static class TestCurrentAuditorResolver implements CurrentAuditorResolver<String> {
		@Override
		public String resolveCurrentAuditor() {
			return "auditor";
		}
	}

	@Entity(name = "AuditedEntityWithoutConfiguredResolver")
	public static class AuditedEntity {
		@Id
		private Long id;

		@CreatedBy
		private String createdBy;
	}

	@Entity(name = "EntityWithoutAuditorAnnotations")
	public static class PlainEntity {
		@Id
		private Long id;
	}
}
