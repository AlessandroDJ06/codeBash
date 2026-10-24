import java.util.ArrayList;
import java.util.List;

abstract class Fighter {
    final int index;
    final String id;
    int hp;
    int maxHp;
    int initiative;
    Effects effects;

    Fighter(int index, String id, int hp, int maxHp, int initiative, Effects effects) {
        this.index = index;
        this.id = id;
        this.hp = hp;
        this.maxHp = maxHp;
        this.initiative = initiative;
        this.effects = effects;
    }

    boolean isAlive() { return hp > 0; }

    int damageBonus() { return 0; }
}

final class ActionSlot {
    final String actionId;
    int availableFromRound;

    ActionSlot(String actionId, int availableFromRound) {
        this.actionId = actionId;
        this.availableFromRound = availableFromRound;
    }

    ActionSlot copy() { return new ActionSlot(actionId, availableFromRound); }
}

final class HeroState extends Fighter {
    int damageBonus;
    final List<ActionSlot> slots = new ArrayList<>();

    HeroState(int index, HeroDef def) {
        super(index, def.id(), def.maxHp(), def.maxHp(), def.initiative(), new Effects());
        for (String actionId : def.actionIds()) slots.add(new ActionSlot(actionId, 1));
    }

    private HeroState(HeroState other) {
        super(other.index, other.id, other.hp, other.maxHp, other.initiative, other.effects.copy());
        this.damageBonus = other.damageBonus;
        for (ActionSlot slot : other.slots) slots.add(slot.copy());
    }

    HeroState copy() { return new HeroState(this); }

    @Override
    int damageBonus() { return damageBonus; }

    boolean hasAvailableAction(int round) {
        for (ActionSlot slot : slots) if (slot.availableFromRound <= round) return true;
        return false;
    }

    void rebaseForNextBattle(int lastRound) {
        effects = effects.forNextBattle(lastRound);
        for (ActionSlot slot : slots) slot.availableFromRound = Math.max(1, slot.availableFromRound - lastRound);
    }
}

final class EnemyState extends Fighter {
    final EnemyDef def;
    int rotationIndex;
    final int[] lastAttackSequence;

    EnemyState(int index, EnemyDef def, int heroCount) {
        super(index, def.id(), def.hp(), def.hp(), def.initiative(), new Effects());
        this.def = def;
        this.lastAttackSequence = new int[heroCount];
    }

    private EnemyState(EnemyState other) {
        super(other.index, other.id, other.hp, other.maxHp, other.initiative, other.effects.copy());
        this.def = other.def;
        this.rotationIndex = other.rotationIndex;
        this.lastAttackSequence = other.lastAttackSequence.clone();
    }

    EnemyState copy() { return new EnemyState(this); }
}
