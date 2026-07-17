/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.utilities.lifecycle.Loadable
import dev.gradienttim.gradeway.utilities.lifecycle.Reloadable
import dev.gradienttim.gradeway.utilities.lifecycle.Unloadable

/**
 * Represents a manager responsible for handling language-related operations in the system.
 *
 * The `LanguageManager` interface extends `Loadable`, `Unloadable`, and `Reloadable`,
 * indicating that it supports lifecycle operations for loading, unloading, and reloading
 * language data or resources.
 *
 * Implementations of this interface are expected to manage functionalities such as:
 * - Loading language-specific configurations or resources.
 * - Unloading resources or performing cleanup when language data is no longer needed.
 * - Reloading resources or updating language-related data dynamically when changes occur.
 *
 * This interface is typically used in applications that support internationalization
 * (i18n) or localization (l10n) and need to ensure that language-related changes are
 * handled efficiently and without disrupting the application's workflow.
 */
interface LanguageManager : Loadable, Unloadable, Reloadable
