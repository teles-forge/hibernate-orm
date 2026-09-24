/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.context.spi.CurrentAuditorResolver;
import org.hibernate.generator.internal.CurrentAuditorGeneration;

/**
 * Specifies that the annotated field or property is populated with the
 * {@linkplain CurrentAuditorResolver current auditor} when the entity is inserted
 * or updated.
 * <p>
 * A {@link CurrentAuditorResolver} must be configured using
 * {@link StateManagementSettings#CURRENT_AUDITOR_RESOLVER}. If the resolver returns
 * {@code null}, the current value of the property is left unchanged.
 *
 * @see CreatedBy
 * @see CurrentAuditorResolver
 * @see StateManagementSettings#CURRENT_AUDITOR_RESOLVER
 *
 * @since 8.1
 */
@Incubating(since = "8.1")
@ValueGenerationType(generatedBy = CurrentAuditorGeneration.class)
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface LastModifiedBy {
}
