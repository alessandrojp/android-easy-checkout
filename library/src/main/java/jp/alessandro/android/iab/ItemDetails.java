/*
 *  Copyright (C) 2016 Alessandro Yuichi Okimoto
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  Contact email: alessandro@alessandro.jp
 */

package jp.alessandro.android.iab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemDetails {

    private final Map<String, Item> mMap = new LinkedHashMap<>();

    public ItemDetails() {
    }

    public boolean hasItemId(String itemId) {
        return mMap.containsKey(itemId);
    }

    public List<Item> getAll() {
        return new ArrayList<>(mMap.values());
    }

    public Item getByItemId(String itemId) {
        return mMap.get(itemId);
    }

    public int getSize() {
        return mMap.size();
    }

    void put(Item item) {
        mMap.put(item.getSku(), item);
    }
}