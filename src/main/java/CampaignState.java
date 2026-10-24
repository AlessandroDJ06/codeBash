import java.util.ArrayList;
import java.util.List;

final class CampaignState {
    final Campaign campaign;
    final List<HeroState> heroes;
    final List<EnemyState> enemies;
    int battleIndex;
    int round;
    int turnPos;
    long score;
    int attackCounter;
    Phase phase;
    int[] order;
    PathNode path;

    private CampaignState(Campaign campaign, List<HeroState> heroes, List<EnemyState> enemies) {
        this.campaign = campaign;
        this.heroes = heroes;
        this.enemies = enemies;
    }

    static CampaignState initial(Campaign campaign) {
        List<HeroState> heroes = new ArrayList<>();
        for (int i = 0; i < campaign.heroes().size(); i++) heroes.add(new HeroState(i, campaign.heroes().get(i)));
        CampaignState state = new CampaignState(campaign, heroes, new ArrayList<>());
        Rules.beginBattle(state);
        return state;
    }

    CampaignState copy() {
        List<HeroState> heroCopies = new ArrayList<>(heroes.size());
        for (HeroState hero : heroes) heroCopies.add(hero.copy());
        List<EnemyState> enemyCopies = new ArrayList<>(enemies.size());
        for (EnemyState enemy : enemies) enemyCopies.add(enemy.copy());
        CampaignState copy = new CampaignState(campaign, heroCopies, enemyCopies);
        copy.battleIndex = battleIndex;
        copy.round = round;
        copy.turnPos = turnPos;
        copy.score = score;
        copy.attackCounter = attackCounter;
        copy.phase = phase;
        copy.order = order;
        copy.path = path;
        return copy;
    }

    BattleDef battle() { return campaign.battles().get(battleIndex); }

    boolean isHeroSlot(int slot) { return slot < heroes.size(); }

    Fighter fighterAt(int slot) {
        return isHeroSlot(slot) ? heroes.get(slot) : enemies.get(slot - heroes.size());
    }

    HeroState currentHero() { return heroes.get(order[turnPos]); }

    boolean allEnemiesDead() {
        for (EnemyState enemy : enemies) if (enemy.isAlive()) return false;
        return true;
    }

    boolean allHeroesDead() {
        for (HeroState hero : heroes) if (hero.isAlive()) return false;
        return true;
    }

    void log(String line) { path = new PathNode(line, path); }

    long fingerprint() {
        long hash = mix(mix(mix(battleIndex, round), turnPos), score);
        hash = mix(hash, phase.ordinal());
        for (HeroState hero : heroes) hash = mixHero(hash, hero);
        for (EnemyState enemy : enemies) hash = mixEnemy(hash, enemy);
        return hash;
    }

    private static long mixHero(long hash, HeroState hero) {
        hash = mix(mix(mix(hash, hero.hp), hero.maxHp), mix(hero.initiative, hero.damageBonus));
        for (ActionSlot slot : hero.slots) {
            hash = mix(hash, slot.actionId.hashCode());
            hash = mix(hash, slot.availableFromRound);
        }
        return hero.effects.mixInto(hash);
    }

    private static long mixEnemy(long hash, EnemyState enemy) {
        hash = mix(mix(hash, enemy.hp), enemy.rotationIndex);
        for (int sequence : enemy.lastAttackSequence) hash = mix(hash, sequence);
        return enemy.effects.mixInto(hash);
    }

    static long mix(long hash, long value) {
        return (hash ^ value) * 0x100000001B3L + 0x9E3779B97F4A7C15L;
    }
}
