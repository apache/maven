/**
 * A dependency injection framework for Maven that provides JSR-330 style annotations
 * for managing object lifecycle and dependencies within Maven's build process.
 * <p>
 * This package provides a set of annotations that control how objects are created,
 * managed and injected throughout Maven's execution lifecycle. The framework is designed
 * to be lightweight yet powerful, supporting various scopes of object lifecycle from
 * singleton instances to mojo-execution-scoped beans.
 * <p>
 * Key features include:
 * <ul>
 *   <li>Constructor, method, and field injection</li>
 *   <li>Qualifiers for distinguishing between beans of the same type</li>
 *   <li>Multiple scopes (Singleton, Session, and MojoExecution)</li>
 *   <li>Priority-based implementation selection</li>
 *   <li>Type-safe dependency injection</li>
 * </ul>
 *
 * @since 4.0.0
 */
package org.apache.maven.api.di;
