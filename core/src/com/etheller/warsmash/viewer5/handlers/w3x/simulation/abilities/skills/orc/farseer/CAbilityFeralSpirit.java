package com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.orc.farseer;

import java.util.ArrayList;
import java.util.List;

import com.etheller.warsmash.units.manager.MutableObjectData.MutableGameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitClassification;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.CAbilityNoTargetSpellBase;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.skills.util.CBuffTimedLife;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbilityFields;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.definitions.impl.AbstractCAbilityTypeDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIds;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;

public class CAbilityFeralSpirit extends CAbilityNoTargetSpellBase {
	private War3ID summonUnitId;
	private int summonUnitCount;
	private War3ID buffId;
	private float duration;
	private float areaOfEffect;

	// TODO maybe "lastSummonHandleIds" instead, for ease of use with saving game,
	// but then we have to track when they die or else risk re-used handle ID
	// messing us up
	private final List<CUnit> lastSummonUnits = new ArrayList<>();

	public CAbilityFeralSpirit(final int handleId, final War3ID alias) {
		super(handleId, alias);
	}

	@Override
	public void populateData(final MutableGameObject worldEditorAbility, final int level) {
		final String unitTypeOne = worldEditorAbility.getFieldAsString(AbilityFields.FERAL_SPIRIT_SUMMON_UNIT_TYPE_1,
				level);
		this.summonUnitId = unitTypeOne.length() == 4 ? War3ID.fromString(unitTypeOne) : War3ID.NONE;
		this.summonUnitCount = worldEditorAbility.getFieldAsInteger(AbilityFields.FERAL_SPIRIT_SUMMON_UNIT_COUNT_1,
				level);
		this.buffId = AbstractCAbilityTypeDefinition.getBuffId(worldEditorAbility, level);
		this.duration = worldEditorAbility.getFieldAsFloat(AbilityFields.DURATION, level);
		this.areaOfEffect = worldEditorAbility.getFieldAsFloat(AbilityFields.AREA_OF_EFFECT, level);
	}

	@Override
	public int getBaseOrderId() {
		return OrderIds.spiritwolf;
	}

	@Override
	public boolean doEffect(final CSimulation simulation, final CUnit unit, final AbilityTarget target) {
		final float facing = unit.getFacing();
		final float facingRad = (float) StrictMath.toRadians(facing);
		final float x = unit.getX() + ((float) StrictMath.cos(facingRad) * this.areaOfEffect);
		final float y = unit.getY() + ((float) StrictMath.sin(facingRad) * this.areaOfEffect);
		for (final CUnit lastSummon : this.lastSummonUnits) {
			if (!lastSummon.isDead()) {
				lastSummon.kill(simulation);
			}
		}
		this.lastSummonUnits.clear();
		for (int i = 0; i < this.summonUnitCount; i++) {
			final CUnit summonedUnit = simulation.createUnitSimple(this.summonUnitId, unit.getPlayerIndex(), x, y,
					facing);
			summonedUnit.addClassification(CUnitClassification.SUMMONED);
			summonedUnit.add(simulation,
					new CBuffTimedLife(simulation.getHandleIdAllocator().createId(), this.buffId, this.duration));
			summonedUnit.setExplodesOnDeath(true);
			simulation.createSpellEffectOnUnit(summonedUnit, getAlias(), CEffectType.SPECIAL);
			this.lastSummonUnits.add(summonedUnit);
		}
		return false;
	}

	public War3ID getSummonUnitId() {
		return this.summonUnitId;
	}

	public int getSummonUnitCount() {
		return this.summonUnitCount;
	}

	public War3ID getBuffId() {
		return this.buffId;
	}

	public float getDuration() {
		return this.duration;
	}

	public float getAreaOfEffect() {
		return this.areaOfEffect;
	}

	public void setSummonUnitId(final War3ID summonUnitId) {
		this.summonUnitId = summonUnitId;
	}

	public void setSummonUnitCount(final int summonUnitCount) {
		this.summonUnitCount = summonUnitCount;
	}

	public void setBuffId(final War3ID buffId) {
		this.buffId = buffId;
	}

	public void setDuration(final float duration) {
		this.duration = duration;
	}

	public void setAreaOfEffect(final float areaOfEffect) {
		this.areaOfEffect = areaOfEffect;
	}

}
