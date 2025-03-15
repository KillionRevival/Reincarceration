package org.kif.reincarceration.rewards;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data @Builder @ToString
public class CycleItem {
    final String material;
    final String name;
    final Map<String, Integer> enchantments;
    final List<String> lore;
    final Integer amount;
    final boolean stamped;
}
