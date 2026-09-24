/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import java.util.EnumSet;

import org.hibernate.MappingException;
import org.hibernate.PropertyValueException;
import org.hibernate.annotations.CreatedBy;
import org.hibernate.annotations.LastModifiedBy;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.context.spi.CurrentAuditorResolver;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.internal.util.type.PrimitiveWrappers;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

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
	private final ResolverAccess resolverAccess;
	private final EnumSet<EventType> eventTypes;
	private final Class<?> propertyType;
	private final String entityName;
	private final String propertyName;

	public CurrentAuditorGeneration(CreatedBy annotation, GeneratorCreationContext context) {
		this( context, INSERT_ONLY );
	}

	public CurrentAuditorGeneration(LastModifiedBy annotation, GeneratorCreationContext context) {
		this( context, INSERT_AND_UPDATE );
	}

	private CurrentAuditorGeneration(
			GeneratorCreationContext context,
			EnumSet<EventType> eventTypes) {
		resolverAccess = resolveCurrentAuditorResolver( context );
		this.eventTypes = eventTypes;
		propertyType = context.getType().getReturnedClass();

		final var persistentClass = context.getPersistentClass();
		entityName = persistentClass != null
				? persistentClass.getEntityName()
				: context.getMemberDetails().toJavaMember().getDeclaringClass().getName();
		propertyName = context.getProperty().getName();
	}

	@Override
	public Object generate(
			SharedSessionContractImplementor session,
			Object owner,
			Object currentValue,
			EventType eventType) {
		final Object auditor = resolverAccess.get().resolveCurrentAuditor();
		if ( auditor == null ) {
			return currentValue;
		}
		if ( !PrimitiveWrappers.isInstance( propertyType, auditor ) ) {
			throw new PropertyValueException(
					"CurrentAuditorResolver returned value of type '" + auditor.getClass().getTypeName()
							+ "' which is not assignable to auditor property type '" + propertyType.getTypeName() + "'",
					entityName,
					propertyName
			);
		}
		return auditor;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return eventTypes;
	}

	@Override
	public Class<?> getGeneratedType() {
		return propertyType;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ResolverAccess resolveCurrentAuditorResolver(GeneratorCreationContext context) {
		final var serviceRegistry = context.getServiceRegistry();
		final Object setting = serviceRegistry.requireService( ConfigurationService.class )
				.getSettings()
				.get( CURRENT_AUDITOR_RESOLVER );

		if ( setting == null ) {
			throw new MappingException(
					"A CurrentAuditorResolver must be configured using '"
							+ CURRENT_AUDITOR_RESOLVER
							+ "' when using @CreatedBy or @LastModifiedBy"
			);
		}

		if ( setting instanceof CurrentAuditorResolver<?> resolver ) {
			return () -> resolver;
		}

		final Class<? extends CurrentAuditorResolver> resolverClass;
		if ( setting instanceof Class<?> implementationClass ) {
			try {
				resolverClass = implementationClass.asSubclass( CurrentAuditorResolver.class );
			}
			catch (ClassCastException e) {
				throw new MappingException(
						"Configured current auditor resolver class '" + implementationClass.getName()
								+ "' does not implement " + CurrentAuditorResolver.class.getName(),
						e
				);
			}
		}
		else {
			resolverClass = serviceRegistry.requireService( StrategySelector.class )
					.selectStrategyImplementor( CurrentAuditorResolver.class, setting.toString() );
		}

		final var bean = serviceRegistry.requireService( ManagedBeanRegistry.class )
				.getBootstrapSafeBean( resolverClass );
		return () -> bean.getBeanInstance();
	}

	@FunctionalInterface
	private interface ResolverAccess {
		CurrentAuditorResolver<?> get();
	}
}
