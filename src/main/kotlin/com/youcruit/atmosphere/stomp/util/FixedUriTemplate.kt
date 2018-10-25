package com.youcruit.atmosphere.stomp.util

import org.atmosphere.util.uri.UriPattern
import org.atmosphere.util.uri.UriTemplateParser

class FixedUriTemplate(
    templateParser: UriTemplateParser
) {
    @Suppress("unused") // for debugging
    val template: String = templateParser.template
    val pattern: UriPattern = UriPattern(templateParser.pattern, templateParser.groupIndexes)
    private val templateVariables: List<String> = templateParser.names

    constructor(template: String) : this(UriTemplateParser(template))

    fun match(uri: CharSequence, templateVariableToValue: Map<String, String>): Boolean =
        pattern.match(uri, templateVariables, templateVariableToValue)

    override fun toString(): String = "Template $template pattern $pattern"

    override fun hashCode(): Int = pattern.hashCode()

    override fun equals(other: Any?): Boolean = other is FixedUriTemplate && this.pattern == other.pattern
}