/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@code Multimap} whose keys and values are ordered by their natural ordering or
 * by supplied comparators. In all cases, this implementation uses {@link Comparable#compareTo} or
 * {@link Comparator#compare} instead of {@link Object#equals} to determine equivalence of
 * instances.
 *
 * <p><b>Warning:</b> The comparators or comparables used must be <i>consistent with equals</i> as
 * explained by the {@link Comparable} class specification. Otherwise, the resulting multiset will
 * violate the general contract of {@link SetMultimap}, which is specified in terms of {@link
 * Object#equals}.
 *
 * <p>The collections returned by {@code keySet} and {@code asMap} iterate through the keys
 * according to the key comparator ordering or the natural ordering of the keys. Similarly, {@code
 * get}, {@code removeAll}, and {@code replaceValues} return collections that iterate through the
 * values according to the value comparator ordering or the natural ordering of the values. The
 * collections generated by {@code entries}, {@code keys}, and {@code values} iterate across the
 * keys according to the above key ordering, and for each key they iterate across the values
 * according to the value ordering.
 *
 * <p>The multimap does not store duplicate key-value pairs. Adding a new key-value pair equal to an
 * existing key-value pair has no effect.
 *
 * <p>Null keys and values are permitted (provided, of course, that the respective comparators
 * support them). All optional multimap methods are supported, and all returned views are
 * modifiable.
 *
 * <p>This class is not threadsafe when any concurrent operations update the multimap. Concurrent
 * read operations will work correctly. To allow concurrent update operations, wrap your multimap
 * with a call to {@link Multimaps#synchronizedSortedSetMultimap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multimap"> {@code
 * Multimap}</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
public class TreeMultimap<K, V>
    extends AbstractSortedKeySortedSetMultimap<K, V> {
  private transient Comparator<? super K> keyComparator;
  private transient Comparator<? super V> valueComparator;

  /**
   * Creates an empty {@code TreeMultimap} ordered by the natural ordering of its keys and values.
   */
  public static <K extends Comparable, V extends Comparable> TreeMultimap<K, V> create() {
    return new TreeMultimap<>(Ordering.natural(), Ordering.natural());
  }

  /**
   * Creates an empty {@code TreeMultimap} instance using explicit comparators. Neither comparator
   * may be null; use {@link Ordering#natural()} to specify natural order.
   *
   * @param keyComparator the comparator that determines the key ordering
   * @param valueComparator the comparator that determines the value ordering
   */
  public static <K, V> TreeMultimap<K, V> create(
      Comparator<? super K> keyComparator, Comparator<? super V> valueComparator) {
    return new TreeMultimap<>(checkNotNull(keyComparator), checkNotNull(valueComparator));
  }

  /**
   * Constructs a {@code TreeMultimap}, ordered by the natural ordering of its keys and values, with
   * the same mappings as the specified multimap.
   *
   * @param multimap the multimap whose contents are copied to this multimap
   */
  public static <K extends Comparable, V extends Comparable> TreeMultimap<K, V> create(
      Multimap<? extends K, ? extends V> multimap) {
    return new TreeMultimap<>(Ordering.natural(), Ordering.natural(), multimap);
  }

  TreeMultimap(Comparator<? super K> keyComparator, Comparator<? super V> valueComparator) {
    super(new TreeMap<K, Collection<V>>(keyComparator));
    this.keyComparator = keyComparator;
    this.valueComparator = valueComparator;
  }

  private TreeMultimap(
      Comparator<? super K> keyComparator,
      Comparator<? super V> valueComparator,
      Multimap<? extends K, ? extends V> multimap) {
    this(keyComparator, valueComparator);
    putAll(multimap);
  }

  @Override
  Map<K, Collection<V>> createAsMap() {
    return createMaybeNavigableAsMap();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Creates an empty {@code TreeSet} for a collection of values for one key.
   *
   * @return a new {@code TreeSet} containing a collection of values for one key
   */
  @Override
  SortedSet<V> createCollection() {
    return new TreeSet<V>(valueComparator);
  }

  @Override
  Collection<V> createCollection(K key) {
    if (key == null) {
      keyComparator().compare(key, key);
    }
    return super.createCollection(key);
  }

  /**
   * Returns the comparator that orders the multimap keys.
   *
   * @deprecated Use {@code ((NavigableSet<K>) multimap.keySet()).comparator()} instead.
   */
  @Deprecated
  public Comparator<? super K> keyComparator() {
    return keyComparator;
  }

  @Override
  public Comparator<? super V> valueComparator() {
    return valueComparator;
  }

  /** @since 14.0 (present with return type {@code SortedSet} since 2.0) */
  @Override
  @GwtIncompatible // NavigableSet
  public NavigableSet<V> get(K key) {
    return (NavigableSet<V>) super.get(key);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Because a {@code TreeMultimap} has unique sorted keys, this method returns a {@link
   * NavigableSet}, instead of the {@link java.util.Set} specified in the {@link Multimap}
   * interface.
   *
   * @since 14.0 (present with return type {@code SortedSet} since 2.0)
   */
  @Override
  public NavigableSet<K> keySet() {
    return (NavigableSet<K>) super.keySet();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Because a {@code TreeMultimap} has unique sorted keys, this method returns a {@link
   * NavigableMap}, instead of the {@link java.util.Map} specified in the {@link Multimap}
   * interface.
   *
   * @since 14.0 (present with return type {@code SortedMap} since 2.0)
   */
  @Override
  public NavigableMap<K, Collection<V>> asMap() {
    return (NavigableMap<K, Collection<V>>) super.asMap();
  }

  /**
   * @serialData key comparator, value comparator, number of distinct keys, and then for each
   *     distinct key: the key, number of values for that key, and key values
   */
  @GwtIncompatible // java.io.ObjectOutputStream
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(keyComparator());
    stream.writeObject(valueComparator());
    Serialization.writeMultimap(this, stream);
  }

  @GwtIncompatible // java.io.ObjectInputStream
  @SuppressWarnings("unchecked") // reading data stored by writeObject
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    keyComparator = checkNotNull((Comparator<? super K>) stream.readObject());
    valueComparator = checkNotNull((Comparator<? super V>) stream.readObject());
    setMap(new TreeMap<K, Collection<V>>(keyComparator));
    Serialization.populateMultimap(this, stream);
  }

  @GwtIncompatible // not needed in emulated source
  private static final long serialVersionUID = 0;
}
