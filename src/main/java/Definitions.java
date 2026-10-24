import java.util.List;
import java.util.Map;

enum ActionType { DAMAGE, HEAL, SHIELD, BUFF_ATTACK, DEBUFF_ATTACK, STUN, DISPEL }

enum TargetKind { ENEMY, ALLY, SELF }

enum Criterion {
    SELF, LOWEST_HP, HIGHEST_HP, LOWEST_HP_PERCENT, HIGHEST_HP_PERCENT, MOST_MISSING_HP,
    HIGHEST_INITIATIVE, LOWEST_INITIATIVE, LAST_ATTACKER, MOST_SHIELD, LEAST_SHIELD,
    MOST_BUFFS, MOST_DEBUFFS, STUNNED
}

enum RewardType {
    SCORE_BONUS, RESTORE_HP_PARTY, RESTORE_HP_SINGLE, REVIVE_HERO,
    UPGRADE_DAMAGE, UPGRADE_MAX_HP, UPGRADE_INITIATIVE, LEARN_ACTION
}

record ActionDef(String id, ActionType type, int value, TargetKind targetKind, int duration, int cooldown) {}

record HeroDef(String id, String name, int maxHp, int initiative, List<String> actionIds) {}

record RotationEntry(String actionId, List<Criterion> criteria) {}

record EnemyDef(String id, String name, int hp, int initiative, int reward, List<RotationEntry> rotation) {}

record RewardDef(String id, RewardType type, int value, int cost, String actionId) {}

record BattleDef(String id, int maxRounds, int completionReward, List<EnemyDef> enemies, List<RewardDef> rewards) {}

record Campaign(Map<String, ActionDef> actions, List<HeroDef> heroes, List<BattleDef> battles) {}

record Command(int heroIndex, String actionId, boolean targetsHero, int targetIndex) {}

record RewardChoice(RewardDef reward, int heroIndex) {}

record PathNode(String line, PathNode previous) {}

enum Phase { PLAYING, BATTLE_WON, FAILED, COMPLETED }
