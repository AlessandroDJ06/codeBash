import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

final class TargetSelector {
    private TargetSelector() {}

    static Fighter choose(CampaignState state, EnemyState actor, TargetKind kind, List<Criterion> criteria) {
        List<Fighter> candidates = candidates(state, actor, kind);
        for (Criterion criterion : criteria) {
            if (candidates.size() <= 1) break;
            candidates = mostPreferred(candidates, preference(criterion, state, actor));
        }
        return candidates.stream().min(Comparator.comparing(f -> f.id)).orElse(null);
    }

    private static List<Fighter> candidates(CampaignState state, EnemyState actor, TargetKind kind) {
        List<Fighter> candidates = new ArrayList<>();
        switch (kind) {
            case ENEMY -> state.heroes.stream().filter(Fighter::isAlive).forEach(candidates::add);
            case ALLY -> state.enemies.stream().filter(Fighter::isAlive).forEach(candidates::add);
            case SELF -> candidates.add(actor);
        }
        return candidates;
    }

    private static List<Fighter> mostPreferred(List<Fighter> candidates, Comparator<Fighter> preference) {
        Fighter top = candidates.stream().min(preference).orElseThrow();
        return candidates.stream().filter(f -> preference.compare(f, top) == 0).toList();
    }

    private static Comparator<Fighter> preference(Criterion criterion, CampaignState state, EnemyState actor) {
        int round = state.round;
        return switch (criterion) {
            case SELF -> by(f -> f == actor ? 0 : 1);
            case LOWEST_HP -> by(f -> f.hp);
            case HIGHEST_HP -> by(f -> -f.hp);
            case LOWEST_HP_PERCENT -> lowestHpPercent();
            case HIGHEST_HP_PERCENT -> lowestHpPercent().reversed();
            case MOST_MISSING_HP -> by(f -> f.hp - f.maxHp);
            case HIGHEST_INITIATIVE -> by(f -> -f.initiative);
            case LOWEST_INITIATIVE -> by(f -> f.initiative);
            case LAST_ATTACKER -> by(f -> -lastAttackSequence(actor, f));
            case MOST_SHIELD -> by(f -> -f.effects.shieldTotal(round));
            case LEAST_SHIELD -> by(f -> f.effects.shieldTotal(round));
            case MOST_BUFFS -> by(f -> -f.effects.buffCount(round));
            case MOST_DEBUFFS -> by(f -> -f.effects.debuffCount(round));
            case STUNNED -> by(f -> f.effects.isStunned(round) ? 0 : 1);
        };
    }

    private static int lastAttackSequence(EnemyState actor, Fighter candidate) {
        if (!(candidate instanceof HeroState hero)) return 0;
        return actor.lastAttackSequence[hero.index];
    }

    private static Comparator<Fighter> lowestHpPercent() {
        return (a, b) -> Long.compare((long) a.hp * b.maxHp, (long) b.hp * a.maxHp);
    }

    private static Comparator<Fighter> by(ToIntFunction<Fighter> key) {
        return Comparator.comparingInt(key);
    }
}
