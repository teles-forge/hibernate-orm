/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.CreatedBy;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CreatedByAnnotation implements CreatedBy {

	public CreatedByAnnotation(ModelsContext modelContext) {
	}

	public CreatedByAnnotation(CreatedBy annotation, ModelsContext modelContext) {
	}

	public CreatedByAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return CreatedBy.class;
	}
}
