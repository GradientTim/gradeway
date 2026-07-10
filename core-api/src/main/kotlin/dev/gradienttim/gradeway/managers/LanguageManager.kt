/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.managers

import dev.gradienttim.gradeway.utilities.lifecycle.Loadable
import dev.gradienttim.gradeway.utilities.lifecycle.Reloadable
import dev.gradienttim.gradeway.utilities.lifecycle.Unloadable

interface LanguageManager : Loadable, Unloadable, Reloadable
