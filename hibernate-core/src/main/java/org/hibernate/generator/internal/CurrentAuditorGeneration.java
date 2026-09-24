/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import java.util.EnumSet;

import org.hibernate.MappingException;
import org.hibernate.annotations.CreatedBy;
import org.hibernate.annotations.LastModifiedBy;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.context.spi.CurrentAuditorResolver;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;

import static org.hibernate.cfg.StateManagementSettings.CURRENT_AUDITOR_RESOLVER;
import static org.hibernate.generator.EventTypeSets.INSERT_AND_UPDATE;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;

/**
 * Value generation strategy underlying {@link CreatedBy} and
 * {@link LastModifiedBy}.
 *
 * @since 8.1
 */
public class CurrentAuditorGeneration implements BeforeExecutionGenerator {
	private final CurrentAuditorResolver<?> resolver;
	private final EnumSet<EventType> eventTypes;
	private final Class<?> propertyType;

	public CurrentAuditorGeneration(CreatedBy annotation, GeneratorCreationContext context) {
		this( context, INSERT_ONLY );
	}

	public CurrentAuditorGeneration(LastModifiedBy annotation, GeneratorCreationContext context) {
		this( context, INSERT_AND_UPDATE );
	}

	private CurrentAuditorGeneration(
			GeneratorCreationContext context,
			EnumSet<EventType> eventTypes) {
		this.resolver = resolveCurrentAuditorResolver( context );
		this.eventTypes = eventTypes;
		this.propertyType = context.getType().getReturnedClass();
	}

	@Override
	public Object generate(
			SharedSessionContractImplementor session,
			Object owner,
			Object currentValue,
			EventType eventType) {
		final Object auditor = resolver.resolveCurrentAuditor( session );
		return auditor == null ? currentValue : auditor;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return eventTypes;
	}

	@Override
	public Class<?> getGeneratedType() {
		return propertyType;
	}

	private static CurrentAuditorResolver<?> resolveCurrentAuditorResolver(
			GeneratorCreationContext context) {
		final var serviceRegistry = context.getServiceRegistry();
		final var setting = serviceRegistry.requireService( ConfigurationService.class )
				.getSettings()
				.get( CURRENT_AUDITOR_RESOLVER );
		final var resolver = serviceRegistry.requireService( StrategySelector.class )
				.resolveStrategy( CurrentAuditorResolver.class, setting );
		if ( resolver == null ) {
			throw new MappingException(
					"A CurrentAuditorResolver must be configured using '"
							+ CURRENT_AUDITOR_RESOLVER
							+ "' when using @CreatedBy or @LastModifiedBy"
			);
		}
		return resolver;
	}
}
