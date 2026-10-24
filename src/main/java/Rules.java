import java.util.Comparator;
import java.util.stream.IntStream;

final class Rules {
    private Rules() {}

    static void beginBattle(CampaignState state) {
        BattleDef battle = state.battle();
        state.enemies.clear();
        for (int i = 0; i < battle.enemies().size(); i++) {
            state.enemies.add(new EnemyState(i, battle.enemies().get(i), state.heroes.size()));
        }
        state.round = 1;
        state.turnPos = 0;
        state.phase = Phase.PLAYING;
        state.order = turnOrder(state);
        settle(state);
    }

    static void applyHeroCommand(CampaignState state, Command command) {
        HeroState hero = state.heroes.get(command.heroIndex());
        ActionDef action = state.campaign.actions().get(command.actionId());
        Fighter target = command.targetsHero() ? state.heroes.get(command.targetIndex()) : state.enemies.get(command.targetIndex());
        startCooldown(state, hero, action);
        state.log("%s %d %s %s %s".formatted(state.battle().id(), state.round, hero.id, action.id(), target.id));
        perform(state, hero, action, target);
        checkBattleEnd(state);
        skipHero(state);
    }

    static void skipHero(CampaignState state) {
        if (state.phase != Phase.PLAYING) return;
        state.turnPos++;
        settle(state);
    }

    static void applyReward(CampaignState state, RewardChoice choice) {
        if (choice.reward() != null) claimReward(state, choice);
        advanceBattle(state);
    }

    private static void startCooldown(CampaignState state, HeroState hero, ActionDef action) {
        for (ActionSlot slot : hero.slots) {
            if (!slot.actionId.equals(action.id()) || slot.availableFromRound > state.round) continue;
            slot.availableFromRound = state.round + Math.max(1, action.cooldown());
            return;
        }
    }

    private static void settle(CampaignState state) {
        while (state.phase == Phase.PLAYING) {
            if (state.turnPos >= state.order.length) {
                endRound(state);
                continue;
            }
            int slot = state.order[state.turnPos];
            Fighter actor = state.fighterAt(slot);
            if (!actor.isAlive()) {
                state.turnPos++;
                continue;
            }
            if (actor.effects.isStunned(state.round)) {
                skipStunned(state, actor);
                continue;
            }
            if (!state.isHeroSlot(slot)) {
                enemyTurn(state, (EnemyState) actor);
                state.turnPos++;
                continue;
            }
            if (!((HeroState) actor).hasAvailableAction(state.round)) {
                state.turnPos++;
                continue;
            }
            return;
        }
    }

    private static void skipStunned(CampaignState state, Fighter actor) {
        if (actor instanceof EnemyState enemy) advanceRotation(enemy);
        state.turnPos++;
    }

    private static void endRound(CampaignState state) {
        for (HeroState hero : state.heroes) hero.effects.pruneExpired(state.round);
        for (EnemyState enemy : state.enemies) enemy.effects.pruneExpired(state.round);
        state.round++;
        state.turnPos = 0;
        if (state.round > state.battle().maxRounds()) fail(state);
    }

    private static void enemyTurn(CampaignState state, EnemyState enemy) {
        if (enemy.def.rotation().isEmpty()) return;
        RotationEntry entry = enemy.def.rotation().get(enemy.rotationIndex);
        advanceRotation(enemy);
        ActionDef action = state.campaign.actions().get(entry.actionId());
        Fighter target = TargetSelector.choose(state, enemy, action.targetKind(), entry.criteria());
        if (target == null) return;
        perform(state, enemy, action, target);
        checkBattleEnd(state);
    }

    private static void advanceRotation(EnemyState enemy) {
        int size = enemy.def.rotation().size();
        if (size == 0) return;
        enemy.rotationIndex = (enemy.rotationIndex + 1) % size;
    }

    private static void perform(CampaignState state, Fighter actor, ActionDef action, Fighter target) {
        recordHostileAttempt(state, actor, action, target);
        if (!target.isAlive()) return;
        int round = state.round;
        switch (action.type()) {
            case DAMAGE -> dealDamage(state, actor, action, target);
            case HEAL -> target.hp = Math.min(target.maxHp, target.hp + action.value());
            case SHIELD -> target.effects.addShield(action.value(), round + action.duration());
            case BUFF_ATTACK -> target.effects.addBuff(action.value(), round + action.duration());
            case DEBUFF_ATTACK -> target.effects.addDebuff(action.value(), round + action.duration());
            case STUN -> target.effects.stunUntil(round + action.duration());
            case DISPEL -> dispel(action, target);
        }
    }

