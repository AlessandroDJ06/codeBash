final class Heuristic {
    private static final double PARTIAL_PROGRESS_WEIGHT = 0.5;
    private static final double HERO_HEALTH_WEIGHT = 0.25;

    private Heuristic() {}

    static double evaluate(CampaignState state) {
        BattleDef battle = state.battle();
        double value = state.score + heroHealth(state, battle);
        if (state.phase != Phase.PLAYING) return value;
        return value + enemyProgress(state, battle)
                + Scoring.completionBonus(battle.completionReward(), battle.maxRounds(), state.round);
    }

    private static double enemyProgress(CampaignState state, BattleDef battle) {
        double sharePerEnemy = (double) battle.completionReward() / battle.enemies().size();
        double progress = 0;
        for (EnemyState enemy : state.enemies) {
            double damaged = 1.0 - (double) enemy.hp / enemy.maxHp;
            progress += damaged * (enemy.def.reward() + sharePerEnemy) * PARTIAL_PROGRESS_WEIGHT;
        }
        return progress;
    }

    private static double heroHealth(CampaignState state, BattleDef battle) {
        double totalReward = battle.completionReward();
        for (EnemyDef enemy : battle.enemies()) totalReward += enemy.reward();
        double weightPerHero = HERO_HEALTH_WEIGHT * totalReward / state.heroes.size();
        double health = 0;
        for (HeroState hero : state.heroes) health += (double) hero.hp / hero.maxHp;
        return health * weightPerHero;
    }
}
