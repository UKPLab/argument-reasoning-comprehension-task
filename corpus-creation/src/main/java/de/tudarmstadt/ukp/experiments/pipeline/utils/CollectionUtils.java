/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
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

package de.tudarmstadt.ukp.experiments.pipeline.utils;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.util.*;

/**
 * @author Ivan Habernal
 */
public class CollectionUtils
{
    /**
     * Sorts map by value (http://stackoverflow.com/a/2581754)
     *
     * @param map map
     * @param <K> key
     * @param <V> value
     * @return sorted map by value
     */
    public static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> sortByValue(
            Map<K, V> map, final boolean asc)
    {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
            {
                if (asc) {
                    return (o1.getValue()).compareTo(o2.getValue());
                }
                else {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            }
        });

        LinkedHashMap<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * If the map contains the key, it returns its value. If there is no such key, a new
     * entry will be added and returned.
     *
     * @param map          map
     * @param key          key
     * @param defaultValue defaultValue
     * @param <K>          key
     * @param <V>          value
     * @return value
     */
    public static <K, V> V put(Map<K, V> map, K key, V defaultValue)
    {
        if (!map.containsKey(key)) {
            map.put(key, defaultValue);
        }

        return map.get(key);
    }

    public static DescriptiveStatistics frequencyToStatistics(Frequency frequency)
    {
        Iterator<Comparable<?>> comparableIterator = frequency.valuesIterator();
        DescriptiveStatistics result = new DescriptiveStatistics();
        while (comparableIterator.hasNext()) {
            Comparable<?> next = comparableIterator.next();
            long count = frequency.getCount(next);

            for (int i = 0; i < count; i++) {
                if (next instanceof Number) {
                    result.addValue(((Number) next).doubleValue());
                }
            }
        }

        return result;
    }
}
