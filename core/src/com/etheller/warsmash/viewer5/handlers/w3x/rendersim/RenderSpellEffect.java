package com.etheller.warsmash.viewer5.handlers.w3x.rendersim;

import java.util.List;

import com.etheller.warsmash.viewer5.handlers.mdx.MdxComplexInstance;
import com.etheller.warsmash.viewer5.handlers.mdx.MdxModel;
import com.etheller.warsmash.viewer5.handlers.mdx.Sequence;
import com.etheller.warsmash.viewer5.handlers.mdx.SequenceLoopMode;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens.PrimaryTag;
import com.etheller.warsmash.viewer5.handlers.w3x.IndexedSequence;
import com.etheller.warsmash.viewer5.handlers.w3x.SequenceUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;

public class RenderSpellEffect implements RenderEffect {
	public static final PrimaryTag[] DEFAULT_ANIMATION_QUEUE = { PrimaryTag.BIRTH, PrimaryTag.STAND, PrimaryTag.DEATH };
	public static final PrimaryTag[] STAND_ONLY = { PrimaryTag.STAND };
	public static final PrimaryTag[] DEATH_ONLY = { PrimaryTag.DEATH };
	private SequenceLoopMode sequenceLoopMode;
	private final MdxComplexInstance modelInstance;
	private PrimaryTag[] animationQueue;
	private int animationQueueIndex;
	private final List<Sequence> sequences;
	private boolean killWhenDone = true;

	public RenderSpellEffect(final MdxComplexInstance modelInstance, final War3MapViewer war3MapViewer, final float yaw,
			final PrimaryTag[] animationQueue) {
		this.modelInstance = modelInstance;
		this.animationQueue = animationQueue;
		final MdxModel model = (MdxModel) this.modelInstance.model;
		this.sequences = model.getSequences();
		this.sequenceLoopMode = SequenceLoopMode.MODEL_LOOP;
		this.modelInstance.setSequenceLoopMode(sequenceLoopMode);
		this.modelInstance.localRotation.setFromAxisRad(0, 0, 1, yaw);
		this.modelInstance.sequenceEnded = true;
		playNextAnimation();
	}

	@Override
	public boolean updateAnimations(final War3MapViewer war3MapViewer, final float deltaTime) {
		playNextAnimation();
		if (this.killWhenDone) {
			final boolean everythingDone = this.animationQueueIndex >= this.animationQueue.length;
			if (everythingDone) {
				if (this.modelInstance.parent != null) {
					this.modelInstance.setParent(null);
				}
				war3MapViewer.worldScene.removeInstance(this.modelInstance);
			}
			return everythingDone;
		}
		else {
			this.animationQueueIndex = 0;
			playNextAnimation();
			;
			return false;
		}
	}

	private void playNextAnimation() {
		while (this.modelInstance.sequenceEnded && (this.animationQueueIndex < this.animationQueue.length)) {
			applySequence();
			this.animationQueueIndex++;
		}
	}

	public void applySequence() {
		final PrimaryTag tag = this.animationQueue[this.animationQueueIndex];
		final IndexedSequence sequence = SequenceUtils.selectSequence(tag, SequenceUtils.EMPTY, this.sequences, true);
		if ((sequence != null) && (sequence.index != -1)) {
			if ((tag == PrimaryTag.STAND) && (sequenceLoopMode != SequenceLoopMode.NEVER_LOOP)) {
				this.modelInstance.setSequenceLoopMode(SequenceLoopMode.ALWAYS_LOOP);
			}
			else {
				this.modelInstance.setSequenceLoopMode(sequenceLoopMode);
			}
			this.modelInstance.setSequence(sequence.index);
		}
	}

	public void setAnimations(final PrimaryTag[] animations, final boolean killWhenDone) {
		this.animationQueue = animations;
		this.animationQueueIndex = 0;
		setKillWhenDone(killWhenDone);
		applySequence();
	}

	public void setKillWhenDone(final boolean killWhenDone) {
		this.killWhenDone = killWhenDone;
		if (killWhenDone) {
			sequenceLoopMode = SequenceLoopMode.NEVER_LOOP;
			this.modelInstance.setSequenceLoopMode(SequenceLoopMode.NEVER_LOOP);
		}
		else {
			this.modelInstance.setSequenceLoopMode(sequenceLoopMode);
		}
	}

	public void setHeight(final float height) {
		this.modelInstance.setLocation(modelInstance.localLocation.x, modelInstance.localLocation.y, height);
	}
}
