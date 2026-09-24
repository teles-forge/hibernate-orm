/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.context.spi;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.SharedSessionContract;
import org.hibernate.cfg.StateManagementSettings;

/**
 * A callback responsible for resolving the auditor associated with the current
 * persistence operation.
 * <p>
 * The resolver is configured using
 * {@link StateManagementSettings#CURRENT_AUDITOR_RESOLVER}. Hibernate does not
 * make any assumption about where the auditor comes from. An implementation may
 * use a security context, a service account, a scheduled-job identity, or any
 * other application-specific source.
 * <p>
 * A resolver must be configured when using
 * {@link org.hibernate.annotations.CreatedBy @CreatedBy} or
 * {@link org.hibernate.annotations.LastModifiedBy @LastModifiedBy}. Returning
 * {@code null} indicates that no auditor is available for the current operation,
 * in which case the current property value is left unchanged.
 *
 * @param <T> the auditor type
 *
 * @see StateManagementSettings#CURRENT_AUDITOR_RESOLVER
 * @see org.hibernate.annotations.CreatedBy
 * @see org.hibernate.annotations.LastModifiedBy
 *
 * @since 8.1
 */
@Incubating(since = "8.1")
public interface CurrentAuditorResolver<T> {

	/**
	 * Resolve the auditor associated with the current persistence operation.
	 *
	 * @param session the current session
	 * @return the current auditor, or {@code null} if no auditor is available
	 */
	@Nullable
	T resolveCurrentAuditor(SharedSessionContract session);
}
