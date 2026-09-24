/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.LastModifiedBy;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class LastModifiedByAnnotation implements LastModifiedBy {

	public LastModifiedByAnnotation(ModelsContext modelContext) {
	}

	public LastModifiedByAnnotation(LastModifiedBy annotation, ModelsContext modelContext) {
	}

	public LastModifiedByAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return LastModifiedBy.class;
	}
}
