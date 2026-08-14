package com.alkacode.items.model;

import com.alkacode.items.effect.EffectTrigger;
import com.alkacode.items.effect.EffectType;

public record ItemEffect(EffectType type, EffectTrigger trigger, ParamMap params) {
}