    private static void dealDamage(CampaignState state, Fighter actor, ActionDef action, Fighter target) {
        int round = state.round;
        int amount = Math.max(0, action.value() + actor.damageBonus()
                + actor.effects.buffTotal(round) - actor.effects.debuffTotal(round));
        int throughShields = target.effects.absorb(amount, round);
        target.hp = Math.max(0, target.hp - throughShields);
        if (!target.isAlive() && target instanceof EnemyState enemy) state.score += enemy.def.reward();
    }

    private static void dispel(ActionDef action, Fighter target) {
        if (action.targetKind() == TargetKind.ENEMY) target.effects.dispelHostile();
        else target.effects.dispelFriendly();
    }

    private static void recordHostileAttempt(CampaignState state, Fighter actor, ActionDef action, Fighter target) {
        if (!isHostile(action)) return;
        if (actor instanceof HeroState hero && target instanceof EnemyState enemy) {
            enemy.lastAttackSequence[hero.index] = ++state.attackCounter;
        }
    }

    static boolean isHostile(ActionDef action) {
        if (action.targetKind() != TargetKind.ENEMY) return false;
        return switch (action.type()) {
            case DAMAGE, DEBUFF_ATTACK, STUN, DISPEL -> true;
            default -> false;
        };
    }

    private static void checkBattleEnd(CampaignState state) {
        if (state.phase != Phase.PLAYING) return;
        if (state.allEnemiesDead()) win(state);
        else if (state.allHeroesDead()) fail(state);
    }

    private static void win(CampaignState state) {
        BattleDef battle = state.battle();
        state.score += Scoring.completionBonus(battle.completionReward(), battle.maxRounds(), state.round);
        state.phase = Phase.BATTLE_WON;
    }

    private static void fail(CampaignState state) {
        for (EnemyState enemy : state.enemies) if (enemy.isAlive()) state.score -= enemy.def.reward();
        state.phase = Phase.FAILED;
    }

    private static void claimReward(CampaignState state, RewardChoice choice) {
        RewardDef reward = choice.reward();
        HeroState target = choice.heroIndex() >= 0 ? state.heroes.get(choice.heroIndex()) : null;
        String targetSuffix = target == null ? "" : " " + target.id;
        state.log("%s PICK_REWARD %s%s".formatted(state.battle().id(), reward.id(), targetSuffix));
        state.score -= reward.cost();
        switch (reward.type()) {
            case SCORE_BONUS -> state.score += reward.value();
            case RESTORE_HP_PARTY -> state.heroes.stream().filter(Fighter::isAlive).forEach(h -> heal(h, reward.value()));
            case RESTORE_HP_SINGLE -> heal(target, reward.value());
            case REVIVE_HERO -> target.hp = Math.min(reward.value(), target.maxHp);
            case UPGRADE_DAMAGE -> target.damageBonus += reward.value();
            case UPGRADE_MAX_HP -> upgradeMaxHp(target, reward.value());
            case UPGRADE_INITIATIVE -> target.initiative += reward.value();
            case LEARN_ACTION -> target.slots.add(new ActionSlot(reward.actionId(), 1));
        }
    }

    private static void heal(Fighter fighter, int amount) {
        fighter.hp = Math.min(fighter.maxHp, fighter.hp + amount);
    }

    private static void upgradeMaxHp(HeroState hero, int amount) {
        hero.maxHp += amount;
        if (hero.isAlive()) hero.hp += amount;
    }

    private static void advanceBattle(CampaignState state) {
        for (HeroState hero : state.heroes) hero.rebaseForNextBattle(state.round);
        state.battleIndex++;
        if (state.battleIndex >= state.campaign.battles().size()) {
            state.phase = Phase.COMPLETED;
            return;
        }
        beginBattle(state);
    }

    private static int[] turnOrder(CampaignState state) {
        int total = state.heroes.size() + state.enemies.size();
        return IntStream.range(0, total).boxed()
                .sorted(turnComparator(state))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    private static Comparator<Integer> turnComparator(CampaignState state) {
        return Comparator.<Integer>comparingInt(slot -> -state.fighterAt(slot).initiative)
                .thenComparingInt(slot -> state.isHeroSlot(slot) ? 0 : 1)
                .thenComparing(slot -> state.fighterAt(slot).id);
    }
}
