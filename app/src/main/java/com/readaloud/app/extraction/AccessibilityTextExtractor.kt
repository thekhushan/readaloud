package com.readaloud.app.extraction

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityTextExtractor(
    private val grouper: MessageGrouper = MessageGrouper()
) {
    fun extract(root: AccessibilityNodeInfo?): List<String> {
        if (root == null) return emptyList()
        val items = mutableListOf<ExtractedText>()
        traverse(root, items, Counter())
        return grouper.fromAccessibilityItems(items)
    }

    private fun traverse(node: AccessibilityNodeInfo, out: MutableList<ExtractedText>, counter: Counter) {
        val bounds = Rect()
        runCatching { node.getBoundsInScreen(bounds) }

        if (node.isVisibleToUser) {
            val text = node.text?.toString()?.trim().orEmpty()
            val description = node.contentDescription?.toString()?.trim().orEmpty()
            val className = node.className?.toString()
            val viewId = runCatching { node.viewIdResourceName }.getOrNull()

            listOf(text, description)
                .filter { it.isNotBlank() }
                .distinct()
                .forEach {
                    out += ExtractedText(
                        text = it,
                        top = bounds.top,
                        left = bounds.left,
                        order = counter.next(),
                        className = className,
                        viewId = viewId
                    )
                }
        }

        for (index in 0 until node.childCount) {
            val child = runCatching { node.getChild(index) }.getOrNull() ?: continue
            traverse(child, out, counter)
        }
    }

    private class Counter {
        private var value = 0
        fun next(): Int = value++
    }
}
