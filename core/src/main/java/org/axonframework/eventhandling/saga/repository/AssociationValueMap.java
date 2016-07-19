/*
 * Copyright (c) 2010-2014. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.eventhandling.saga.repository;

import org.axonframework.eventhandling.saga.AssociationValue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * In-memory storage for AssociationValue to Saga mappings. A single AssociationValue can map to several Sagas, and a
 * single Saga can be mapped by several AssociationValues.
 * <p/>
 * Note that this "map" does not implement the Map interface. This is mainly due to the specific nature and intent of
 * this implementation. For example, the Map interface does not allow a single key to point to more than one value.
 * <p/>
 * This implementation is thread safe and has an expected average time cost of {@code log(n)}.
 *
 * @author Allard Buijze
 * @since 0.7
 */
public class AssociationValueMap {

    private final NavigableSet<SagaAssociationValue> mappings;

    /**
     * Initializes a new and empty AssociationValueMap.
     */
    public AssociationValueMap() {
        mappings = new ConcurrentSkipListSet<>(new AssociationValueComparator());
    }

    /**
     * Returns the identifiers of the Sagas that have been associated with the given {@code associationValue}.
     *
     * @param sagaType         The type of the associated Saga
     * @param associationValue The associationValue to find Sagas for
     * @return A set of Saga identifiers
     */
    public Set<String> findSagas(String sagaType, AssociationValue associationValue) {
        Set<String> identifiers = new HashSet<>();
        for (SagaAssociationValue item : mappings.tailSet(new SagaAssociationValue(associationValue, sagaType, null))) {
            if (!item.getKey().equals(associationValue.getKey())) {
                // we've had all relevant items
                break;
            }
            if (associationValue.equals(item.getAssociationValue()) && sagaType.equals(item.getSagaType())) {
                identifiers.add(item.getSagaIdentifier());
            }
        }
        return identifiers;
    }

    /**
     * Adds an association between the given {@code associationValue} and {@code sagaIdentifier}.
     *
     * @param associationValue The association value associated with the Saga
     * @param sagaType         The type of the associated Saga
     * @param sagaIdentifier   The identifier of the associated Saga
     */
    public void add(AssociationValue associationValue, String sagaType, String sagaIdentifier) {
        mappings.add(new SagaAssociationValue(associationValue, sagaType, sagaIdentifier));
    }

    /**
     * Removes an association between the given {@code associationValue} and {@code sagaIdentifier}.
     *
     * @param associationValue The association value associated with the Saga
     * @param sagaType         The type of the associated Saga
     * @param sagaIdentifier   The identifier of the associated Saga
     */
    public void remove(AssociationValue associationValue, String sagaType, String sagaIdentifier) {
        mappings.remove(new SagaAssociationValue(associationValue, sagaType, sagaIdentifier));
    }

    /**
     * Clears all the associations.
     */
    public void clear() {
        mappings.clear();
    }

    private static final class SagaAssociationValue {

        private final AssociationValue associationValue;
        private final String sagaType;
        private final String sagaIdentifier;

        private SagaAssociationValue(AssociationValue associationValue, String sagaType, String sagaIdentifier) {
            this.associationValue = associationValue;
            this.sagaType = sagaType;
            this.sagaIdentifier = sagaIdentifier;
        }

        public AssociationValue getAssociationValue() {
            return associationValue;
        }

        public String getSagaIdentifier() {
            return sagaIdentifier;
        }

        public String getKey() {
            return associationValue.getKey();
        }

        public Object getValue() {
            return associationValue.getValue();
        }

        public String getSagaType() {
            return sagaType;
        }
    }

    /**
     * Indicates whether any elements are contained within this map.
     *
     * @return {@code true} if this Map is empty, {@code false} if it contains any associations.
     */
    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    /**
     * Returns an approximation of the size of this map. Due to the concurrent nature of this map, size cannot return
     * an
     * accurate value.
     * <p/>
     * This is not a constant-time operation. The backing store of this map requires full traversal of elements to
     * calculate this size.
     *
     * @return an approximation of the number of elements in this map
     */
    public int size() {
        return mappings.size();
    }

    private static class AssociationValueComparator implements Comparator<SagaAssociationValue>, Serializable {

        private static final long serialVersionUID = -8733800489211327001L;

        @SuppressWarnings({"unchecked"})
        @Override
        public int compare(SagaAssociationValue o1, SagaAssociationValue o2) {
            int value = o1.getKey().compareTo(o2.getKey());
            if (value == 0 && !nullSafeEquals(o1.getValue(), o2.getValue())) {
                value = o1.getValue().getClass().getName().compareTo(o2.getValue().getClass().getName());
            }
            if (value == 0 && !nullSafeEquals(o1.getValue(), o2.getValue())) {
                // the objects are of the same class
                if (o1.getValue() instanceof Comparable) {
                    value = ((Comparable) o1.getValue()).compareTo(o2.getValue());
                } else {
                    value = o1.getValue().hashCode() - o2.getValue().hashCode();
                    if (value == 0 && o1.getValue() != o2.getValue()) {
                        value = o1.getValue().toString().compareTo(o2.getValue().toString());
                    }
                }
            }

            if (value == 0 && !nullSafeEquals(o1.getSagaType(), o2.getSagaType())) {
                if (o1.getSagaType() == null) {
                    return -1;
                } else if (o2.getSagaType() == null) {
                    return 1;
                }
                return o1.getSagaType().compareTo(o2.getSagaType());
            }

            if (value == 0 && !nullSafeEquals(o1.getSagaIdentifier(), o2.getSagaIdentifier())) {
                if (o1.getSagaIdentifier() == null) {
                    return -1;
                } else if (o2.getSagaIdentifier() == null) {
                    return 1;
                }
                return o1.getSagaIdentifier().compareTo(o2.getSagaIdentifier());
            }
            return value;
        }

        /**
         * Copied from Spring's ObjectUtils because Spring dependency is no longer allowed and I
         * did not want to introduce any regressions by handcrafting something worse.
         */
        protected static boolean nullSafeEquals(Object o1, Object o2) {
            if (o1 == o2) {
                return true;
            }
            if (o1 == null || o2 == null) {
                return false;
            }
            if (o1.equals(o2)) {
                return true;
            }
            if (o1.getClass().isArray() && o2.getClass().isArray()) {
                if (o1 instanceof Object[] && o2 instanceof Object[]) {
                    return Arrays.equals((Object[]) o1, (Object[]) o2);
                }
                if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
                    return Arrays.equals((boolean[]) o1, (boolean[]) o2);
                }
                if (o1 instanceof byte[] && o2 instanceof byte[]) {
                    return Arrays.equals((byte[]) o1, (byte[]) o2);
                }
                if (o1 instanceof char[] && o2 instanceof char[]) {
                    return Arrays.equals((char[]) o1, (char[]) o2);
                }
                if (o1 instanceof double[] && o2 instanceof double[]) {
                    return Arrays.equals((double[]) o1, (double[]) o2);
                }
                if (o1 instanceof float[] && o2 instanceof float[]) {
                    return Arrays.equals((float[]) o1, (float[]) o2);
                }
                if (o1 instanceof int[] && o2 instanceof int[]) {
                    return Arrays.equals((int[]) o1, (int[]) o2);
                }
                if (o1 instanceof long[] && o2 instanceof long[]) {
                    return Arrays.equals((long[]) o1, (long[]) o2);
                }
                if (o1 instanceof short[] && o2 instanceof short[]) {
                    return Arrays.equals((short[]) o1, (short[]) o2);
                }
            }
            return false;
        }

    }
}
