/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package androidx.compose.runtime.collection

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.max

/**
 * A [MutableList]-like structure with a simplified interface that offers faster access than
 * [ArrayList].
 */
@OptIn(ExperimentalContracts::class, ExperimentalCollectionApi::class)
@ExperimentalCollectionApi
class MutableVector<T> @PublishedApi internal constructor(
    @PublishedApi internal var content: Array<T?>,
    size: Int
) {
    /**
     * Stores allocated [MutableList] representation of this vector.
     */
    private var list: MutableList<T>? = null

    /**
     * The number of elements in the [MutableVector].
     */
    var size: Int = size
        private set

    /**
     * Returns the last valid index in the [MutableVector].
     */
    inline val lastIndex: Int get() = size - 1

    /**
     * Returns an [IntRange] of the valid indices for this [MutableVector].
     */
    inline val indices: IntRange get() = 0..size - 1

    /**
     * Adds [element] to the [MutableVector] and returns `true`.
     */
    fun add(element: T): Boolean {
        ensureCapacity(size + 1)
        content[size] = element
        size++
        return true
    }

    /**
     * Adds [element] to the [MutableVector] at the given [index], shifting over any elements
     * that are in the way.
     */
    fun add(index: Int, element: T) {
        ensureCapacity(size + 1)
        val content = content
        if (index != size) {
            content.copyInto(
                destination = content,
                destinationOffset = index + 1,
                startIndex = index,
                endIndex = size
            )
        }
        content[index] = element
        size++
    }

    /**
     * Adds all [elements] to the [MutableVector] at the given [index], shifting over any
     * elements that are in the way.
     */
    fun addAll(index: Int, elements: List<T>): Boolean {
        if (elements.isEmpty()) return false
        ensureCapacity(size + elements.size)
        val content = content
        if (index != size) {
            content.copyInto(
                destination = content,
                destinationOffset = index + elements.size,
                startIndex = index,
                endIndex = size
            )
        }
        for (i in elements.indices) {
            content[index + i] = elements[i]
        }
        size += elements.size
        return true
    }

    /**
     * Adds all [elements] to the [MutableVector] at the given [index], shifting over any
     * elements that are in the way.
     */
    fun addAll(index: Int, elements: MutableVector<T>): Boolean {
        if (elements.isEmpty()) return false
        ensureCapacity(size + elements.size)
        val content = content
        if (index != size) {
            content.copyInto(
                destination = content,
                destinationOffset = index + elements.size,
                startIndex = index,
                endIndex = size
            )
        }
        elements.content.copyInto(
            destination = content,
            destinationOffset = index,
            startIndex = 0,
            endIndex = elements.size
        )
        size += elements.size
        return true
    }

    /**
     * Adds all [elements] to the end of the [MutableVector] and returns `true` if the
     * [MutableVector] was changed.
     */
    inline fun addAll(elements: List<T>): Boolean {
        return addAll(size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableVector] and returns `true` if the
     * [MutableVector] was changed.
     */
    inline fun addAll(elements: MutableVector<T>): Boolean {
        return addAll(size, elements)
    }

    /**
     * Adds all [elements] to the end of the [MutableVector] and returns `true` if the
     * [MutableVector] was changed.
     */
    fun addAll(
        @Suppress("ArrayReturn")
        elements: Array<T>
    ): Boolean {
        if (elements.isEmpty()) {
            return false
        }
        ensureCapacity(size + elements.size)
        elements.copyInto(
            destination = content,
            destinationOffset = size
        )
        return true
    }

    /**
     * Adds all [elements] to the [MutableVector] at the given [index], shifting over any
     * elements that are in the way.
     */
    fun addAll(index: Int, elements: Collection<T>): Boolean {
        if (elements.isEmpty()) return false
        ensureCapacity(size + elements.size)
        val content = content
        if (index != size) {
            content.copyInto(
                destination = content,
                destinationOffset = index + elements.size,
                startIndex = index,
                endIndex = size
            )
        }
        elements.forEachIndexed { i, item ->
            content[index + i] = item
        }
        size += elements.size
        return true
    }

    /**
     * Adds all [elements] to the end of the [MutableVector] and returns `true` if the
     * [MutableVector] was changed.
     */
    fun addAll(elements: Collection<T>): Boolean {
        return addAll(size, elements)
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate].
     */
    inline fun any(predicate: (T) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        for (i in 0..lastIndex) {
            if (predicate(get(i))) return true
        }
        return false
    }

    /**
     * Returns `true` if any of the elements give a `true` return value for [predicate] while
     * iterating in the reverse order.
     */
    inline fun reversedAny(predicate: (T) -> Boolean): Boolean {
        contract { callsInPlace(predicate) }
        for (i in lastIndex downTo 0) {
            if (predicate(get(i))) return true
        }
        return false
    }

    /**
     * Returns [MutableList] interface access to the [MutableVector].
     */
    fun asMutableList(): MutableList<T> {
        return list ?: MutableVectorList(this).also {
            list = it
        }
    }

    /**
     * Removes all elements in the [MutableVector].
     */
    fun clear() {
        val content = content
        for (i in lastIndex downTo 0) {
            content[i] = null
        }
        size = 0
    }

    /**
     * Returns `true` if the [MutableVector] contains [element] or `false` otherwise.
     */
    operator fun contains(element: T): Boolean {
        for (i in 0..lastIndex) {
            if (get(i) == element) return true
        }
        return false
    }

    /**
     * Returns `true` if the [MutableVector] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    fun containsAll(elements: List<T>): Boolean {
        for (i in elements.indices) {
            if (!contains(elements[i])) return false
        }
        return true
    }

    /**
     * Returns `true` if the [MutableVector] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    fun containsAll(elements: Collection<T>): Boolean {
        elements.forEach {
            if (!contains(it)) return false
        }
        return true
    }

    /**
     * Returns `true` if the [MutableVector] contains all elements in [elements] or `false` if
     * one or more are missing.
     */
    fun containsAll(elements: MutableVector<T>): Boolean {
        for (i in elements.indices) {
            if (!contains(elements[i])) return false
        }
        return true
    }

    /**
     * Returns `true` if the contents of the [MutableVector] are the same or `false` if there
     * is any difference. This uses equality comparisons on each element rather than reference
     * equality.
     */
    fun contentEquals(other: MutableVector<T>): Boolean {
        if (other.size != size) {
            return false
        }
        for (i in 0..lastIndex) {
            if (other[i] != this[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Ensures that there is enough space to store [capacity] elements in the [MutableVector].
     */
    fun ensureCapacity(capacity: Int) {
        val oldContent = content
        if (oldContent.size < capacity) {
            val newSize = max(capacity, oldContent.size * 2)
            content = oldContent.copyOf(newSize)
        }
    }

    /**
     * Returns the first element in the [MutableVector] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    fun first(): T {
        if (isEmpty()) {
            throw NoSuchElementException("MutableVector is empty.")
        }
        return get(0)
    }

    /**
     * Returns the first element in the [MutableVector] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     */
    inline fun first(predicate: (T) -> Boolean): T {
        contract { callsInPlace(predicate) }
        for (i in 0..lastIndex) {
            val item = get(i)
            if (predicate(item)) {
                return item
            }
        }
        throwNoSuchElementException()
    }

    /**
     * Returns the first element in the [MutableVector] or `null` if it [isEmpty].
     */
    inline fun firstOrNull() = if (isEmpty()) null else get(0)

    /**
     * Returns the first element in the [MutableVector] for which [predicate] returns `true` or
     * returns `null` if nothing matches.
     */
    inline fun firstOrNull(predicate: (T) -> Boolean): T? {
        contract { callsInPlace(predicate) }
        for (i in 0..lastIndex) {
            val item = get(i)
            if (predicate(item)) {
                return item
            }
        }
        return null
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [MutableVector] in order.
     */
    inline fun <R> fold(initial: R, operation: (acc: R, T) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        for (i in 0..lastIndex) {
            acc = operation(acc, get(i))
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [MutableVector] in order.
     */
    inline fun <R> foldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        for (i in 0..lastIndex) {
            acc = operation(i, acc, get(i))
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [MutableVector] in reverse order.
     */
    inline fun <R> foldRight(initial: R, operation: (T, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        for (i in lastIndex downTo 0) {
            acc = operation(get(i), acc)
        }
        return acc
    }

    /**
     * Accumulates values, starting with [initial], and applying [operation] to each element
     * in the [MutableVector] in reverse order.
     */
    inline fun <R> foldRightIndexed(initial: R, operation: (index: Int, T, acc: R) -> R): R {
        contract { callsInPlace(operation) }
        var acc = initial
        for (i in lastIndex downTo 0) {
            acc = operation(i, get(i), acc)
        }
        return acc
    }

    /**
     * Calls [block] for each element in the [MutableVector], in order.
     */
    inline fun forEach(block: (T) -> Unit) {
        contract { callsInPlace(block) }
        for (i in 0..lastIndex) {
            block(get(i))
        }
    }

    /**
     * Calls [block] for each element in the [MutableVector] along with its index, in order.
     */
    inline fun forEachIndexed(block: (Int, T) -> Unit) {
        contract { callsInPlace(block) }
        for (i in 0 until size) {
            block(i, get(i))
        }
    }

    /**
     * Calls [block] for each element in the [MutableVector] in reverse order.
     */
    inline fun forEachReversed(block: (T) -> Unit) {
        contract { callsInPlace(block) }
        for (i in lastIndex downTo 0) {
            block(get(i))
        }
    }

    /**
     * Calls [block] for each element in the [MutableVector] along with its index, in reverse
     * order.
     */
    inline fun forEachReversedIndexed(block: (Int, T) -> Unit) {
        contract { callsInPlace(block) }
        for (i in lastIndex downTo 0) {
            block(i, get(i))
        }
    }

    /**
     * Returns the element at the given [index].
     */
    inline operator fun get(index: Int): T = content[index] as T

    /**
     * Returns the index of [element] in the [MutableVector] or `-1` if [element] is not there.
     */
    fun indexOf(element: T): Int {
        for (i in 0..lastIndex) {
            if (element == get(i)) return i
        }
        return -1
    }

    /**
     * Returns the index if the first element in the [MutableVector] for which [predicate]
     * returns `true`.
     */
    inline fun indexOfFirst(predicate: (T) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        for (i in 0..lastIndex) {
            if (predicate(get(i))) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns the index if the last element in the [MutableVector] for which [predicate]
     * returns `true`.
     */
    inline fun indexOfLast(predicate: (T) -> Boolean): Int {
        contract { callsInPlace(predicate) }
        for (i in lastIndex downTo 0) {
            if (predicate(get(i))) {
                return i
            }
        }
        return -1
    }

    /**
     * Returns `true` if the [MutableVector] has no elements in it or `false` otherwise.
     */
    fun isEmpty(): Boolean = size == 0

    /**
     * Returns `true` if there are elements in the [MutableVector] or `false` if it is empty.
     */
    fun isNotEmpty(): Boolean = size != 0

    /**
     * Returns the last element in the [MutableVector] or throws a [NoSuchElementException] if
     * it [isEmpty].
     */
    fun last(): T {
        if (isEmpty()) {
            throw NoSuchElementException("MutableVector is empty.")
        }
        return get(lastIndex)
    }

    /**
     * Returns the last element in the [MutableVector] for which [predicate] returns `true` or
     * throws [NoSuchElementException] if nothing matches.
     */
    inline fun last(predicate: (T) -> Boolean): T {
        contract { callsInPlace(predicate) }
        for (i in lastIndex downTo 0) {
            val item = get(i)
            if (predicate(item)) {
                return item
            }
        }
        throwNoSuchElementException()
    }

    /**
     * Returns the index of the last element in the [MutableVector] that is the same as
     * [element] or `-1` if no elements match.
     */
    fun lastIndexOf(element: T): Int {
        for (i in lastIndex downTo 0) {
            if (element == get(i)) return i
        }
        return -1
    }

    /**
     * Returns the last element in the [MutableVector] or `null` if it [isEmpty].
     */
    inline fun lastOrNull() = if (isEmpty()) null else get(lastIndex)

    /**
     * Returns the last element in the [MutableVector] for which [predicate] returns `true` or
     * returns `null` if nothing matches.
     */
    inline fun lastOrNull(predicate: (T) -> Boolean): T? {
        contract { callsInPlace(predicate) }
        for (i in lastIndex downTo 0) {
            val item = get(i)
            if (predicate(item)) {
                return item
            }
        }
        return null
    }

    /**
     * Returns an [Array] of results of transforming each element in the [MutableVector]. The
     * Array will be the same size as this.
     */
    @Suppress("ArrayReturn")
    inline fun <reified R> map(transform: (T) -> R): Array<R> {
        contract { callsInPlace(transform) }
        return Array(size) { transform(get(it)) }
    }

    /**
     * Returns an [Array] of results of transforming each element in the [MutableVector]. The
     * Array will be the same size as this.
     */
    @Suppress("ArrayReturn")
    inline fun <reified R> mapIndexed(transform: (index: Int, T) -> R): Array<R> {
        contract { callsInPlace(transform) }
        return Array(size) { transform(it, get(it)) }
    }

    /**
     * Returns an [MutableVector] of results of transforming each element in the [MutableVector],
     * excluding those transformed values that are `null`.
     */
    inline fun <reified R> mapIndexedNotNull(transform: (index: Int, T) -> R?): MutableVector<R> {
        contract { callsInPlace(transform) }
        val arr = arrayOfNulls<R>(size)
        var targetSize = 0
        for (i in 0..lastIndex) {
            val target = transform(i, get(i))
            if (target != null) {
                arr[targetSize++] = target
            }
        }
        return MutableVector(arr, targetSize)
    }

    /**
     * Returns an [MutableVector] of results of transforming each element in the [MutableVector],
     * excluding those transformed values that are `null`.
     */
    inline fun <reified R> mapNotNull(transform: (T) -> R?): MutableVector<R> {
        contract { callsInPlace(transform) }
        val arr = arrayOfNulls<R>(size)
        var targetSize = 0
        for (i in 0..lastIndex) {
            val target = transform(get(i))
            if (target != null) {
                arr[targetSize++] = target
            }
        }
        return MutableVector(arr, targetSize)
    }

    /**
     * [add] [element] to the [MutableVector].
     */
    inline operator fun plusAssign(element: T) {
        add(element)
    }

    /**
     * [remove] [element] from the [MutableVector]
     */
    inline operator fun minusAssign(element: T) {
        remove(element)
    }

    /**
     * Removes [element] from the [MutableVector]. If [element] was in the [MutableVector]
     * and was removed, `true` will be returned, or `false` will be returned if the element
     * was not found.
     */
    fun remove(element: T): Boolean {
        val index = indexOf(element)
        if (index >= 0) {
            removeAt(index)
            return true
        }
        return false
    }

    /**
     * Removes all [elements] from the [MutableVector] and returns `true` if anything was removed.
     */
    fun removeAll(elements: List<T>): Boolean {
        val initialSize = size
        for (i in elements.indices) {
            remove(elements[i])
        }
        return initialSize != size
    }

    /**
     * Removes all [elements] from the [MutableVector] and returns `true` if anything was removed.
     */
    fun removeAll(elements: MutableVector<T>): Boolean {
        val initialSize = size
        for (i in 0..elements.lastIndex) {
            remove(elements.get(i))
        }
        return initialSize != size
    }

    /**
     * Removes all [elements] from the [MutableVector] and returns `true` if anything was removed.
     */
    fun removeAll(elements: Collection<T>): Boolean {
        if (elements.isEmpty()) {
            return false
        }
        val initialSize = size
        elements.forEach {
            remove(it)
        }
        return initialSize != size
    }

    /**
     * Removes the element at the given [index] and returns it.
     */
    fun removeAt(index: Int): T {
        val content = content
        val item = content[index] as T
        if (index != lastIndex) {
            content.copyInto(
                destination = content,
                destinationOffset = index,
                startIndex = index + 1,
                endIndex = size
            )
        }
        size--
        content[size] = null
        return item
    }

    /**
     * Removes items from index [start] (inclusive) to [end] (exclusive).
     */
    fun removeRange(start: Int, end: Int) {
        if (end > start) {
            if (end < size) {
                content.copyInto(
                    destination = content,
                    destinationOffset = start,
                    startIndex = end,
                    endIndex = size
                )
            }
            val newSize = size - (end - start)
            for (i in newSize..lastIndex) {
                content[i] = null // clean up the removed items
            }
            size = newSize
        }
    }

    /**
     * Keeps only [elements] in the [MutableVector] and removes all other values.
     */
    fun retainAll(elements: Collection<T>): Boolean {
        val initialSize = size
        for (i in lastIndex downTo 0) {
            val item = get(i)
            if (item !in elements) {
                removeAt(i)
            }
        }
        return initialSize != size
    }

    /**
     * Sets the value at [index] to [element].
     */
    operator fun set(index: Int, element: T): T {
        val content = content
        val old = content[index] as T
        content[index] = element
        return old
    }

    /**
     * Sorts the [MutableVector] using [comparator] to order the items.
     */
    fun sortWith(comparator: Comparator<T>) {
        (content as Array<T>).sortWith(comparator = comparator, fromIndex = 0, toIndex = size)
    }

    /**
     * Returns the sum of all values produced by [selector] for each element in the
     * [MutableVector].
     */
    inline fun sumBy(selector: (T) -> Int): Int {
        contract { callsInPlace(selector) }
        var sum = 0
        for (i in 0..lastIndex) {
            sum += selector(get(i))
        }
        return sum
    }

    @PublishedApi
    internal fun throwNoSuchElementException(): Nothing {
        throw NoSuchElementException("MutableVector contains no element matching the predicate.")
    }

    private class VectorListIterator<T>(
        private val list: MutableList<T>,
        private var index: Int
    ) : MutableListIterator<T> {

        override fun hasNext(): Boolean {
            return index < list.size
        }

        override fun next(): T {
            return list[index++]
        }

        override fun remove() {
            index--
            list.removeAt(index)
        }

        override fun hasPrevious(): Boolean {
            return index > 0
        }

        override fun nextIndex(): Int {
            return index
        }

        override fun previous(): T {
            index--
            return list[index]
        }

        override fun previousIndex(): Int {
            return index - 1
        }

        override fun add(element: T) {
            list.add(index, element)
            index++
        }

        override fun set(element: T) {
            list[index] = element
        }
    }

    /**
     * [MutableList] implementation for a [MutableVector], used in [asMutableList].
     */
    private class MutableVectorList<T>(private val vector: MutableVector<T>) : MutableList<T> {
        override val size: Int
            get() = vector.size

        override fun contains(element: T): Boolean = vector.contains(element)

        override fun containsAll(elements: Collection<T>): Boolean = vector.containsAll(elements)

        override fun get(index: Int): T = vector[index]

        override fun indexOf(element: T): Int = vector.indexOf(element)

        override fun isEmpty(): Boolean = vector.isEmpty()

        override fun iterator(): MutableIterator<T> = VectorListIterator(this, 0)

        override fun lastIndexOf(element: T): Int = vector.lastIndexOf(element)

        override fun add(element: T): Boolean = vector.add(element)

        override fun add(index: Int, element: T) = vector.add(index, element)

        override fun addAll(index: Int, elements: Collection<T>): Boolean =
            vector.addAll(index, elements)

        override fun addAll(elements: Collection<T>): Boolean = vector.addAll(elements)

        override fun clear() = vector.clear()

        override fun listIterator(): MutableListIterator<T> = VectorListIterator(this, 0)

        override fun listIterator(index: Int): MutableListIterator<T> =
            VectorListIterator(this, index)

        override fun remove(element: T): Boolean = vector.remove(element)

        override fun removeAll(elements: Collection<T>): Boolean = vector.removeAll(elements)

        override fun removeAt(index: Int): T = vector.removeAt(index)

        override fun retainAll(elements: Collection<T>): Boolean = vector.retainAll(elements)

        override fun set(index: Int, element: T): T = vector.set(index, element)

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
            SubList(this, fromIndex, toIndex)
    }

    /**
     * A view into an underlying [MutableList] that directly accesses the underlying [MutableList].
     * This is important for the implementation of [List.subList]. A change to the [SubList]
     * also changes the referenced [MutableList].
     */
    private class SubList<T>(
        private val list: MutableList<T>,
        private val start: Int,
        private var end: Int
    ) : MutableList<T> {
        override val size: Int
            get() = end - start

        override fun contains(element: T): Boolean {
            for (i in start until end) {
                if (list[i] == element) {
                    return true
                }
            }
            return false
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            elements.forEach {
                if (!contains(it)) {
                    return false
                }
            }
            return true
        }

        override fun get(index: Int): T = list[index + start]

        override fun indexOf(element: T): Int {
            for (i in start until end) {
                if (list[i] == element) {
                    return i - start
                }
            }
            return -1
        }

        override fun isEmpty(): Boolean = end == start

        override fun iterator(): MutableIterator<T> = VectorListIterator(this, 0)

        override fun lastIndexOf(element: T): Int {
            for (i in end - 1 downTo start) {
                if (list[i] == element) {
                    return i - start
                }
            }
            return -1
        }

        override fun add(element: T): Boolean {
            list.add(end++, element)
            return true
        }

        override fun add(index: Int, element: T) {
            list.add(index + start, element)
            end++
        }

        override fun addAll(index: Int, elements: Collection<T>): Boolean {
            list.addAll(index + start, elements)
            end += elements.size
            return elements.size > 0
        }

        override fun addAll(elements: Collection<T>): Boolean {
            list.addAll(end, elements)
            end += elements.size
            return elements.size > 0
        }

        override fun clear() {
            for (i in end - 1 downTo start) {
                list.removeAt(i)
            }
            end = start
        }

        override fun listIterator(): MutableListIterator<T> = VectorListIterator(this, 0)

        override fun listIterator(index: Int): MutableListIterator<T> =
            VectorListIterator(this, index)

        override fun remove(element: T): Boolean {
            for (i in start until end) {
                if (list[i] == element) {
                    list.removeAt(i)
                    end--
                    return true
                }
            }
            return false
        }

        override fun removeAll(elements: Collection<T>): Boolean {
            val originalEnd = end
            elements.forEach {
                remove(it)
            }
            return originalEnd != end
        }

        override fun removeAt(index: Int): T {
            val item = list.removeAt(index + start)
            end--
            return item
        }

        override fun retainAll(elements: Collection<T>): Boolean {
            val originalEnd = end
            for (i in end - 1 downTo start) {
                val item = list[i]
                if (item !in elements) {
                    list.removeAt(i)
                    end--
                }
            }
            return originalEnd != end
        }

        override fun set(index: Int, element: T): T = list.set(index + start, element)

        override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
            SubList(this, fromIndex, toIndex)
    }
}

/**
 * Create a [MutableVector] with a given initial [capacity].
 *
 * @see MutableVector.ensureCapacity
 */
@ExperimentalCollectionApi
inline fun <reified T> MutableVector(capacity: Int = 16) =
    MutableVector<T>(arrayOfNulls<T>(capacity), 0)

/**
 * Create a [MutableVector] with a given [size], initialiing each element using the [init]
 * function.
 *
 * [init] is called for each element in the [MutableVector], starting from the first one and should
 * return the value to be assigned to the element at its given index.
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalCollectionApi
inline fun <reified T> MutableVector(size: Int, noinline init: (Int) -> T): MutableVector<T> {
    contract { callsInPlace(init) }
    val arr = Array(size, init)
    return MutableVector(arr as Array<T?>, size)
}

/**
 * Creates an empty [MutableVector] with a [capacity][MutableVector.ensureCapacity] of 16.
 */
@ExperimentalCollectionApi
inline fun <reified T> mutableVectorOf() =
    MutableVector<T>()

/**
 * Creates a [MutableVector] with the given values. This will use the passed vararg [elements]
 * storage.
 */
@ExperimentalCollectionApi
inline fun <reified T> mutableVectorOf(vararg elements: T): MutableVector<T> {
    return MutableVector(
        elements as Array<T?>,
        elements.size
    )
}
