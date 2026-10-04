/*
MIT License
Copyright (c) 2026 GradientTim
*/
package dev.gradienttim.gradeway.extensions

import dev.gradienttim.gradeway.commands.extensions.infoList
import dev.gradienttim.gradeway.commands.extensions.infoListArguments
import dev.gradienttim.gradeway.commands.gradeway.trackStagePreview
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.translation.Argument
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslationStore
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer
import java.util.Locale
import java.util.Properties
import java.util.UUID
import kotlin.test.*

class AdventureExtensionsTest {
    private val miniMessage = MiniMessage.builder()
        .editTags { builder ->
            builder.tag("prefix", Tag.inserting(Component.text("Gradeway: ")))
            builder.tag("primary", Tag.styling { })
            builder.tag("secondary", Tag.styling { })
        }
        .build()

    private val languages: Map<String, String> = Properties()
        .apply {
            AdventureExtensionsTest::class.java.classLoader
                .getResourceAsStream("languages/en.properties")!!
                .use { load(it) }
        }
        .entries
        .associate { (key, value) -> key.toString() to value.toString() }

    private fun Component.plainText(): String {
        val text = StringBuilder()
        ComponentFlattener.basic().flatten(this) { text.append(it) }
        return text.toString()
    }

    private fun Component.descendants(): Sequence<Component> =
        sequenceOf(this) + children().asSequence().flatMap { it.descendants() }

    @Test
    fun `toIdArgument resolves the id placeholder and the copy_id click tag`() {
        val id = UUID.fromString("3f2a9c1e-4b7d-4e21-9a0c-5d8e1f2b7b10")
        val store = MiniMessageTranslationStore.create(Key.key("gradeway", "test"), miniMessage)
        store.registerAll(Locale.US, languages)

        val translated = store.translate(
            Component.translatable(
                "gradeway.command.role.list.entry",
                id.toIdArgument(),
                Argument.string("name", "admin")
            ),
            Locale.US
        )

        assertNotNull(translated)
        assertEquals("Gradeway: • admin", translated.plainText())

        val clickable = translated.descendants().first { it.clickEvent() != null }
        assertEquals(ClickEvent.copyToClipboard(id.toString()), clickable.clickEvent())

        val hover = translated.descendants().mapNotNull { it.hoverEvent() }.first()
        assertEquals(HoverEvent.Action.SHOW_TEXT, hover.action())
        assertContains((hover.value() as Component).plainText(), id.toString())
    }

    @Test
    fun `info list arguments render the count inline and the translated entries in the hover`() {
        val store = MiniMessageTranslationStore.create(Key.key("gradeway", "test"), miniMessage)
        store.registerAll(Locale.US, languages)
        val renderer = TranslatableComponentRenderer.usingTranslationSource(store)

        val roles = listOf(
            Component.translatable("gradeway.command.player.info.role", Argument.string("role", "admin")),
            Component.translatable(
                "gradeway.command.player.info.roleTemporary",
                Argument.string("role", "vip"),
                Argument.string("until", "2026-12-31 00:00:00 UTC")
            )
        )
        val rendered = renderer.render(
            Component.translatable(
                "gradeway.command.player.info",
                UUID.randomUUID().toIdArgument(),
                Argument.string("name", "GradientTim"),
                Argument.component("primary_role", Component.text("admin")),
                Argument.numeric("weight", 5),
                *infoListArguments("roles", roles),
                Argument.numeric("permissions", 3),
                Argument.numeric("templates", 1),
                Argument.numeric("attributes", 0),
                Argument.string("created", "2026-07-24 16:28:49 UTC"),
                Argument.string("updated", "2026-10-03 11:30:48 UTC")
            ),
            Locale.US
        )

        assertContains(rendered.plainText(), "Roles: 2")
        val rolesHover = rendered.descendants()
            .mapNotNull { it.hoverEvent()?.value() as? Component }
            .map { it.plainText() }
            .first { it.startsWith("Roles") }
        assertContains(rolesHover, "admin")
        assertContains(rolesHover, "vip (until 2026-12-31 00:00:00 UTC)")
    }

    @Test
    fun `info list arguments fall back to a none entry when empty`() {
        val store = MiniMessageTranslationStore.create(Key.key("gradeway", "test"), miniMessage)
        store.registerAll(Locale.US, languages)
        val renderer = TranslatableComponentRenderer.usingTranslationSource(store)

        val rendered = renderer.render(infoList(emptyList()), Locale.US)

        assertEquals("None", rendered.plainText())
    }

    @Test
    fun `track stage preview lists up to ten stages and summarizes the rest`() {
        val store = MiniMessageTranslationStore.create(Key.key("gradeway", "test"), miniMessage)
        store.registerAll(Locale.US, languages)
        val renderer = TranslatableComponentRenderer.usingTranslationSource(store)

        val preview = renderer.render(trackStagePreview((1..12).map { "role$it" }), Locale.US).plainText()

        val lines = preview.lines()
        assertEquals(11, lines.size)
        assertEquals("#1 role1", lines.first())
        assertEquals("#10 role10", lines[9])
        assertEquals("... and 2 more", lines.last())
        assertEquals("None", renderer.render(trackStagePreview(emptyList()), Locale.US).plainText())
    }

    @Test
    fun `every english translation value parses without leftover tags`() {
        val copyId = TagResolver.resolver("copy_id", Tag.styling { })
        val anyTag = object : TagResolver {
            override fun resolve(name: String, arguments: ArgumentQueue, ctx: Context): Tag? =
                if (has(name)) Tag.selfClosingInserting(Component.text("x")) else null

            override fun has(name: String): Boolean = !miniMessage.tags().has(name) && !copyId.has(name)
        }

        languages.forEach { (key, value) ->
            val plain = miniMessage.deserialize(value, copyId, anyTag).plainText()
            assertFalse(plain.contains('<') || plain.contains('>'), "Unparsed tag in $key: $plain")
        }
    }
}
