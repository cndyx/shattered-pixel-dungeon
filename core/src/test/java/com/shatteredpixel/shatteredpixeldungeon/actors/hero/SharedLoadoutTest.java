package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Cleanse;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Sunray;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import static org.junit.Assert.*;

public class SharedLoadoutTest {
    @After public void cleanup(){ Dungeon.hero = null; }

    @Test public void sharedTalentsAreAvailableToEveryClassAndDoNotResetPoints(){
        for (HeroClass cls : HeroClass.values()){
            ArrayList<LinkedHashMap<Talent,Integer>> talents = new ArrayList<>();
            Talent.initClassTalents(cls, talents);
            assertTrue(cls.name(), talents.get(0).containsKey(Talent.IRON_WILL));
            assertTrue(talents.get(0).containsKey(Talent.SATIATED_SPELLS));
            assertTrue(talents.get(0).containsKey(Talent.HOLY_INTUITION));
            assertTrue(talents.get(0).containsKey(Talent.SEARING_LIGHT));
            assertTrue(talents.get(0).containsKey(Talent.SHIELD_OF_LIGHT));
            for (Talent t : new Talent[]{Talent.LIQUID_WILLPOWER, Talent.RUNIC_TRANSFERENCE,
                    Talent.ENLIGHTENING_MEAL, Talent.RECALL_INSCRIPTION, Talent.SUNRAY,
                    Talent.DIVINE_SENSE, Talent.BLESS}) assertTrue(talents.get(1).containsKey(t));
            assertTrue(talents.get(2).containsKey(Talent.CLEANSE));
            assertTrue(talents.get(2).containsKey(Talent.LIGHT_READING));
            talents.get(0).put(Talent.IRON_WILL, 2);
            Talent.initClassTalents(cls, talents);
            assertEquals(Integer.valueOf(2), talents.get(0).get(Talent.IRON_WILL));
        }
    }

    @Test public void nonClericCanUnlockTomeSpells(){
        Hero hero = new Hero();
        hero.heroClass = HeroClass.MAGE;
        Talent.initClassTalents(hero);
        hero.talents.get(1).put(Talent.SUNRAY, 2);
        hero.talents.get(2).put(Talent.CLEANSE, 3);
        assertTrue(ClericSpell.getSpellList(hero, 2).contains(Sunray.INSTANCE));
        assertTrue(ClericSpell.getSpellList(hero, 3).contains(Cleanse.INSTANCE));
    }

    @Test public void anyAccessoryFitsAnySlotAndFourthIsIterable(){
        Hero hero = new Hero();
        for (int slot=0; slot<Belongings.MISC_SLOTS; slot++){
            RingOfMight ring = new RingOfMight();
            hero.belongings.setMiscSlot(slot, ring);
            assertTrue(ring.isEquipped(hero));
            assertSame(ring, hero.belongings.getItem(RingOfMight.class));
            hero.belongings.setMiscSlot(slot, null);
            HolyTome tome = new HolyTome();
            hero.belongings.setMiscSlot(slot, tome);
            assertTrue(tome.isEquipped(hero));
            hero.belongings.setMiscSlot(slot, null);
        }
        for (int slot=0; slot<4; slot++) hero.belongings.setMiscSlot(slot, new RingOfMight());
        assertEquals(4, hero.belongings.getAllItems(RingOfMight.class).size());
        assertEquals(4, hero.belongings.equippedMiscs().size());
        assertEquals(4, ((RingOfMight)hero.belongings.ring).combinedBonus(hero));
    }

    @Test public void iteratorRemovesFourthSlotAndBackpackSeparately(){
        Hero hero = new Hero();
        KindofMisc fourth = new RingOfMight();
        hero.belongings.misc2 = fourth;
        Item carried = new Item();
        hero.belongings.backpack.items.add(carried);
        Iterator<Item> it = hero.belongings.iterator();
        assertSame(fourth, it.next());
        it.remove();
        assertNull(hero.belongings.misc2);
        assertSame(carried, it.next());
        it.remove();
        assertTrue(hero.belongings.backpack.items.isEmpty());
        assertFalse(it.hasNext());
    }

    @Test public void fourthSlotRespectsLostInventoryAndClear(){
        Hero hero = new Hero();
        KindofMisc fourth = new RingOfMight();
        hero.belongings.misc2 = fourth;
        hero.belongings.lostInventory(true);
        assertFalse(fourth.isEquipped(hero));
        assertTrue(hero.belongings.equippedMiscs().isEmpty());
        hero.belongings.lostInventory(false);
        assertTrue(fourth.isEquipped(hero));
        hero.belongings.clear();
        assertTrue(hero.belongings.equippedMiscs().isEmpty());
    }

    // A no-effects accessory isolates serialization/activation from rendering and audio.
    public static class SavedAccessory extends KindofMisc {
        boolean activated;
        @Override public void activate(com.shatteredpixel.shatteredpixeldungeon.actors.Char hero){ activated = true; }
    }

    @Test public void fourSlotsRestoreAndActivateEvenWithOldTypedSaveNames(){
        Hero hero = new Hero();
        for (int slot=0; slot<4; slot++) hero.belongings.setMiscSlot(slot,new SavedAccessory());
        Bundle bundle = new Bundle();
        hero.belongings.storeInBundle(bundle);
        Hero restored = new Hero();
        restored.belongings.restoreFromBundle(bundle);
        assertEquals(4,restored.belongings.equippedMiscs().size());
        for (KindofMisc item : restored.belongings.miscSlots()) assertTrue(((SavedAccessory)item).activated);
    }

    @Test public void oldThreeSlotSaveLoadsWithEmptyFourthSlot(){
        Hero hero = new Hero();
        Bundle bundle = new Bundle();
        hero.belongings.backpack.storeInBundle(bundle);
        bundle.put("artifact",new SavedAccessory());
        bundle.put("misc",new SavedAccessory());
        bundle.put("ring",new SavedAccessory());
        hero.belongings.restoreFromBundle(bundle);
        assertEquals(3,hero.belongings.equippedMiscs().size());
        assertNull(hero.belongings.misc2);
    }
}
