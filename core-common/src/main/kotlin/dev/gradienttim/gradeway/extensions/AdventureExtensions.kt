/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.translation.Argument
import java.util.UUID

fun UUID.toIdArgument(): ComponentLike = Argument.tagResolver(
    Placeholder.unparsed("id", toString()),
    TagResolver.resolver("copy_id", Tag.styling(ClickEvent.copyToClipboard(toString())))
)
