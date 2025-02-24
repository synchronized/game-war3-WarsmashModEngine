package com.etheller.warsmash.parsers.jass;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.etheller.interpreter.JassLexer;
import com.etheller.interpreter.JassParser;
import com.etheller.interpreter.ast.debug.JassException;
import com.etheller.interpreter.ast.function.JassFunction;
import com.etheller.interpreter.ast.scope.GlobalScope;
import com.etheller.interpreter.ast.scope.TriggerExecutionScope;
import com.etheller.interpreter.ast.scope.trigger.RemovableTriggerEvent;
import com.etheller.interpreter.ast.scope.trigger.Trigger;
import com.etheller.interpreter.ast.scope.trigger.TriggerBooleanExpression;
import com.etheller.interpreter.ast.scope.variableevent.CLimitOp;
import com.etheller.interpreter.ast.util.CHandle;
import com.etheller.interpreter.ast.util.JassSettings;
import com.etheller.interpreter.ast.value.BooleanJassValue;
import com.etheller.interpreter.ast.value.HandleJassType;
import com.etheller.interpreter.ast.value.HandleJassValue;
import com.etheller.interpreter.ast.value.IntegerJassValue;
import com.etheller.interpreter.ast.value.JassType;
import com.etheller.interpreter.ast.value.JassValue;
import com.etheller.interpreter.ast.value.RealJassValue;
import com.etheller.interpreter.ast.value.StringJassValue;
import com.etheller.interpreter.ast.value.visitor.BooleanJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.IntegerJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.JassFunctionJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.JassTypeGettingValueVisitor;
import com.etheller.interpreter.ast.value.visitor.ObjectJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.RealJassValueVisitor;
import com.etheller.interpreter.ast.value.visitor.StringJassValueVisitor;
import com.etheller.interpreter.ast.visitors.JassProgramVisitor;
import com.etheller.warsmash.datasources.DataSource;
import com.etheller.warsmash.parsers.fdf.GameSkin;
import com.etheller.warsmash.parsers.fdf.GameUI;
import com.etheller.warsmash.parsers.fdf.datamodel.AnchorDefinition;
import com.etheller.warsmash.parsers.fdf.datamodel.FramePoint;
import com.etheller.warsmash.parsers.fdf.frames.SetPoint;
import com.etheller.warsmash.parsers.fdf.frames.StringFrame;
import com.etheller.warsmash.parsers.fdf.frames.UIFrame;
import com.etheller.warsmash.parsers.jass.scope.CommonTriggerExecutionScope;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprAnd;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprCondition;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprFilter;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprNot;
import com.etheller.warsmash.parsers.jass.triggers.BoolExprOr;
import com.etheller.warsmash.parsers.jass.triggers.TriggerAction;
import com.etheller.warsmash.parsers.jass.triggers.TriggerCondition;
import com.etheller.warsmash.parsers.jass.triggers.UnitGroup;
import com.etheller.warsmash.units.Element;
import com.etheller.warsmash.units.manager.MutableObjectData.MutableGameObject;
import com.etheller.warsmash.util.War3ID;
import com.etheller.warsmash.util.WarsmashConstants;
import com.etheller.warsmash.viewer5.Scene;
import com.etheller.warsmash.viewer5.handlers.mdx.Sequence;
import com.etheller.warsmash.viewer5.handlers.w3x.AnimationTokens;
import com.etheller.warsmash.viewer5.handlers.w3x.UnitSound;
import com.etheller.warsmash.viewer5.handlers.w3x.War3MapViewer;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderSpellEffect;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.RenderUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.rendersim.ability.ItemUI;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CDestructableType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CItem;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CSimulation;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnit;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitEnumFunction;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUnitType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CUpgradeType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.CWidget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.CAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.GetAbilityByRawcodeVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.AbstractGenericAliasedAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.generic.CLevelingAbility;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.hero.CAbilityHero;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.inventory.CAbilityInventory;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.mine.CAbilityBlightedGoldMine;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityPointTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTarget;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.targeting.AbilityTargetVisitor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.CAbilityType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.BehaviorExpr;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.CAbilityTypeJassDefinition;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.CAbilityTypeJassDefinition.JassOrder;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.CAbilityTypeJassDefinition.JassOrderButtonType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.abilities.types.jass.JassFunctionBehaviorExpr;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.ai.AIDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.CBehaviorAttackListener;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.behaviors.jass.CBehaviorJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.combat.CAttackType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.CPlayerAPI;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.config.War3MapConfig;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.item.CItemTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.orders.OrderIdUtils;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CAllianceType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapFlag;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CMapPlacement;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerColor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerGameResult;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerScore;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CPlayerUnitOrderExecutor;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRace;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CRacePreference;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.players.CStartLocPrio;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegionTriggerEnter;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.region.CRegionTriggerLeave;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CMIDISound;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSound;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundFilename;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.sound.CSoundFromLabel;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CGameState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.state.CUnitState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimer;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.timers.CTimerNativeEvent;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.JassGameEventsWar3;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CAttackTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CBlendMode;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CCameraField;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CDamageType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CEffectType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CFogState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CGameSpeed;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CGameType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDensity;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CMapDifficulty;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CPathingTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CPlayerSlotState;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CRarityControl;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundType;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CSoundVolumeGroup;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CTexMapFlags;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CVersion;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.trigger.enumtypes.CWeaponSoundTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.unit.CUnitTypeJass;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityActivationReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.BooleanAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CHashtable;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.CWidgetAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.util.PointAbilityTargetCheckReceiver;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.MeleeUI;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialog;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.dialog.CScriptDialogButton;

public class Jass2 {
	public static final boolean REPORT_SYNTAX_ERRORS = true;

	public static CommonEnvironment loadCommon(final DataSource dataSource, final Viewport uiViewport,
			final Scene uiScene, final War3MapViewer war3MapViewer, final MeleeUI meleeUI, final String... files) {

		final JassProgramVisitor jassProgramVisitor = new JassProgramVisitor();
		final CommonEnvironment environment = new CommonEnvironment(jassProgramVisitor, dataSource, uiViewport, uiScene,
				war3MapViewer, meleeUI);
		for (String jassFilePath : files) {
			if (!dataSource.has(jassFilePath)) {
				jassFilePath = jassFilePath
						.substring(Math.max(jassFilePath.lastIndexOf('/'), jassFilePath.lastIndexOf('\\')) + 1);
			}
			final String jassFile = jassFilePath;
			try {
				JassLexer lexer;
				try {
					lexer = new JassLexer(CharStreams.fromStream(dataSource.getResourceAsStream(jassFile)));
				}
				catch (final IOException e) {
					throw new RuntimeException(e);
				}
				final JassParser parser = new JassParser(new CommonTokenStream(lexer));
//				parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
				parser.addErrorListener(new BaseErrorListener() {
					@Override
					public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol,
							final int line, final int charPositionInLine, final String msg,
							final RecognitionException e) {
						if (!REPORT_SYNTAX_ERRORS) {
							return;
						}

						final String sourceName = String.format("%s:%d:%d: ", jassFile, line, charPositionInLine);

						System.err.println(sourceName + "line " + line + ":" + charPositionInLine + " " + msg);
						throw new IllegalStateException(
								sourceName + "line " + line + ":" + charPositionInLine + " " + msg);
					}
				});
				jassProgramVisitor.setCurrentFileName(jassFile);
				jassProgramVisitor.visit(parser.program());
			}
			catch (final Exception e) {
				e.printStackTrace();
			}
		}
		jassProgramVisitor.getJassNativeManager().checkUnregisteredNatives();
		return environment;
	}

	public static ConfigEnvironment loadConfig(final DataSource dataSource, final Viewport uiViewport,
			final Scene uiScene, final GameUI gameUI, final War3MapConfig mapConfig, final String... files) {

		final JassProgramVisitor jassProgramVisitor = new JassProgramVisitor();
		final ConfigEnvironment environment = new ConfigEnvironment(jassProgramVisitor, dataSource, uiViewport, uiScene,
				gameUI, mapConfig);
		for (String jassFilePath : files) {
			if (!dataSource.has(jassFilePath)) {
				jassFilePath = jassFilePath
						.substring(Math.max(jassFilePath.lastIndexOf('/'), jassFilePath.lastIndexOf('\\')) + 1);
			}
			final String jassFile = jassFilePath;
			try {
				JassLexer lexer;
				try {
					lexer = new JassLexer(CharStreams.fromStream(dataSource.getResourceAsStream(jassFile)));
				}
				catch (final IOException e) {
					throw new RuntimeException(e);
				}
				final JassParser parser = new JassParser(new CommonTokenStream(lexer));
//				parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
				parser.addErrorListener(new BaseErrorListener() {
					@Override
					public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol,
							final int line, final int charPositionInLine, final String msg,
							final RecognitionException e) {
						if (!REPORT_SYNTAX_ERRORS) {
							return;
						}

						final String sourceName = String.format("%s:%d:%d: ", jassFile, line, charPositionInLine);

						System.err.println(sourceName + "line " + line + ":" + charPositionInLine + " " + msg);
						throw new IllegalStateException(
								sourceName + "line " + line + ":" + charPositionInLine + " " + msg);
					}
				});
				jassProgramVisitor.setCurrentFileName(jassFile);
				jassProgramVisitor.visit(parser.program());
			}
			catch (final Exception e) {
				e.printStackTrace();
			}
		}
		jassProgramVisitor.getJassNativeManager().checkUnregisteredNatives();
		return environment;
	}

	public static JUIEnvironment loadJUI(final DataSource dataSource, final Viewport uiViewport, final Scene uiScene,
			final War3MapViewer war3MapViewer, final RootFrameListener rootFrameListener, final String... files) {

		final JassProgramVisitor jassProgramVisitor = new JassProgramVisitor();
		final JUIEnvironment environment = new JUIEnvironment(jassProgramVisitor, dataSource, uiViewport, uiScene,
				war3MapViewer, rootFrameListener);
		for (final String jassFile : files) {
			try {
				JassLexer lexer;
				try {
					lexer = new JassLexer(CharStreams.fromStream(dataSource.getResourceAsStream(jassFile)));
				}
				catch (final IOException e) {
					throw new RuntimeException(e);
				}
				final JassParser parser = new JassParser(new CommonTokenStream(lexer));
//				parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
				parser.addErrorListener(new BaseErrorListener() {
					@Override
					public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol,
							final int line, final int charPositionInLine, final String msg,
							final RecognitionException e) {
						if (!REPORT_SYNTAX_ERRORS) {
							return;
						}

						final String sourceName = String.format("%s:%d:%d: ", jassFile, line, charPositionInLine);

						System.err.println(sourceName + "line " + line + ":" + charPositionInLine + " " + msg);
					}
				});
				jassProgramVisitor.visit(parser.program());
			}
			catch (final Exception e) {
				e.printStackTrace();
			}
		}
		jassProgramVisitor.getJassNativeManager().checkUnregisteredNatives();
		return environment;
	}

	public static interface RootFrameListener {
		void onCreate(GameUI rootFrame);
	}

	private static final class JUIEnvironment {
		private GameUI gameUI;
		private Element skin;

		public JUIEnvironment(final JassProgramVisitor jassProgramVisitor, final DataSource dataSource,
				final Viewport uiViewport, final Scene uiScene, final War3MapViewer war3MapViewer,
				final RootFrameListener rootFrameListener) {
			final GlobalScope globals = jassProgramVisitor.getGlobals();
			final HandleJassType frameHandleType = globals.registerHandleType("framehandle");
			final HandleJassType framePointType = globals.registerHandleType("framepointtype");
			final HandleJassType triggerType = globals.registerHandleType("trigger");
			final HandleJassType triggerActionType = globals.registerHandleType("triggeraction");
			final HandleJassType triggerConditionType = globals.registerHandleType("triggercondition");
			final HandleJassType boolExprType = globals.registerHandleType("boolexpr");
			final HandleJassType conditionFuncType = globals.registerHandleType("conditionfunc");
			final HandleJassType filterType = globals.registerHandleType("filterfunc");
			final HandleJassType eventidType = globals.registerHandleType("eventid");
			jassProgramVisitor.getJassNativeManager().createNative("LogError", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String stringValue = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					System.err.println(stringValue);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ConvertFramePointType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int value = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(framePointType, FramePoint.values()[value]);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateRootFrame", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String skinArg = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final GameSkin skin = GameUI.loadSkin(dataSource, skinArg);
					final GameUI gameUI = new GameUI(dataSource, skin, uiViewport, uiScene, war3MapViewer, 0,
							war3MapViewer.getAllObjectData().getWts());
					JUIEnvironment.this.gameUI = gameUI;
					JUIEnvironment.this.skin = skin.getSkin();
					rootFrameListener.onCreate(gameUI);
					return new HandleJassValue(frameHandleType, gameUI);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("LoadTOCFile", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String tocFileName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					try {
						JUIEnvironment.this.gameUI.loadTOCFile(tocFileName);
					}
					catch (final IOException e) {
						throw new RuntimeException(e);
					}
					return BooleanJassValue.TRUE;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateSimpleFrame", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String templateName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final UIFrame ownerFrame = arguments.get(1).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
					final int createContext = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());

					final UIFrame simpleFrame = JUIEnvironment.this.gameUI.createSimpleFrame(templateName, ownerFrame,
							createContext);

					return new HandleJassValue(frameHandleType, simpleFrame);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateFrame", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String templateName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final UIFrame ownerFrame = arguments.get(1).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
					final int priority = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final int createContext = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());

					final UIFrame simpleFrame = JUIEnvironment.this.gameUI.createFrame(templateName, ownerFrame,
							priority, createContext);

					return new HandleJassValue(frameHandleType, simpleFrame);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetFrameByName", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String templateName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final int createContext = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

					final UIFrame simpleFrame = JUIEnvironment.this.gameUI.getFrameByName(templateName, createContext);
					return new HandleJassValue(frameHandleType, simpleFrame);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetAnchor", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
					final FramePoint framePoint = arguments.get(1)
							.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());

					frame.addAnchor(new AnchorDefinition(framePoint, GameUI.convertX(uiViewport, (float) x),
							GameUI.convertY(uiViewport, (float) y)));
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetAbsPoint", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
					final FramePoint framePoint = arguments.get(1)
							.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());

					frame.setFramePointX(framePoint, GameUI.convertX(uiViewport, (float) x));
					frame.setFramePointY(framePoint, GameUI.convertY(uiViewport, (float) y));
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetPoint", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
					final FramePoint framePoint = arguments.get(1)
							.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
					final UIFrame otherFrame = arguments.get(2).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
					final FramePoint otherPoint = arguments.get(3)
							.visit(ObjectJassValueVisitor.<FramePoint>getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());

					frame.addSetPoint(new SetPoint(framePoint, otherFrame, otherPoint,
							GameUI.convertX(uiViewport, (float) x), GameUI.convertY(uiViewport, (float) y)));
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetText", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final StringFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<StringFrame>getInstance());
					final String text = arguments.get(1).visit(StringJassValueVisitor.getInstance());

					JUIEnvironment.this.gameUI.setText(frame, text);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("FrameSetTextColor", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final StringFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<StringFrame>getInstance());
					final int colorInt = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					frame.setColor(new Color(colorInt));
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ConvertColor", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int a = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final int r = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int g = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final int b = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
					return new IntegerJassValue(a | (b << 8) | (g << 16) | (r << 24));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("FramePositionBounds", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final UIFrame frame = arguments.get(0).visit(ObjectJassValueVisitor.<UIFrame>getInstance());
					frame.positionBounds(JUIEnvironment.this.gameUI, uiViewport);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SkinGetField", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String fieldName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					return new StringJassValue(JUIEnvironment.this.skin.getField(fieldName));
				}
			});
			setupTriggerAPI(jassProgramVisitor, triggerType, triggerActionType, triggerConditionType, boolExprType,
					conditionFuncType, filterType, eventidType);
		}
	}

	public static final class CommonEnvironment {

		private GameUI gameUI;
		private Element skin;
		private final JassProgramVisitor jassProgramVisitor;
		private CSimulation simulation;

		private CommonEnvironment(final JassProgramVisitor jassProgramVisitor, final DataSource dataSource,
				final Viewport uiViewport, final Scene uiScene, final War3MapViewer war3MapViewer,
				final MeleeUI meleeUI) {
			this.jassProgramVisitor = jassProgramVisitor;
			this.gameUI = war3MapViewer.getGameUI();
			final Rectangle tempRect = new Rectangle();
			this.simulation = war3MapViewer.simulation;
			final GlobalScope globals = jassProgramVisitor.getGlobals();
			final HandleJassType agentType = globals.registerHandleType("agent");
			final HandleJassType eventType = globals.registerHandleType("event");
			final HandleJassType playerType = globals.registerHandleType("player");
			final HandleJassType widgetType = globals.registerHandleType("widget");
			final HandleJassType unitType = globals.registerHandleType("unit");
			final HandleJassType destructableType = globals.registerHandleType("destructable");
			final HandleJassType itemType = globals.registerHandleType("item");
			final HandleJassType abilityType = globals.registerHandleType("ability");
			final HandleJassType buffType = globals.registerHandleType("buff");
			final HandleJassType forceType = globals.registerHandleType("force");
			final HandleJassType groupType = globals.registerHandleType("group");
			final HandleJassType triggerType = globals.registerHandleType("trigger");
			final HandleJassType triggerconditionType = globals.registerHandleType("triggercondition");
			final HandleJassType triggeractionType = globals.registerHandleType("triggeraction");
			final HandleJassType timerType = globals.registerHandleType("timer");
			final HandleJassType locationType = globals.registerHandleType("location");
			final HandleJassType regionType = globals.registerHandleType("region");
			final HandleJassType rectType = globals.registerHandleType("rect");
			final HandleJassType boolexprType = globals.registerHandleType("boolexpr");
			final HandleJassType soundType = globals.registerHandleType("sound");
			final HandleJassType conditionfuncType = globals.registerHandleType("conditionfunc");
			final HandleJassType filterfuncType = globals.registerHandleType("filterfunc");
			final HandleJassType unitpoolType = globals.registerHandleType("unitpool");
			final HandleJassType itempoolType = globals.registerHandleType("itempool");
			final HandleJassType raceType = globals.registerHandleType("race");
			final HandleJassType alliancetypeType = globals.registerHandleType("alliancetype");
			final HandleJassType racepreferenceType = globals.registerHandleType("racepreference");
			final HandleJassType gamestateType = globals.registerHandleType("gamestate");
			final HandleJassType igamestateType = globals.registerHandleType("igamestate");
			final HandleJassType fgamestateType = globals.registerHandleType("fgamestate");
			final HandleJassType playerstateType = globals.registerHandleType("playerstate");
			final HandleJassType playerscoreType = globals.registerHandleType("playerscore");
			final HandleJassType playergameresultType = globals.registerHandleType("playergameresult");
			final HandleJassType unitstateType = globals.registerHandleType("unitstate");
			final HandleJassType aidifficultyType = globals.registerHandleType("aidifficulty");
			final HandleJassType eventidType = globals.registerHandleType("eventid");
			final HandleJassType gameeventType = globals.registerHandleType("gameevent");
			final HandleJassType playereventType = globals.registerHandleType("playerevent");
			final HandleJassType playeruniteventType = globals.registerHandleType("playerunitevent");
			final HandleJassType uniteventType = globals.registerHandleType("unitevent");
			final HandleJassType limitopType = globals.registerHandleType("limitop");
			final HandleJassType widgeteventType = globals.registerHandleType("widgetevent");
			final HandleJassType dialogeventType = globals.registerHandleType("dialogevent");
			final HandleJassType unittypeType = globals.registerHandleType("unittype");
			final HandleJassType gamespeedType = globals.registerHandleType("gamespeed");
			final HandleJassType gamedifficultyType = globals.registerHandleType("gamedifficulty");
			final HandleJassType gametypeType = globals.registerHandleType("gametype");
			final HandleJassType mapflagType = globals.registerHandleType("mapflag");
			final HandleJassType mapvisibilityType = globals.registerHandleType("mapvisibility");
			final HandleJassType mapsettingType = globals.registerHandleType("mapsetting");
			final HandleJassType mapdensityType = globals.registerHandleType("mapdensity");
			final HandleJassType mapcontrolType = globals.registerHandleType("mapcontrol");
			final HandleJassType playerslotstateType = globals.registerHandleType("playerslotstate");
			final HandleJassType volumegroupType = globals.registerHandleType("volumegroup");
			final HandleJassType camerafieldType = globals.registerHandleType("camerafield");
			final HandleJassType camerasetupType = globals.registerHandleType("camerasetup");
			final HandleJassType playercolorType = globals.registerHandleType("playercolor");
			final HandleJassType placementType = globals.registerHandleType("placement");
			final HandleJassType startlocprioType = globals.registerHandleType("startlocprio");
			final HandleJassType raritycontrolType = globals.registerHandleType("raritycontrol");
			final HandleJassType blendmodeType = globals.registerHandleType("blendmode");
			final HandleJassType texmapflagsType = globals.registerHandleType("texmapflags");
			final HandleJassType effectType = globals.registerHandleType("effect");
			final HandleJassType effecttypeType = globals.registerHandleType("effecttype");
			final HandleJassType weathereffectType = globals.registerHandleType("weathereffect");
			final HandleJassType terraindeformationType = globals.registerHandleType("terraindeformation");
			final HandleJassType fogstateType = globals.registerHandleType("fogstate");
			final HandleJassType fogmodifierType = globals.registerHandleType("fogmodifier");
			final HandleJassType dialogType = globals.registerHandleType("dialog");
			final HandleJassType buttonType = globals.registerHandleType("button");
			final HandleJassType questType = globals.registerHandleType("quest");
			final HandleJassType questitemType = globals.registerHandleType("questitem");
			final HandleJassType defeatconditionType = globals.registerHandleType("defeatcondition");
			final HandleJassType timerdialogType = globals.registerHandleType("timerdialog");
			final HandleJassType leaderboardType = globals.registerHandleType("leaderboard");
			final HandleJassType multiboardType = globals.registerHandleType("multiboard");
			final HandleJassType multiboarditemType = globals.registerHandleType("multiboarditem");
			final HandleJassType trackableType = globals.registerHandleType("trackable");
			final HandleJassType gamecacheType = globals.registerHandleType("gamecache");
			final HandleJassType versionType = globals.registerHandleType("version");
			final HandleJassType itemtypeType = globals.registerHandleType("itemtype");
			final HandleJassType texttagType = globals.registerHandleType("texttag");
			final HandleJassType attacktypeType = globals.registerHandleType("attacktype");
			final HandleJassType damagetypeType = globals.registerHandleType("damagetype");
			final HandleJassType weapontypeType = globals.registerHandleType("weapontype");
			final HandleJassType soundtypeType = globals.registerHandleType("soundtype");
			final HandleJassType lightningType = globals.registerHandleType("lightning");
			final HandleJassType pathingtypeType = globals.registerHandleType("pathingtype");
			final HandleJassType imageType = globals.registerHandleType("image");
			final HandleJassType ubersplatType = globals.registerHandleType("ubersplat");
			final HandleJassType hashtableType = globals.registerHandleType("hashtable");
			final HandleJassType frameHandleType = globals.registerHandleType("framehandle");

			// Warsmash Ability API
			final HandleJassType abilitytypeType = globals.registerHandleType("abilitytype");
			final HandleJassType orderbuttonType = globals.registerHandleType("orderbutton");
			final HandleJassType orderbuttontypeType = globals.registerHandleType("orderbuttontype");
			final HandleJassType abilitybehaviorType = globals.registerHandleType("abilitybehavior");
			final HandleJassType behaviorexprType = globals.registerHandleType("behaviorexpr");
			final HandleJassType iconuiType = globals.registerHandleType("iconui");

			registerTypingNatives(jassProgramVisitor, raceType, alliancetypeType, racepreferenceType, igamestateType,
					fgamestateType, playerstateType, playerscoreType, playergameresultType, unitstateType,
					aidifficultyType, gameeventType, playereventType, playeruniteventType, uniteventType, limitopType,
					widgeteventType, dialogeventType, unittypeType, gamespeedType, gamedifficultyType, gametypeType,
					mapflagType, mapvisibilityType, mapsettingType, mapdensityType, mapcontrolType, playerslotstateType,
					volumegroupType, camerafieldType, playercolorType, placementType, startlocprioType,
					raritycontrolType, blendmodeType, texmapflagsType, effecttypeType, fogstateType, versionType,
					itemtypeType, attacktypeType, damagetypeType, weapontypeType, soundtypeType, pathingtypeType);
			jassProgramVisitor.getJassNativeManager().createNative("ConvertOrderButtonType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(orderbuttontypeType, JassOrderButtonType.VALUES[i]);
				}
			});

			jassProgramVisitor.getJassNativeManager().createNative("UnitId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String idString = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final CUnitType unitType = CommonEnvironment.this.simulation.getUnitData()
							.getUnitTypeByJassLegacyName(idString);
					if (unitType == null) {
						return new IntegerJassValue(0);
					}
					return new IntegerJassValue(unitType.getTypeId().getValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitId2String", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final War3ID war3id = new War3ID(id);
					final CUnitType unitType = CommonEnvironment.this.simulation.getUnitData().getUnitType(war3id);
					return unitType == null ? JassType.STRING.getNullValue()
							: new StringJassValue(unitType.getLegacyName());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AbilityId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new IntegerJassValue(0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AbilityId2String", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new StringJassValue("");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("BlzGetUnitAbilityByIndex", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int whichAbilityIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

					final List<CAbility> abilities = whichUnit.getAbilities();
					if (whichAbilityIndex < abilities.size()) {
						return new HandleJassValue(abilityType, abilities.get(whichAbilityIndex));
					}
					else {
						return new HandleJassValue(abilityType, null);
					}
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("WarsmashGetAbilityTypeId", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (ability instanceof AbstractGenericAliasedAbility) {
						final AbstractGenericAliasedAbility aliasedAbility = (AbstractGenericAliasedAbility) ability;
						return new IntegerJassValue(aliasedAbility.getAlias().getValue());
					}
					return IntegerJassValue.ZERO;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("WarsmashGetAbilityClassName", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbility ability = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new StringJassValue(ability.getClass().getSimpleName());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("WarsmashGetRawcode2String", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new StringJassValue(new War3ID(rawcode).toString());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetObjectName", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final War3ID war3id = new War3ID(id);
					final CUnitType unitType = CommonEnvironment.this.simulation.getUnitData().getUnitType(war3id);
					if (unitType != null) {
						return new StringJassValue(unitType.getName());
					}
					// TODO for now this looks in the ability editor data, not the fast symbol table
					// layer on top, because the layer on top forgot to have a name value...
					final MutableGameObject abilityEditorData = war3MapViewer.getAllObjectData().getAbilities()
							.get(war3id);
					if (abilityEditorData != null) {
						return new StringJassValue(abilityEditorData.getName());
					}
					final ItemUI itemUI = war3MapViewer.getAbilityDataUI().getItemUI(war3id);
					if (itemUI != null) {
						return new StringJassValue(itemUI.getName());
					}
					final CDestructableType destructableType = CommonEnvironment.this.simulation.getDestructableData()
							.getUnitType(war3id);
					if (destructableType != null) {
						return new StringJassValue(destructableType.getName());
					}
					return new StringJassValue("");
				}
			});
			registerConversionAndStringNatives(jassProgramVisitor, war3MapViewer.getGameUI());
			final War3MapConfig mapConfig = war3MapViewer.getMapConfig();
			registerConfigNatives(jassProgramVisitor, mapConfig, startlocprioType, gametypeType, placementType,
					gamespeedType, gamedifficultyType, mapdensityType, locationType, playerType, playercolorType,
					mapcontrolType, playerslotstateType, this.simulation);

			// ============================================================================
			// Timer API
			//
			jassProgramVisitor.getJassNativeManager().createNative("CreateTimer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(timerType, new CTimerJass(globalScope,
							CommonEnvironment.this.simulation.getHandleIdAllocator().createId()));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyTimer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = arguments.get(0).visit(ObjectJassValueVisitor.<CTimerJass>getInstance());
					CommonEnvironment.this.simulation.unregisterTimer(timer);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TimerStart", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = nullable(arguments, 0, ObjectJassValueVisitor.<CTimerJass>getInstance());
					final Double timeout = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final boolean periodic = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final JassFunction handlerFunc = nullable(arguments, 3, JassFunctionJassValueVisitor.getInstance());
					if ((timer != null) && !timer.isRunning()) {
						timer.setTimeoutTime(timeout.floatValue());
						timer.setRepeats(periodic);
						timer.setHandlerFunc(handlerFunc);
						timer.start(CommonEnvironment.this.simulation);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TimerGetElapsed", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = arguments.get(0).visit(ObjectJassValueVisitor.<CTimerJass>getInstance());
					return new RealJassValue(timer.getElapsed(CommonEnvironment.this.simulation));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TimerGetRemaining", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = arguments.get(0).visit(ObjectJassValueVisitor.<CTimerJass>getInstance());
					return new RealJassValue(timer.getRemaining(CommonEnvironment.this.simulation));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TimerGetTimeout", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = arguments.get(0).visit(ObjectJassValueVisitor.<CTimerJass>getInstance());
					return new RealJassValue(timer.getTimeoutTime());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("PauseTimer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = arguments.get(0).visit(ObjectJassValueVisitor.<CTimerJass>getInstance());
					timer.pause(CommonEnvironment.this.simulation);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ResumeTimer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = arguments.get(0).visit(ObjectJassValueVisitor.<CTimerJass>getInstance());
					timer.resume(CommonEnvironment.this.simulation);
					return null;
				}
			});

			// ============================================================================
			// Group API
			//
			jassProgramVisitor.getJassNativeManager().createNative("CreateGroup", new JassFunction() {
				int stupidGroupHandleId = 0x100000;

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(groupType, new UnitGroup(this.stupidGroupHandleId++));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyGroup", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					System.err.println(
							"DestroyGroup called but in Java we don't have a destructor, so we need to unregister later when that is implemented");
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupAddUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.<CUnit>getInstance());
					group.add(whichUnit);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupRemoveUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.<CUnit>getInstance());
					group.remove(whichUnit);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupClear", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					group.clear();
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsOfType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final String unitname = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final TriggerBooleanExpression filter = nullable(arguments, 2,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					for (final CUnit unit : CommonEnvironment.this.simulation.getUnits()) {
						if (unitname.equals(unit.getUnitType().getLegacyName())) {
							if ((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
								// TODO the trigger scope for evaluation here might need to be a clean one?
								group.add(unit);
							}
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsOfPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final CPlayerJass player = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final TriggerBooleanExpression filter = nullable(arguments, 2,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					for (final CUnit unit : CommonEnvironment.this.simulation.getUnits()) {
						if (unit.getPlayerIndex() == player.getId()) {
							if ((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
								// TODO the trigger scope for evaluation here might need to be a clean one?
								group.add(unit);
							}
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IssuePointOrderLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return BooleanJassValue.FALSE;
					}
					final String orderString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final Point2D.Double whichLocation = arguments.get(2)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
							.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
					final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE;
					final int orderId = OrderIdUtils.getOrderId(orderString);
					int abilityHandleId = 0;
					AbilityPointTarget targetAsPoint = new AbilityPointTarget((float) whichLocation.x,
							(float) whichLocation.y);
					for (final CAbility ability : whichUnit.getAbilities()) {
						ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, orderId, activationReceiver);
						if (activationReceiver.isOk()) {
							final PointAbilityTargetCheckReceiver targetReceiver = PointAbilityTargetCheckReceiver.INSTANCE;
							ability.checkCanTarget(CommonEnvironment.this.simulation, whichUnit, orderId, targetAsPoint,
									targetReceiver.reset());
							if (targetReceiver.getTarget() != null) {
								targetAsPoint = targetReceiver.getTarget();
								abilityHandleId = ability.getHandleId();
							}
						}
					}
					if (abilityHandleId != 0) {
						defaultPlayerUnitOrderExecutor.issuePointOrder(whichUnit.getHandleId(), abilityHandleId,
								orderId, targetAsPoint.x, targetAsPoint.y, false);
					}
					return BooleanJassValue.of(abilityHandleId != 0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IssuePointOrderById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return BooleanJassValue.FALSE;
					}
					final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final double whichLocationX = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double whichLocationY = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
							.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
					final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE;
					int abilityHandleId = 0;
					AbilityPointTarget targetAsPoint = new AbilityPointTarget((float) whichLocationX,
							(float) whichLocationY);
					for (final CAbility ability : whichUnit.getAbilities()) {
						ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, orderId, activationReceiver);
						if (activationReceiver.isOk()) {
							final PointAbilityTargetCheckReceiver targetReceiver = PointAbilityTargetCheckReceiver.INSTANCE;
							ability.checkCanTarget(CommonEnvironment.this.simulation, whichUnit, orderId, targetAsPoint,
									targetReceiver.reset());
							if (targetReceiver.getTarget() != null) {
								targetAsPoint = targetReceiver.getTarget();
								abilityHandleId = ability.getHandleId();
							}
						}
					}
					if (abilityHandleId != 0) {
						defaultPlayerUnitOrderExecutor.issuePointOrder(whichUnit.getHandleId(), abilityHandleId,
								orderId, targetAsPoint.x, targetAsPoint.y, false);
					}
					return BooleanJassValue.of(abilityHandleId != 0);
				}
			});
			// TODO if BuildOrderById is actually different from PointOrderById then this
			// needs to be fixed:
			jassProgramVisitor.getJassNativeManager().createNative("IssueBuildOrderById",
					jassProgramVisitor.getGlobals().getFunctionByName("IssuePointOrderById"));
			jassProgramVisitor.getJassNativeManager().createNative("IssueTargetOrder", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return BooleanJassValue.FALSE;
					}
					final String orderString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					CWidget whichTarget = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
					final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
							.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
					final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE;
					final int orderId = OrderIdUtils.getOrderId(orderString);
					int abilityHandleId = 0;
					for (final CAbility ability : whichUnit.getAbilities()) {
						ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, orderId, activationReceiver);
						if (activationReceiver.isOk()) {
							final CWidgetAbilityTargetCheckReceiver targetReceiver = CWidgetAbilityTargetCheckReceiver.INSTANCE;
							ability.checkCanTarget(CommonEnvironment.this.simulation, whichUnit, orderId, whichTarget,
									targetReceiver.reset());
							if (targetReceiver.getTarget() != null) {
								whichTarget = targetReceiver.getTarget();
								abilityHandleId = ability.getHandleId();
							}
						}
					}
					if (abilityHandleId != 0) {
						defaultPlayerUnitOrderExecutor.issueTargetOrder(whichUnit.getHandleId(), abilityHandleId,
								orderId, whichTarget.getHandleId(), false);
					}
					return BooleanJassValue.of(abilityHandleId != 0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IssueTargetOrderById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return BooleanJassValue.FALSE;
					}
					final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					CWidget whichTarget = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
					final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
							.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
					final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE;
					int abilityHandleId = 0;
					for (final CAbility ability : whichUnit.getAbilities()) {
						ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, orderId, activationReceiver);
						if (activationReceiver.isOk()) {
							final CWidgetAbilityTargetCheckReceiver targetReceiver = CWidgetAbilityTargetCheckReceiver.INSTANCE;
							ability.checkCanTarget(CommonEnvironment.this.simulation, whichUnit, orderId, whichTarget,
									targetReceiver.reset());
							if (targetReceiver.getTarget() != null) {
								whichTarget = targetReceiver.getTarget();
								abilityHandleId = ability.getHandleId();
							}
						}
					}
					if (abilityHandleId != 0) {
						defaultPlayerUnitOrderExecutor.issueTargetOrder(whichUnit.getHandleId(), abilityHandleId,
								orderId, whichTarget.getHandleId(), false);
					}
					return BooleanJassValue.of(abilityHandleId != 0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IssueImmediateOrder", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String orderString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
							.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
					final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE;
					final int orderId = OrderIdUtils.getOrderId(orderString);
					int abilityHandleId = 0;
					for (final CAbility ability : whichUnit.getAbilities()) {
						ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, orderId, activationReceiver);
						if (activationReceiver.isOk()) {
							final BooleanAbilityTargetCheckReceiver<Void> targetReceiver = BooleanAbilityTargetCheckReceiver
									.<Void>getInstance();
							ability.checkCanTargetNoTarget(CommonEnvironment.this.simulation, whichUnit, orderId,
									targetReceiver.reset());
							if (targetReceiver.isTargetable()) {
								abilityHandleId = ability.getHandleId();
							}
						}
					}
					defaultPlayerUnitOrderExecutor.issueImmediateOrder(whichUnit.getHandleId(), abilityHandleId,
							orderId, false);
					return BooleanJassValue.of(abilityHandleId != 0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IssueImmediateOrderById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final CPlayerUnitOrderExecutor defaultPlayerUnitOrderExecutor = CommonEnvironment.this.simulation
							.getDefaultPlayerUnitOrderExecutor(whichUnit.getPlayerIndex());
					final BooleanAbilityActivationReceiver activationReceiver = BooleanAbilityActivationReceiver.INSTANCE;
					int abilityHandleId = 0;
					for (final CAbility ability : whichUnit.getAbilities()) {
						ability.checkCanUse(CommonEnvironment.this.simulation, whichUnit, orderId, activationReceiver);
						if (activationReceiver.isOk()) {
							final BooleanAbilityTargetCheckReceiver<Void> targetReceiver = BooleanAbilityTargetCheckReceiver
									.<Void>getInstance();
							ability.checkCanTargetNoTarget(CommonEnvironment.this.simulation, whichUnit, orderId,
									targetReceiver.reset());
							if (targetReceiver.isTargetable()) {
								abilityHandleId = ability.getHandleId();
							}
						}
					}
					defaultPlayerUnitOrderExecutor.issueImmediateOrder(whichUnit.getHandleId(), abilityHandleId,
							orderId, false);
					return BooleanJassValue.of(abilityHandleId != 0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitDamageTarget", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CWidget target = nullable(arguments, 1, ObjectJassValueVisitor.getInstance());
					final double amount = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final boolean attack = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					final boolean ranged = arguments.get(4).visit(BooleanJassValueVisitor.getInstance());
					CAttackType attackType = nullable(arguments, 5, ObjectJassValueVisitor.getInstance());
					final CDamageType damageType = nullable(arguments, 6, ObjectJassValueVisitor.getInstance());
					CWeaponSoundTypeJass weaponType = nullable(arguments, 7, ObjectJassValueVisitor.getInstance());

					if (whichUnit != null) {
						if (target != null) {
							if (attackType == null) {
								attackType = CAttackType.UNKNOWN;
							}

							if (weaponType == null) {
								weaponType = CWeaponSoundTypeJass.WHOKNOWS;
							}

							target.damage(CommonEnvironment.this.simulation, whichUnit, attackType, weaponType.name(),
									(float) amount);
						}
					}

					return BooleanJassValue.TRUE;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsOfTypeCounted", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final String unitname = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final TriggerBooleanExpression filter = arguments.get(2)
							.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					final Integer countLimit = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
					int count = 0;
					for (final CUnit unit : CommonEnvironment.this.simulation.getUnits()) {
						if (unitname.equals(unit.getUnitType().getLegacyName())) {
							if (filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
								// TODO the trigger scope for evaluation here might need to be a clean one?
								group.add(unit);
								count++;
								if (count >= countLimit) {
									break;
								}
							}
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					final TriggerBooleanExpression filter = nullable(arguments, 2,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					CommonEnvironment.this.simulation.getWorldCollision().enumUnitsInRect(rect,
							new CUnitEnumFunction() {
								@Override
								public boolean call(final CUnit unit) {
									if ((filter == null) || filter.evaluate(globalScope,
											CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
										// TODO the trigger scope for evaluation here might need to be a clean one?
										group.add(unit);
									}
									return false;
								}
							});
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRectCounted", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					final TriggerBooleanExpression filter = arguments.get(2)
							.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					final Integer countLimit = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.getWorldCollision().enumUnitsInRect(rect,
							new CUnitEnumFunction() {
								int count = 0;

								@Override
								public boolean call(final CUnit unit) {
									if (filter.evaluate(globalScope,
											CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
										// TODO the trigger scope for evaluation here might need to be a clean one?
										group.add(unit);
										this.count++;
										if (this.count >= countLimit) {
											return true;
										}
									}
									return false;
								}
							});
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRange", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float radius = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final TriggerBooleanExpression filter = nullable(arguments, 4,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					CommonEnvironment.this.simulation.getWorldCollision().enumUnitsInRect(
							tempRect.set(x - radius, y - radius, radius * 2, radius * 2), new CUnitEnumFunction() {

								@Override
								public boolean call(final CUnit unit) {
									if (unit.distance(x, y) <= radius) {
										if ((filter == null) || filter.evaluate(globalScope,
												CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
											// TODO the trigger scope for evaluation here might need to be a clean one?
											group.add(unit);
										}
									}
									return false;
								}
							});
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeOfLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final Point2D.Double whichLocation = arguments.get(1)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final float x = (float) whichLocation.x;
					final float y = (float) whichLocation.y;
					final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final TriggerBooleanExpression filter = nullable(arguments, 3,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					CommonEnvironment.this.simulation.getWorldCollision().enumUnitsInRect(
							tempRect.set(x - radius, y - radius, radius * 2, radius * 2), new CUnitEnumFunction() {

								@Override
								public boolean call(final CUnit unit) {
									if (unit.distance(x, y) <= radius) {
										if ((filter == null) || filter.evaluate(globalScope,
												CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
											// TODO the trigger scope for evaluation here might need to be a clean one?
											group.add(unit);
										}
									}
									return false;
								}
							});
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeCounted", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float radius = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final TriggerBooleanExpression filter = arguments.get(4)
							.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					final Integer countLimit = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.getWorldCollision().enumUnitsInRect(
							tempRect.set(x - radius, y - radius, radius, radius), new CUnitEnumFunction() {
								int count = 0;

								@Override
								public boolean call(final CUnit unit) {
									if (unit.distance(x, y) <= radius) {
										if (filter.evaluate(globalScope,
												CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
											// TODO the trigger scope for evaluation here might need to be a clean one?
											group.add(unit);
											this.count++;
											if (this.count >= countLimit) {
												return true;
											}
										}
									}
									return false;
								}
							});
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsInRangeOfLocCounted",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final List<CUnit> group = arguments.get(0)
									.visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
							final Point2D.Double whichLocation = arguments.get(1)
									.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
							final float x = (float) whichLocation.x;
							final float y = (float) whichLocation.y;
							final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance())
									.floatValue();
							final TriggerBooleanExpression filter = arguments.get(3)
									.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
							final Integer countLimit = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
							CommonEnvironment.this.simulation.getWorldCollision().enumUnitsInRect(
									tempRect.set(x - radius, y - radius, radius, radius), new CUnitEnumFunction() {
										int count = 0;

										@Override
										public boolean call(final CUnit unit) {
											if (unit.distance(x, y) <= radius) {
												if (filter.evaluate(globalScope,
														CommonTriggerExecutionScope.filterScope(triggerScope, unit))) {
													// TODO the trigger scope for evaluation here might need to be a
													// clean one?
													group.add(unit);
													this.count++;
													if (this.count >= countLimit) {
														return true;
													}
												}
											}
											return false;
										}
									});
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GroupEnumUnitsSelected", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final CPlayerJass whyichPlayer = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final TriggerBooleanExpression filter = arguments.get(2)
							.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					throw new UnsupportedOperationException("GroupEnumUnitsSelected not supported yet.");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupImmediateOrder", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final int orderId = OrderIdUtils.getOrderId(order);
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, orderId, null);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupImmediateOrderById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final int order = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, order, null);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrder", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final Double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final Double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final AbilityPointTarget target = new AbilityPointTarget(x.floatValue(), y.floatValue());
					final int orderId = OrderIdUtils.getOrderId(order);
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, orderId, target);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrderLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final Point2D.Double whichLocation = arguments.get(2)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final AbilityPointTarget target = new AbilityPointTarget((float) whichLocation.x,
							(float) whichLocation.y);
					final int orderId = OrderIdUtils.getOrderId(order);
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, orderId, target);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrderById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final Double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final Double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final AbilityPointTarget target = new AbilityPointTarget(x.floatValue(), y.floatValue());
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, orderId, target);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupPointOrderByIdLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final Point2D.Double whichLocation = arguments.get(2)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final AbilityPointTarget target = new AbilityPointTarget((float) whichLocation.x,
							(float) whichLocation.y);
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, orderId, target);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupTargetOrder", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final String order = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final CWidget target = arguments.get(2).visit(ObjectJassValueVisitor.<CWidget>getInstance());
					final int orderId = OrderIdUtils.getOrderId(order);
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, orderId, target);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GroupTargetOrderById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final CWidget target = arguments.get(2).visit(ObjectJassValueVisitor.<CWidget>getInstance());
					boolean success = true;
					for (final CUnit unit : group) {
						success &= unit.order(CommonEnvironment.this.simulation, orderId, target);
					}
					return BooleanJassValue.of(success);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForGroup", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final JassFunction callback = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
					try {
						for (final CUnit unit : group) {
							callback.call(Collections.emptyList(), globalScope,
									CommonTriggerExecutionScope.enumScope(triggerScope, unit));
						}
					}
					catch (final Exception e) {
						throw new JassException(globalScope, "Exception during ForGroup", e);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("FirstOfGroup", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CUnit> group = arguments.get(0).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					if (group.isEmpty()) {
						return unitType.getNullValue();
					}
					return new HandleJassValue(unitType, group.get(0));
				}
			});
			// ============================================================================
			// Force API
			//
			jassProgramVisitor.getJassNativeManager().createNative("CreateForce", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(forceType, new ArrayList<CPlayerJass>());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyForce", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					System.err.println(
							"DestroyForce called but in Java we don't have a destructor, so we need to unregister later when that is implemented");
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForceAddPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					final CPlayerJass player = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					if (force == null) {
						throw new JassException(globalScope, "force is null", new NullPointerException());
					}
					force.add(player);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForceRemovePlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					final CPlayerJass player = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					force.remove(player);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForceClear", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					force.clear();
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumPlayers", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					final TriggerBooleanExpression filter = nullable(arguments, 1,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
						final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
						if ((filter == null) || filter.evaluate(globalScope,
								CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer))) {
							force.add(jassPlayer);
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumPlayersCounted", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					final TriggerBooleanExpression filter = arguments.get(1)
							.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					final Integer countLimit = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					int count = 0;
					for (int i = 0; (i < WarsmashConstants.MAX_PLAYERS) && (count < countLimit); i++) {
						final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
						if (filter.evaluate(globalScope,
								CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer))) {
							force.add(jassPlayer);
							count++;
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumAllies", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					final CPlayerJass player = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final TriggerBooleanExpression filter = nullable(arguments, 2,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
						final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
						if (player.hasAlliance(i, CAllianceType.PASSIVE)) {
							if ((filter == null) || filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer))) {
								force.add(jassPlayer);
							}
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForceEnumEnemies", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					final CPlayerJass player = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
					final TriggerBooleanExpression filter = nullable(arguments, 2,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					for (int i = 0; i < WarsmashConstants.MAX_PLAYERS; i++) {
						final CPlayerJass jassPlayer = CommonEnvironment.this.simulation.getPlayer(i);
						if (!player.hasAlliance(i, CAllianceType.PASSIVE)) {
							if ((filter != null) && filter.evaluate(globalScope,
									CommonTriggerExecutionScope.filterScope(triggerScope, jassPlayer))) {
								force.add(jassPlayer);
							}
						}
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ForForce", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final List<CPlayerJass> force = arguments.get(0)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					final JassFunction callback = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
					try {
						for (final CPlayerJass player : force) {
							callback.call(Collections.<JassValue>emptyList(), globalScope,
									CommonTriggerExecutionScope.enumScope(triggerScope, player));
						}
					}
					catch (final Exception e) {
						throw new JassException(globalScope, "Exception during ForForce", e);
					}
					return null;
				}
			});
			// ============================================================================
			// Region and Location API
			//
			jassProgramVisitor.getJassNativeManager().createNative("Rect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final float minx = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float miny = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float maxx = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float maxy = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					return new HandleJassValue(rectType, new Rectangle(minx, miny, maxx - minx, maxy - miny));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RectFromLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Point2D.Double min = arguments.get(0)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final Point2D.Double max = arguments.get(1)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final float minx = (float) min.x;
					final float miny = (float) min.y;
					final float maxx = (float) max.x;
					final float maxy = (float) max.y;
					return new HandleJassValue(rectType, new Rectangle(minx, miny, maxx - minx, maxy - miny));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveRect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					System.err.println(
							"RemoveRect called but in Java we don't have a destructor, so we need to unregister later when that is implemented");
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetRect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					final float minx = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float miny = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float maxx = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float maxy = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
					rect.set(minx, miny, maxx - minx, maxy - miny);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetRectFromLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					final Point2D.Double min = arguments.get(1)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final Point2D.Double max = arguments.get(2)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final float minx = (float) min.x;
					final float miny = (float) min.y;
					final float maxx = (float) max.x;
					final float maxy = (float) max.y;
					rect.set(minx, miny, maxx - minx, maxy - miny);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("MoveRectTo", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					final float newCenterX = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float newCenterY = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					rect.setCenter(newCenterX, newCenterY);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("MoveRectToLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					final Point2D.Double newCenterLoc = arguments.get(1)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					rect.setCenter((float) newCenterLoc.x, (float) newCenterLoc.y);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectCenterX", new JassFunction() {
				Vector2 centerHeap = new Vector2();

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					return new RealJassValue(rect.getCenter(this.centerHeap).x);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectCenterY", new JassFunction() {
				Vector2 centerHeap = new Vector2();

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					return new RealJassValue(rect.getCenter(this.centerHeap).y);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMinX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					return new RealJassValue(rect.getX());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMinY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					return new RealJassValue(rect.getY());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMaxX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					return new RealJassValue(rect.getX() + rect.getWidth());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRectMaxY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle rect = arguments.get(0).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					return new RealJassValue(rect.getY() + rect.getHeight());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(regionType, new CRegion());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					region.remove(CommonEnvironment.this.simulation.getRegionManager());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RegionAddRect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					region.addRect(rect, CommonEnvironment.this.simulation.getRegionManager());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RegionClearRect", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final Rectangle rect = arguments.get(1).visit(ObjectJassValueVisitor.<Rectangle>getInstance());
					region.clearRect(rect, CommonEnvironment.this.simulation.getRegionManager());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RegionAddCell", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					region.addCell(x, y, CommonEnvironment.this.simulation.getRegionManager());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RegionAddCellAtLoc", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final Point2D.Double whichLocation = arguments.get(1)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					region.addCell((float) whichLocation.x, (float) whichLocation.y,
							CommonEnvironment.this.simulation.getRegionManager());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RegionClearCell", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					region.clearCell(x, y, CommonEnvironment.this.simulation.getRegionManager());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RegionClearCellAtLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion region = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final Point2D.Double whichLocation = arguments.get(1)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					region.clearCell((float) whichLocation.x, (float) whichLocation.y,
							CommonEnvironment.this.simulation.getRegionManager());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("Location", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					return new HandleJassValue(locationType, new Point2D.Double(x, y));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveLocation", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Point2D.Double whichLocation = arguments.get(0)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					System.err.println(
							"RemoveRect called but in Java we don't have a destructor, so we need to unregister later when that is implemented");
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("MoveLocation", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Point2D.Double whichLocation = arguments.get(0)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					whichLocation.x = x;
					whichLocation.y = y;
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocationX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Point2D.Double whichLocation = arguments.get(0)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					return new RealJassValue(whichLocation.x);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocationY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Point2D.Double whichLocation = arguments.get(0)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					return new RealJassValue(whichLocation.y);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocationZ", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Point2D.Double whichLocation = arguments.get(0)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					return new RealJassValue(
							war3MapViewer.terrain.getGroundHeight((float) whichLocation.x, (float) whichLocation.y));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion whichRegion = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final CUnit whichUnit = arguments.get(1).visit(ObjectJassValueVisitor.<CUnit>getInstance());
					return BooleanJassValue.of(whichUnit.isInRegion(whichRegion));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsPointInRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion whichRegion = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					return BooleanJassValue
							.of(whichRegion.contains(x, y, CommonEnvironment.this.simulation.getRegionManager()));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsLocationInRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CRegion whichRegion = arguments.get(0).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final Point2D.Double whichLocation = arguments.get(1)
							.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
					return BooleanJassValue.of(whichRegion.contains((float) whichLocation.x, (float) whichLocation.y,
							CommonEnvironment.this.simulation.getRegionManager()));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetWorldBounds", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final float worldMinX = CommonEnvironment.this.simulation.getPathingGrid().getWorldX(0) - 16f;
					final float worldMinY = CommonEnvironment.this.simulation.getPathingGrid().getWorldY(0) - 16f;
					final float worldMaxX = CommonEnvironment.this.simulation.getPathingGrid()
							.getWorldX(CommonEnvironment.this.simulation.getPathingGrid().getWidth() - 1) + 16f;
					final float worldMaxY = CommonEnvironment.this.simulation.getPathingGrid()
							.getWorldY(CommonEnvironment.this.simulation.getPathingGrid().getHeight() - 1) + 16f;
					return new HandleJassValue(rectType,
							new Rectangle(worldMinX, worldMinY, worldMaxX - worldMinX, worldMaxY - worldMinY));
				}
			});
			// ============================================================================
			// Native trigger interface
			//
			setupTriggerAPI(jassProgramVisitor, triggerType, triggeractionType, triggerconditionType, boolexprType,
					conditionfuncType, filterfuncType, eventidType);
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getFilterUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getEnumUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterDestructable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(destructableType,
							((CommonTriggerExecutionScope) triggerScope).getFilterDestructable());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumDestructable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(destructableType,
							((CommonTriggerExecutionScope) triggerScope).getEnumDestructable());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(itemType, ((CommonTriggerExecutionScope) triggerScope).getFilterItem());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(itemType, ((CommonTriggerExecutionScope) triggerScope).getEnumItem());
				}
			});

			// ============================================================================
			// Trigger Game Event API
			// ============================================================================
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterVariableEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final String varName = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final CLimitOp limitOp = arguments.get(2).visit(ObjectJassValueVisitor.<CLimitOp>getInstance());
					final Double limitval = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final RemovableTriggerEvent event = globalScope.registerVariableEvent(trigger, varName, limitOp,
							limitval.doubleValue());
					return new HandleJassValue(eventType, event);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTimerEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final Double timeout = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final Boolean periodic = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CTimerNativeEvent timer = new CTimerNativeEvent(globalScope, trigger);
					timer.setRepeats(periodic.booleanValue());
					timer.setTimeoutTime(timeout.floatValue());
					timer.start(CommonEnvironment.this.simulation);
					return new HandleJassValue(eventType, new RemovableTriggerEvent() {
						@Override
						public void remove() {
							CommonEnvironment.this.simulation.unregisterTimer(timer);
						}
					});
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTimerExpireEvent",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final Trigger trigger = arguments.get(0)
									.visit(ObjectJassValueVisitor.<Trigger>getInstance());
							final CTimerJass timer = arguments.get(1)
									.visit(ObjectJassValueVisitor.<CTimerJass>getInstance());
							timer.addEvent(trigger);
							return new HandleJassValue(eventType, new RemovableTriggerEvent() {
								@Override
								public void remove() {
									timer.removeEvent(trigger);
								}
							});
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterGameStateEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final CGameState whichState = arguments.get(1)
							.visit(ObjectJassValueVisitor.<CGameState>getInstance());
					final CLimitOp opcode = arguments.get(2).visit(ObjectJassValueVisitor.<CLimitOp>getInstance());
					final Double limitval = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					if (whichState != CGameState.TIME_OF_DAY) {
						// TODO not yet impl
						throw new UnsupportedOperationException("Not yet implemented: TriggerRegisterGameStateEvent");
					}
					return new HandleJassValue(eventType, CommonEnvironment.this.simulation
							.registerTimeOfDayEvent(globalScope, trigger, opcode, limitval.doubleValue()));
				}
			});
			if (JassSettings.CONTINUE_EXECUTING_ON_ERROR) {
				// TODO this is a dumb stub that wont function
				jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterUnitStateEvent",
						new JassFunction() {
							@Override
							public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
									final TriggerExecutionScope triggerScope) {
								return new HandleJassValue(eventType, RemovableTriggerEvent.DO_NOTHING);
							}
						});
			}
			jassProgramVisitor.getJassNativeManager().createNative("DialogCreate", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(dialogType, meleeUI.createScriptDialog(globalScope));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DialogDestroy", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					meleeUI.destroyDialog(dialog);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DialogClear", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					meleeUI.clearDialog(dialog);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DialogSetMessage", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String messageText = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					dialog.setTitle(CommonEnvironment.this.gameUI, messageText);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DialogAddButton", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CScriptDialog dialog = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String buttonText = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					final int hotkeyInt = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					meleeUI.createScriptDialogButton(dialog, buttonText, (char) hotkeyInt);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DialogDisplay", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CScriptDialog whichDialog = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final boolean flag = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					if (player.getId() == war3MapViewer.getLocalPlayerIndex()) {
						whichDialog.setVisible(flag);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAcquireRange", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final double range = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					unit.setAcquisitionRange((float) range);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetClickedButton", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(buttonType,
							((CommonTriggerExecutionScope) triggerScope).getClickedButton());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetClickedDialog", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(dialogType,
							((CommonTriggerExecutionScope) triggerScope).getClickedDialog());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterDialogEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CScriptDialog dialog = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					if (dialog == null) {
						return new HandleJassValue(eventType, RemovableTriggerEvent.DO_NOTHING);
					}
					return new HandleJassValue(eventType, dialog.addEvent(trigger));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterDialogButtonEvent",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
							final CScriptDialogButton dialogButton = arguments.get(1)
									.visit(ObjectJassValueVisitor.getInstance());
							if (dialogButton == null) {
								return new HandleJassValue(eventType, RemovableTriggerEvent.DO_NOTHING);
							}
							return new HandleJassValue(eventType, dialogButton.addEvent(trigger));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetEventGameState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("Not yet implemented: GetEventGameState");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterGameEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final JassGameEventsWar3 gameEvent = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(eventType,
							CommonEnvironment.this.simulation.registerGameEvent(globalScope, trigger, gameEvent));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetWinningPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("Not yet implemented: GetWinningPlayer");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterEnterRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final CRegion region = arguments.get(1).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final TriggerBooleanExpression boolexpr = nullable(arguments, 2,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					return new HandleJassValue(eventType,
							region.add(new CRegionTriggerEnter(globalScope, trigger, boolexpr)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggeringRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getEnteringUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnteringUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getEnteringUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterLeaveRegion", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
					final CRegion region = arguments.get(1).visit(ObjectJassValueVisitor.<CRegion>getInstance());
					final TriggerBooleanExpression boolexpr = nullable(arguments, 2,
							ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
					return new HandleJassValue(eventType,
							region.add(new CRegionTriggerLeave(globalScope, trigger, boolexpr)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetLeavingUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getLeavingUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTrackableHitEvent",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							throw new UnsupportedOperationException(
									"TriggerRegisterTrackableHitEvent not yet implemented ???");
							// dont feel like implementing this atm, although it probably wouldnt be that
							// hard to do
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterTrackableTrackEvent",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							throw new UnsupportedOperationException(
									"TriggerRegisterTrackableTrackEvent not yet implemented ???");
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggeringTrackable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("GetTriggeringTrackable not yet implemented ???");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetClickedButton", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("GetClickedButton not yet implemented ???");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetClickedDialog", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("GetClickedDialog not yet implemented ???");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentFinishSoonTimeRemaining",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							throw new UnsupportedOperationException(
									"GetTournamentFinishSoonTimeRemaining not yet implemented ???");
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentFinishNowRule", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("GetTournamentFinishNowRule not yet implemented ???");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentFinishNowPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("GetTournamentFinishNowPlayer not yet implemented ???");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTournamentScore", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("GetTournamentScore not yet implemented ???");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSaveBasicFilename", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					throw new UnsupportedOperationException("GetSaveBasicFilename not yet implemented ???");
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterPlayerEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CPlayerJass whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					if (whichPlayer == null) {
						return eventType.getNullValue();
					}
					final JassGameEventsWar3 whichPlayerEvent = arguments.get(2)
							.visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(eventType,
							whichPlayer.addEvent(globalScope, whichTrigger, whichPlayerEvent));
				}
			});
			// TODO past this point things are inconsistent about ordering
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraMargin", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int whichMargin = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final Rectangle playableMapArea = war3MapViewer.terrain.getPlayableMapArea();
					switch (whichMargin) {
					case 0:// CAMERA_MARGIN_LEFT
						return new RealJassValue(war3MapViewer.terrain.getDefaultCameraBounds()[0] - playableMapArea.x);
					case 1:// CAMERA_MARGIN_RIGHT
						return new RealJassValue((playableMapArea.x + playableMapArea.width)
								- war3MapViewer.terrain.getDefaultCameraBounds()[2]);
					case 2:// CAMERA_MARGIN_TOP
						return new RealJassValue((playableMapArea.y + playableMapArea.height)
								- war3MapViewer.terrain.getDefaultCameraBounds()[3]);
					case 3:// CAMERA_MARGIN_BOTTOM
						return new RealJassValue(war3MapViewer.terrain.getDefaultCameraBounds()[1] - playableMapArea.y);
					default:
						throw new IllegalArgumentException(
								"Must input one of these constants: [CAMERA_MARGIN_LEFT, CAMERA_MARGIN_RIGHT, CAMERA_MARGIN_TOP, CAMERA_MARGIN_BOTTOM]");
					}
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMinX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new RealJassValue(meleeUI.getCameraManager().getCameraBounds().getX());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMinY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new RealJassValue(meleeUI.getCameraManager().getCameraBounds().getY());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMaxX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle cameraBounds = meleeUI.getCameraManager().getCameraBounds();
					return new RealJassValue(cameraBounds.getX() + cameraBounds.getWidth());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetCameraBoundMaxY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle cameraBounds = meleeUI.getCameraManager().getCameraBounds();
					return new RealJassValue(cameraBounds.getY() + cameraBounds.getHeight());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetCameraBounds", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final double x1 = arguments.get(0).visit(RealJassValueVisitor.getInstance());
					final double y1 = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final double x2 = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y2 = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double x3 = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					final double y3 = arguments.get(5).visit(RealJassValueVisitor.getInstance());
					final double x4 = arguments.get(6).visit(RealJassValueVisitor.getInstance());
					final double y4 = arguments.get(7).visit(RealJassValueVisitor.getInstance());
					final float left = (float) Math.min(Math.min(x1, x2), Math.min(x3, x4));
					final float bottom = (float) Math.min(Math.min(y1, y2), Math.min(y3, y4));
					final float right = (float) Math.max(Math.max(x1, x2), Math.max(x3, x4));
					final float top = (float) Math.max(Math.max(y1, y2), Math.max(y3, y4));
					meleeUI.getCameraManager().setCameraBounds(new Rectangle(left, bottom, right - left, top - bottom));
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetDayNightModels", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String terrainDNCFile = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final String unitDNCFile = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					war3MapViewer.setDayNightModels(terrainDNCFile, unitDNCFile);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("NewSoundEnvironment", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String environmentName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					System.err.println("#########");
					System.err.println("# Engine requested sound environment: " + environmentName);
					System.err.println("# I don't know how to do that on LibGDX, so for now, I do nothing!");
					System.err.println("#########");
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateMIDISound", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String soundLabel = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final int fadeInRate = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int fadeOutRate = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(soundType, new CMIDISound(soundLabel, fadeInRate, fadeOutRate));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetFloatGameState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CGameState gameState = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					switch (gameState) {
					case TIME_OF_DAY:
						return new RealJassValue(CommonEnvironment.this.simulation.getGameTimeOfDay());
					}
					throw new IllegalArgumentException("Not a float game state: " + gameState);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetIntegerGameState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CGameState gameState = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					switch (gameState) {
					case DISCONNECTED:
					case DIVINE_INTERVENTION:
						return new IntegerJassValue(0); // TODO
					}
					throw new IllegalArgumentException("Not an integer game state: " + gameState);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetFloatGameState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CGameState gameState = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final float value = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					switch (gameState) {
					case TIME_OF_DAY:
						CommonEnvironment.this.simulation.setGameTimeOfDay(value);
						return null;
					}
					throw new IllegalArgumentException("Not a float game state: " + gameState);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("StartSound", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CSound soundHandle = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					soundHandle.start();
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetMapMusic", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String musicName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final boolean random = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					final int index = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());

					final String musicField = CommonEnvironment.this.gameUI.trySkinField(musicName);
					meleeUI.playMusic(musicField, random, index);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("PlayMusic", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String musicName = arguments.get(0).visit(StringJassValueVisitor.getInstance());

					final String musicField = CommonEnvironment.this.gameUI.trySkinField(musicName);
					meleeUI.playMusic(musicField, true, 0);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					return new HandleJassValue(itemType,
							CommonEnvironment.this.simulation.createItem(new War3ID(rawcode), (float) x, (float) y));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddItemById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());

					final CItem newItem = CommonEnvironment.this.simulation.createItem(new War3ID(rawcode), unit.getX(),
							unit.getY());
					unit.getInventoryData().giveItem(CommonEnvironment.this.simulation, unit, newItem, false);

					return new HandleJassValue(itemType, newItem);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddItemToSlotById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int slot = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());

					final CItem newItem = CommonEnvironment.this.simulation.createItem(new War3ID(rawcode), unit.getX(),
							unit.getY());
					unit.getInventoryData().giveItem(CommonEnvironment.this.simulation, unit, newItem, slot, false);

					return new HandleJassValue(itemType, newItem);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ChooseRandomItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int level = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final War3ID randomItemId = CommonEnvironment.this.simulation.getItemData().chooseRandomItem(level,
							CommonEnvironment.this.simulation.getSeededRandom());
					return new IntegerJassValue(randomItemId == null ? 0 : randomItemId.getValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ChooseRandomItemEx", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItemTypeJass whichType = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final War3ID randomItemId = CommonEnvironment.this.simulation.getItemData()
							.chooseRandomItem(whichType, level, CommonEnvironment.this.simulation.getSeededRandom());
					return new IntegerJassValue(randomItemId == null ? 0 : randomItemId.getValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateDestructable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float scale = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
					final int variation = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(destructableType, CommonEnvironment.this.simulation
							.createDestructable(new War3ID(rawcode), x, y, facing, scale, variation));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("BlzCreateDestructableWithSkin", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float scale = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
					final int variation = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					final int skinId = arguments.get(6).visit(IntegerJassValueVisitor.getInstance());
					if (skinId != rawcode) {
//						throw new IllegalStateException(
//								"Our engine does not support DestructableSkinID != DestructableID (skinId="
//										+ new War3ID(skinId) + ", destId=" + new War3ID(rawcode) + ")");
						rawcode = skinId;
					}
					return new HandleJassValue(destructableType, CommonEnvironment.this.simulation
							.createDestructable(new War3ID(rawcode), x, y, facing, scale, variation));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateDestructableZ", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float z = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float facing = arguments.get(4).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float scale = arguments.get(5).visit(RealJassValueVisitor.getInstance()).floatValue();
					final int variation = arguments.get(6).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(destructableType, CommonEnvironment.this.simulation
							.createDestructableZ(new War3ID(rawcode), x, y, z, facing, scale, variation));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("KillDestructable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CDestructable dest = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					dest.setLife(CommonEnvironment.this.simulation, 0f);
					return null;
				}
			});

			jassProgramVisitor.getJassNativeManager().createNative("CreateUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double facing = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					final War3ID rawcodeId = new War3ID(rawcode);
					final CUnit newUnit = CommonEnvironment.this.simulation.createUnitSimple(rawcodeId, player.getId(),
							(float) x, (float) y, (float) facing);
					return new HandleJassValue(unitType, newUnit);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("BlzCreateUnitWithSkin", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double facing = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					final int skinId = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					final War3ID rawcodeId = new War3ID(rawcode);
					final CUnit newUnit = CommonEnvironment.this.simulation.createUnitSimple(rawcodeId, player.getId(),
							(float) x, (float) y, (float) facing);
					if (skinId != rawcode) {
//						throw new IllegalStateException("Our engine does not support UnitSkinID != UnitID (skinId="
//								+ new War3ID(skinId) + ", unitId=" + new War3ID(rawcode) + ")");
						rawcode = skinId;
					}
					return new HandleJassValue(unitType, newUnit);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateUnitAtLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final Point2D.Double whichLocation = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					final float facing = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final War3ID rawcodeId = new War3ID(rawcode);
					final float x = whichLocation == null ? 0 : (float) whichLocation.x;
					final float y = whichLocation == null ? 0 : (float) whichLocation.y;
					final CUnit newUnit = CommonEnvironment.this.simulation.createUnitSimple(rawcodeId, player.getId(),
							x, y, facing);
					return new HandleJassValue(unitType, newUnit);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateBlightedGoldmine", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO this needs to setup a non-blighted mine underneath!!!
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double facing = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final War3ID blightedMineRawcode = War3ID.fromString("ugol");
					final War3ID goldMineRawcode = War3ID.fromString("ngol");
					player.addTechtreeUnlocked(blightedMineRawcode);
					final CUnit blightedMine = CommonEnvironment.this.simulation.createUnitSimple(blightedMineRawcode,
							player.getId(), (float) x, (float) y, (float) facing);
					final CUnit goldMine = CommonEnvironment.this.simulation.createUnitSimple(goldMineRawcode,
							WarsmashConstants.MAX_PLAYERS - 1, (float) x, (float) y, (float) facing);
					goldMine.setHidden(true);
					for (final CAbility ability : blightedMine.getAbilities()) {
						if (ability instanceof CAbilityBlightedGoldMine) {
							((CAbilityBlightedGoldMine) ability).setParentMine(goldMine, goldMine.getGoldMineData());
						}
					}
					return new HandleJassValue(unitType, blightedMine);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitColor", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CPlayerColor whichPlayerColor = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final RenderUnit renderPeer = war3MapViewer.getRenderPeer(whichUnit);
					renderPeer.setPlayerColor(whichPlayerColor.ordinal());
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitOwner", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final boolean changeColor = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());

					whichUnit.setPlayerIndex(CommonEnvironment.this.simulation, whichPlayer.getId(), changeColor);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetResourceAmount", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int resourceAmount = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					whichUnit.setGold(resourceAmount);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetResourceAmount", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new IntegerJassValue(whichUnit.getGold());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final CUnitState whichUnitState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final float value = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					if (whichUnit != null) {
						whichUnit.setUnitState(CommonEnvironment.this.simulation, whichUnitState, value);
					}
					else {
						System.err.println("got SetUnitState(null," + whichUnitState + "," + value
								+ ")  call (skipping because unit is null)");
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CUnitState whichUnitState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return RealJassValue.ZERO;
					}
					return new RealJassValue(whichUnit.getUnitState(CommonEnvironment.this.simulation, whichUnitState));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetHeroLevel", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int level = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final boolean fx = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final CAbilityHero heroData = whichUnit.getHeroData();
					if (heroData != null) {
						heroData.setHeroLevel(CommonEnvironment.this.simulation, whichUnit, level, fx);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CUnitTypeJass whichUnitType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(whichUnit.isUnitType(whichUnitType));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CPlayerState whichPlayerState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final int value = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					player.setPlayerState(CommonEnvironment.this.simulation, whichPlayerState, value);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerState", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CPlayerState whichPlayerState = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return new IntegerJassValue(
							player.getPlayerState(CommonEnvironment.this.simulation, whichPlayerState));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTechResearched", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final boolean specificOnly = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					return BooleanJassValue.of(player.getTechtreeUnlocked(new War3ID(techIdRawcode)) > 0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTechCount", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final boolean specificOnly = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					return new IntegerJassValue(player.getTechtreeUnlocked(new War3ID(techIdRawcode)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTechResearched", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int setToLevel = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final War3ID techIdRawcodeId = new War3ID(techIdRawcode);
					final CUpgradeType upgradeType = CommonEnvironment.this.simulation.getUpgradeData()
							.getType(techIdRawcodeId);
					if (upgradeType != null) {
						player.setTechResearched(CommonEnvironment.this.simulation, techIdRawcodeId, setToLevel);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AddPlayerTechResearched", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int levels = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final War3ID techIdRawcodeId = new War3ID(techIdRawcode);
					final CUpgradeType upgradeType = CommonEnvironment.this.simulation.getUpgradeData()
							.getType(techIdRawcodeId);
					if (upgradeType != null) {
						player.addTechResearched(CommonEnvironment.this.simulation, techIdRawcodeId, levels);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTechMaxAllowed", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int maximum = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					player.setTechtreeMaxAllowed(new War3ID(techIdRawcode), maximum);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTechMaxAllowed", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int techIdRawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					return new IntegerJassValue(player.getTechtreeMaxAllowed(new War3ID(techIdRawcode)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsFogEnabled", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO fog of war!!
					return BooleanJassValue.FALSE;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsFogMaskEnabled", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO fog of war!!
					return BooleanJassValue.FALSE;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateSoundFromLabel", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String soundLabel = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final boolean looping = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					final boolean is3D = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final boolean stopWhenOutOfRange = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					final int fadeInRate = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
					final int fadeOutRate = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					final UnitSound sound = war3MapViewer.getUiSounds().getSound(soundLabel);
					return new HandleJassValue(soundType,
							new CSoundFromLabel(sound,
									is3D ? war3MapViewer.worldScene.audioContext : meleeUI.getUiScene().audioContext,
									looping, is3D, stopWhenOutOfRange, fadeInRate, fadeOutRate));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateSound", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String fileName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final boolean looping = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					final boolean is3D = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final boolean stopWhenOutOfRange = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					final int fadeInRate = arguments.get(4).visit(IntegerJassValueVisitor.getInstance());
					final int fadeOutRate = arguments.get(5).visit(IntegerJassValueVisitor.getInstance());
					final String eaxSetting = arguments.get(6).visit(StringJassValueVisitor.getInstance());
					final Sound newSound = UnitSound.createSound(war3MapViewer.mapMpq, fileName);

					return new HandleJassValue(soundType,
							new CSoundFilename(newSound,
									is3D ? war3MapViewer.worldScene.audioContext : meleeUI.getUiScene().audioContext,
									looping, stopWhenOutOfRange, fadeInRate, fadeOutRate, eaxSetting));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundParamsFromLabel", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CSoundFilename sound = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String soundLabel = arguments.get(1).visit(StringJassValueVisitor.getInstance());

					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("VersionGet", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(versionType, CVersion.VALUES[WarsmashConstants.GAME_VERSION]);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetAllItemTypeSlots", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int slots = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.setAllItemTypeSlots(slots);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetAllUnitTypeSlots", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int slots = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.setAllItemTypeSlots(slots);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterPlayerUnitEvent",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
							final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
							final JassGameEventsWar3 whichPlayerEvent = arguments.get(2)
									.visit(ObjectJassValueVisitor.getInstance());
							final TriggerBooleanExpression filter = nullable(arguments, 3,
									ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
							return new HandleJassValue(eventType,
									whichPlayer.addUnitEvent(globalScope, whichTrigger, whichPlayerEvent, filter));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerEvaluate", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichTrigger == null) {
						return BooleanJassValue.FALSE;
					}
					return BooleanJassValue.of(whichTrigger.evaluate(globalScope,
							new CommonTriggerExecutionScope(whichTrigger, triggerScope)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerExecute", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					whichTrigger.execute(globalScope, new CommonTriggerExecutionScope(whichTrigger, triggerScope));
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("Preloader", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String filename = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					doPreloadScript(dataSource, war3MapViewer, filename);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateTimerDialog", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CTimerJass timer = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(timerdialogType, meleeUI.createTimerDialog(timer));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsPlayerObserver", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return BooleanJassValue.of(player.isObserver());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundParamsFromLabel", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CSoundFilename sound = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String soundLabel = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					// TODO NYI
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundDuration", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CSoundFilename sound = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int duration = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					// TODO NYI
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundPitch", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CSoundFilename sound = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final float pitch = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					// TODO NYI
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetSoundVolume", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CSoundFilename sound = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int volume = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					// TODO NYI
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AddWeatherEffect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle where = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int effectId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					// TODO NYI
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("EnableWeatherEffect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Rectangle where = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean enable = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					// TODO NYI
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterDeathEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CWidget whichWidget = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(eventType, whichWidget.addDeathEvent(globalScope, whichTrigger));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerRegisterUnitEvent", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CUnit whichWidget = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final JassGameEventsWar3 whichPlayerEvent = arguments.get(2)
							.visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(eventType,
							whichWidget.addEvent(globalScope, whichTrigger, whichPlayerEvent));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitHidden", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (unit == null) {
						return BooleanJassValue.TRUE; // TODO this is a workaround, probably
					}
					return BooleanJassValue.of(unit.isHidden());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("PauseUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					unit.setPaused(flag);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetPlayerHandicapXP", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer player = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final float handicap = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					player.setHandicapXP(handicap);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetChangingUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO this is supposed to have some magic nonsense going on where apparently
					// EVENT_WIDGET_DEATH
					// assigns to the return value of this and fires itself upon changing owner!!??
					return unitType.getNullValue();
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetChangingUnitPrevOwner", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO this is supposed to have some magic nonsense going on where apparently
					// EVENT_WIDGET_DEATH
					// assigns to the return value of this and fires itself upon changing owner!!??
					return playerType.getNullValue();
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRandomReal", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final float lowBound = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float highBound = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					return new RealJassValue(
							(CommonEnvironment.this.simulation.getSeededRandom().nextFloat() * (highBound - lowBound))
									+ lowBound);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetRandomInt", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					int lowBound = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					int highBound = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (lowBound > highBound) {
						if (highBound >= 0) {
							lowBound = highBound;
						}
						else {
							highBound = lowBound;
						}
					}
					return new IntegerJassValue(
							CommonEnvironment.this.simulation.getSeededRandom().nextInt((highBound - lowBound) + 1)
									+ lowBound);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetWidgetX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CWidget whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichWidget.getX());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetWidgetY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CWidget whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichWidget.getY());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichWidget == null ? 0 : whichWidget.getX());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichWidget == null ? 0 : whichWidget.getY());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitPointValue", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new IntegerJassValue(whichWidget.getUnitType().getPointValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitPointValueByType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new IntegerJassValue(CommonEnvironment.this.simulation.getUnitData()
							.getUnitType(new War3ID(rawcode)).getPointValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichWidget.getX());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichWidget.getY());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(locationType,
							new Point2D.Double(whichWidget.getX(), whichWidget.getY()));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichWidget = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (whichWidget == null) {
						return new HandleJassValue(locationType, new Point2D.Double(0, 0));
					}
					return new HandleJassValue(locationType,
							new Point2D.Double(whichWidget.getX(), whichWidget.getY()));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAbilityLevel", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (whichWidget == null) {
						return IntegerJassValue.ZERO;
					}
					final CAbility ability = whichWidget
							.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(new War3ID(rawcode)));
					// TODO below code is very stupid!!
					return new IntegerJassValue(ability == null ? 0 : 1);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IncUnitAbilityLevel", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int rawcode = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final War3ID war3id = new War3ID(rawcode);
					final CAbility ability = whichWidget
							.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(war3id));
					if (ability == null) {
						whichWidget.add(CommonEnvironment.this.simulation,
								CommonEnvironment.this.simulation.getAbilityData().createAbility(war3id,
										CommonEnvironment.this.simulation.getHandleIdAllocator().createId()));
						// TODO below code is very stupid!!
						return new IntegerJassValue(1);
					}
					else {
						// TODO below code is very stupid!!
						return new IntegerJassValue(1);
					}
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerHandicap", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichPlayer.getHandicap());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetHandleId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CHandle whichHandle = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichHandle == null) {
						return IntegerJassValue.ZERO;
					}
					return new IntegerJassValue(whichHandle.getHandleId());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("TriggerSleepAction", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final double time = arguments.get(0).visit(RealJassValueVisitor.getInstance());
					if (time != 0) {
						throw new JassException(globalScope, "Needs to sleep " + time, null);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffectTarget", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final CWidget targetWidget = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final String attachPointName = arguments.get(2).visit(StringJassValueVisitor.getInstance());
					return new HandleJassValue(effectType,
							war3MapViewer.addSpecialEffectTarget(modelName, targetWidget, attachPointName));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AddSpecialEffectLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final String modelName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
					final Point2D.Double positionLoc = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(effectType, war3MapViewer.addSpecialEffect(modelName,
							(float) positionLoc.x, (float) positionLoc.y, 0 /* facing */));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AddSpellEffectById", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final CEffectType whichEffectType = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final double x = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double y = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					return new HandleJassValue(effectType, war3MapViewer.spawnSpellEffectEx((float) x, (float) y,
							0 /* facing */, new War3ID(rawcode), whichEffectType, 0));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitFacing", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final double facing = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					whichUnit.setFacing((float) facing);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("BlzSetUnitFacingEx", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final double facing = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					whichUnit.setFacing((float) facing);
					war3MapViewer.getRenderPeer(whichUnit).setFacing((float) facing);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DestroyEffect", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final RenderSpellEffect fx = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					fx.setKillWhenDone(true);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetItemInvulnerable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					whichWidget.setInvulernable(flag);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetDestructableInvulnerable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CDestructable whichWidget = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					whichWidget.setInvulnerable(flag);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SuspendTimeOfDay", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final boolean flag = arguments.get(0).visit(BooleanJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.setTimeOfDaySuspended(flag);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CItem whichItem = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final CAbilityInventory inventoryData = whichUnit.getInventoryData();
					if (inventoryData != null) {
						inventoryData.giveItem(CommonEnvironment.this.simulation, whichUnit, whichItem, false);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitAddAbility", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int abilityId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final War3ID rawcode = new War3ID(abilityId);

					final CAbilityType<?> abilityType = CommonEnvironment.this.simulation.getAbilityData()
							.getAbilityType(rawcode);
					if (abilityType == null) {
						System.err.println(
								"UnitAddAbility: The requested ability has not been programmed yet: " + rawcode);
						return BooleanJassValue.FALSE;
					}

					final CLevelingAbility existingAbility = whichUnit
							.getAbility(GetAbilityByRawcodeVisitor.getInstance().reset(rawcode));
					if (existingAbility != null) {
						return BooleanJassValue.FALSE;
					}
					final CAbility ability = abilityType
							.createAbility(CommonEnvironment.this.simulation.getHandleIdAllocator().createId());
					whichUnit.add(CommonEnvironment.this.simulation, ability);

					return BooleanJassValue.TRUE;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("UnitItemInSlot", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int whichSlot = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return new HandleJassValue(itemType, null);
					}
					final CAbilityInventory inventoryData = whichUnit.getInventoryData();
					if (inventoryData != null) {
						return new HandleJassValue(itemType, inventoryData.getItemInSlot(whichSlot));
					}
					return new HandleJassValue(itemType, null);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetCameraTargetController", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final float xoffset = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float yoffset = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean inheritOrientation = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					meleeUI.getCameraManager().setTargetController(war3MapViewer.getRenderPeer(whichUnit), xoffset,
							yoffset, inheritOrientation);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("PanCameraToTimed", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final float x = arguments.get(0).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					meleeUI.getCameraManager().target.x = x;
					meleeUI.getCameraManager().target.y = y;
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CameraSetupGetField", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO NYI
					return new RealJassValue(0.0f);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("LeaderboardGetItemCount", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO NYI
					return new IntegerJassValue(0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("LeaderboardGetPlayerIndex", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					// TODO NYI
					return new IntegerJassValue(0);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitInvulnerable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean flag = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					whichUnit.setInvulnerable(flag);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitPositionLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final Point2D.Double positionLoc = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit != null) {
						whichUnit.setPointAndCheckUnstuck((float) positionLoc.x, (float) positionLoc.y,
								CommonEnvironment.this.simulation);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitPosition", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final double positionX = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					final double positionY = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					if (whichUnit != null) {
						whichUnit.setPointAndCheckUnstuck((float) positionX, (float) positionY,
								CommonEnvironment.this.simulation);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("ShowUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean show = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					whichUnit.setHidden(!show);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("KillUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					whichUnit.kill(CommonEnvironment.this.simulation);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.removeUnit(whichUnit);
					meleeUI.removedUnit(whichUnit);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RemoveItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.removeItem(whichItem);
					meleeUI.removedItem(whichItem);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitAnimation", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String animation = CommonEnvironment.this.gameUI
							.getTrigStr(arguments.get(1).visit(StringJassValueVisitor.getInstance()));
					final EnumSet<AnimationTokens.PrimaryTag> primaryTags = EnumSet
							.noneOf(AnimationTokens.PrimaryTag.class);
					final EnumSet<AnimationTokens.SecondaryTag> secondaryTags = EnumSet
							.noneOf(AnimationTokens.SecondaryTag.class);
					Sequence.populateTags(primaryTags, secondaryTags, animation);
					if (!primaryTags.isEmpty()) {
						whichUnit.getUnitAnimationListener().playAnimation(true, primaryTags.iterator().next(),
								secondaryTags, 1.0f, true);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerWidget", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(widgetType,
							((CommonTriggerExecutionScope) triggerScope).getTriggerWidget());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerRace", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichPlayer == null) {
						return raceType.getNullValue();
					}
					return new HandleJassValue(raceType, whichPlayer.getRace());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitUserData", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return IntegerJassValue.ZERO;
					}
					return new IntegerJassValue(whichUnit.getTriggerEditorCustomValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetUnitUserData", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int data = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					if (whichUnit != null) {
						whichUnit.setTriggerEditorCustomValue(data);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetDestructableLife", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CDestructable whichDestructable = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					if (whichDestructable == null) {
						// TODO this should not be the way to solve the problem i am facing
						return RealJassValue.ZERO;
					}
					return new RealJassValue(whichDestructable.getLife());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetDestructableMaxLife", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CDestructable whichDestructable = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					return new RealJassValue(whichDestructable.getMaxLife());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetDestructableAnimation", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CDestructable whichDestructable = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final String animation = CommonEnvironment.this.gameUI
							.getTrigStr(arguments.get(1).visit(StringJassValueVisitor.getInstance()));
					final RenderDestructable renderPeer = war3MapViewer.getRenderPeer(whichDestructable);
					renderPeer.setAnimation(animation);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DestructableRestoreLife", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CDestructable whichDestructable = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final float life = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean birth = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					// TODO this "restore" function, is it a summation or assignment below???
					// Guessing assign is OK
					whichDestructable.setLife(CommonEnvironment.this.simulation, life);
					if (!birth) {
						final RenderDestructable renderPeer = war3MapViewer.getRenderPeer(whichDestructable);
						renderPeer.notifyLifeRestored();
					} // else birth plays automatically
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DisplayTimedTextToPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float duration = arguments.get(3).visit(RealJassValueVisitor.getInstance()).floatValue();
					final String message = CommonEnvironment.this.gameUI
							.getTrigStr(arguments.get(4).visit(StringJassValueVisitor.getInstance()));
					System.err.println("DisplayTimedTextToPlayer: " + message.replace("\\n", "\n"));
					if (whichPlayer == CommonEnvironment.this.simulation
							.getPlayer(war3MapViewer.getLocalPlayerIndex())) {
						meleeUI.displayTimedText(x, y, duration, message);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("DisplayTextToPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final float x = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
					final float y = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final String message = CommonEnvironment.this.gameUI
							.getTrigStr(arguments.get(3).visit(StringJassValueVisitor.getInstance()));
					if (whichPlayer == CommonEnvironment.this.simulation
							.getPlayer(war3MapViewer.getLocalPlayerIndex())) {
						meleeUI.displayTimedText(x, y, (message.length() / 6) + 5, message);
					}
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsPlayerInForce", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final List<CPlayerJass> force = arguments.get(1)
							.visit(ObjectJassValueVisitor.<List<CPlayerJass>>getInstance());
					return BooleanJassValue.of(force.contains(whichPlayer));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitTypeId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return new IntegerJassValue(0);
					}
					return new IntegerJassValue(whichUnit.getTypeId().getValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemTypeId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichItem == null) {
						return new IntegerJassValue(0);
					}
					return new IntegerJassValue(whichItem.getTypeId().getValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichItem == null) {
						return itemtypeType.getNullValue();
					}
					return new HandleJassValue(itemtypeType, whichItem.getItemType().getItemClass());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemLevel", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CItem whichItem = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichItem == null) {
						return new IntegerJassValue(0);
					}
					return new IntegerJassValue(whichItem.getItemType().getLevel());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOwningPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					if (whichUnit == null) {
						return playerType.getNullValue();
					}
					return new HandleJassValue(playerType,
							CommonEnvironment.this.simulation.getPlayer(whichUnit.getPlayerIndex()));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetFilterPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(playerType,
							((CommonTriggerExecutionScope) triggerScope).getFilterPlayer());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetDyingUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getDyingUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetAttacker", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getAttacker());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetKillingUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getKillingUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getTriggeringUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTriggerDestructable", new JassFunction() {

				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CWidget triggerWidget = ((CommonTriggerExecutionScope) triggerScope).getTriggerWidget();
					if (triggerWidget instanceof CDestructable) {
						return new HandleJassValue(destructableType, triggerWidget);
					}
					else {
						return new HandleJassValue(destructableType, null);
					}
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderedUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getOrderedUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetManipulatedItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(itemType,
							((CommonTriggerExecutionScope) triggerScope).getManipulatedItem());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetManipulatingUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getManipulatingUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetIssuedOrderId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new IntegerJassValue(((CommonTriggerExecutionScope) triggerScope).getIssuedOrderId());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderPointX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new RealJassValue(((CommonTriggerExecutionScope) triggerScope).getOrderPointX());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderPointY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new RealJassValue(((CommonTriggerExecutionScope) triggerScope).getOrderPointY());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderPointLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CommonTriggerExecutionScope commonTriggerExecutionScope = (CommonTriggerExecutionScope) triggerScope;
					final Point2D.Double jassLocation = new Point2D.Double(commonTriggerExecutionScope.getOrderPointX(),
							commonTriggerExecutionScope.getOrderPointY());
					return new HandleJassValue(locationType, jassLocation);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTarget", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(widgetType,
							((CommonTriggerExecutionScope) triggerScope).getOrderTarget());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTargetDestructable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(destructableType,
							((CommonTriggerExecutionScope) triggerScope).getOrderTargetDestructable());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTargetItem", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(itemType,
							((CommonTriggerExecutionScope) triggerScope).getOrderTargetItem());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetOrderTargetUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getOrderTargetUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getSpellAbilityUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getSpellTargetUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetPoint", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
							.getSpellTargetPoint();
					final Point2D.Double jassLocation = new Point2D.Double(spellTargetPoint.x, spellTargetPoint.y);
					return new HandleJassValue(locationType, jassLocation);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
							.getSpellTargetPoint();
					return new RealJassValue(spellTargetPoint.x);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
							.getSpellTargetPoint();
					return new RealJassValue(spellTargetPoint.y);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final AbilityPointTarget spellTargetPoint = ((CommonTriggerExecutionScope) triggerScope)
							.getSpellTargetPoint();
					return new HandleJassValue(locationType,
							new Point2D.Double(spellTargetPoint.x, spellTargetPoint.y));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final War3ID spellAbilityId = ((CommonTriggerExecutionScope) triggerScope).getSpellAbilityId();
					return new IntegerJassValue(spellAbilityId == null ? 0 : spellAbilityId.getValue());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetEnumPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(playerType,
							((CommonTriggerExecutionScope) triggerScope).getEnumPlayer());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetConstructedStructure", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getConstructedStructure());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetResearchingUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType,
							((CommonTriggerExecutionScope) triggerScope).getResearchingUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetResearched", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new IntegerJassValue(((CommonTriggerExecutionScope) triggerScope).getResearched());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTrainedUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(unitType, ((CommonTriggerExecutionScope) triggerScope).getTrainedUnit());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetTrainedUnitType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new IntegerJassValue(((CommonTriggerExecutionScope) triggerScope).getTrainedUnitType());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitRallyPoint", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					float x, y;
					if (whichUnit != null) {
						final AbilityTarget rallyPoint = whichUnit.getRallyPoint();
						if (rallyPoint != null) {
							x = rallyPoint.getX();
							y = rallyPoint.getY();
						}
						else {
							x = y = 0;
						}
					}
					else {
						x = y = 0;
					}
					return new HandleJassValue(locationType, new Point2D.Double(x, y));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitRallyUnit", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					HandleJassValue rallyUnit;
					if (whichUnit != null) {
						final AbilityTarget rallyPoint = whichUnit.getRallyPoint();
						if (rallyPoint != null) {
							rallyUnit = rallyPoint.visit(new AbilityTargetVisitor<HandleJassValue>() {
								@Override
								public HandleJassValue accept(final AbilityPointTarget target) {
									return unitType.getNullValue();
								}

								@Override
								public HandleJassValue accept(final CUnit target) {
									return new HandleJassValue(unitType, target);
								}

								@Override
								public HandleJassValue accept(final CDestructable target) {
									return unitType.getNullValue();
								}

								@Override
								public HandleJassValue accept(final CItem target) {
									return unitType.getNullValue();
								}
							});
						}
						else {
							rallyUnit = unitType.getNullValue();
						}
					}
					else {
						rallyUnit = unitType.getNullValue();
					}
					return rallyUnit;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitRallyDestructable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					HandleJassValue rallyDest;
					if (whichUnit != null) {
						final AbilityTarget rallyPoint = whichUnit.getRallyPoint();
						if (rallyPoint != null) {
							rallyDest = rallyPoint.visit(new AbilityTargetVisitor<HandleJassValue>() {
								@Override
								public HandleJassValue accept(final AbilityPointTarget target) {
									return destructableType.getNullValue();
								}

								@Override
								public HandleJassValue accept(final CUnit target) {
									return destructableType.getNullValue();
								}

								@Override
								public HandleJassValue accept(final CDestructable target) {
									return new HandleJassValue(destructableType, target);
								}

								@Override
								public HandleJassValue accept(final CItem target) {
									return destructableType.getNullValue();
								}
							});
						}
						else {
							rallyDest = destructableType.getNullValue();
						}
					}
					else {
						rallyDest = destructableType.getNullValue();
					}
					return rallyDest;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitCurrentOrder", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					int orderId;
					if ((whichUnit != null) && (whichUnit.getCurrentOrder() != null)) {
						orderId = whichUnit.getCurrentOrder().getOrderId();
					}
					else {
						orderId = 0;
					}
					return new IntegerJassValue(orderId);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetBlightLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final Point2D.Double whichLocation = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					final float radius = arguments.get(2).visit(RealJassValueVisitor.getInstance()).floatValue();
					final boolean addBlight = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					final float whichLocationX = (float) whichLocation.x;
					final float whichLocationY = (float) whichLocation.y;
					war3MapViewer.setBlight(whichLocationX, whichLocationY, radius, addBlight);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetLocalPlayer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(playerType,
							CommonEnvironment.this.simulation.getPlayer(war3MapViewer.getLocalPlayerIndex()));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitInGroup", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.<CUnit>getInstance());
					final List<CUnit> group = arguments.get(1).visit(ObjectJassValueVisitor.<List<CUnit>>getInstance());
					return BooleanJassValue.of(group.contains(whichUnit));
				}
			});

			// Patch 1.23+ crap
			jassProgramVisitor.getJassNativeManager().createNative("InitHashtable", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(hashtableType, new CHashtable());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SaveInteger", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveReal", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveBoolean", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveStr", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SavePlayerHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveWidgetHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveDestructableHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveItemHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveUnitHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveAbilityHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTimerHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerConditionHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerActionHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTriggerEventHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveForceHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveGroupHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveLocationHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveRectHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveBooleanExprHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveSoundHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveEffectHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveUnitPoolHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveItemPoolHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveQuestHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveQuestItemHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveDefeatConditionHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTimerDialogHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveLeaderboardHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveMultiboardHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveMultiboardItemHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTrackableHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveDialogHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveButtonHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveTextTagHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveLightningHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveImageHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveUbersplatHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveRegionHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveFogStateHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveFogModifierHandle",
					new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveAgentHandle", new SaveHashtableValueFunc());
			jassProgramVisitor.getJassNativeManager().createNative("SaveHashtableHandle", new SaveHashtableValueFunc());

			jassProgramVisitor.getJassNativeManager().createNative("LoadInteger",
					new LoadHashtableValueFunc(IntegerJassValue.ZERO));
			jassProgramVisitor.getJassNativeManager().createNative("LoadReal",
					new LoadHashtableValueFunc(RealJassValue.ZERO));
			jassProgramVisitor.getJassNativeManager().createNative("LoadBoolean",
					new LoadHashtableValueFunc(BooleanJassValue.FALSE));
			jassProgramVisitor.getJassNativeManager().createNative("LoadStr",
					new LoadHashtableValueFunc(StringJassValue.EMPTY_STRING));
			jassProgramVisitor.getJassNativeManager().createNative("LoadTriggerHandle",
					new LoadHashtableValueFunc(new HandleJassValue(triggerType, null)));
			jassProgramVisitor.getJassNativeManager().createNative("LoadSoundHandle",
					new LoadHashtableValueFunc(new HandleJassValue(soundType, null)));

			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedInteger",
					new HaveSavedHashtableValueFunc(JassType.INTEGER));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedReal",
					new HaveSavedHashtableValueFunc(JassType.REAL));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedBoolean",
					new HaveSavedHashtableValueFunc(JassType.BOOLEAN));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedString",
					new HaveSavedHashtableValueFunc(JassType.STRING));
			jassProgramVisitor.getJassNativeManager().createNative("HaveSavedHandle",
					new HaveSavedHashtableValueFunc(agentType));

			jassProgramVisitor.getJassNativeManager().createNative("GetExpiredTimer", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(timerType,
							((CommonTriggerExecutionScope) triggerScope).getExpiringTimer());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerStructureCount", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean includeIncomplete = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					final List<CUnit> units = CommonEnvironment.this.simulation.getUnits();
					int count = 0;
					for (final CUnit unit : units) {
						if (unit.getPlayerIndex() == whichPlayer.getId()) {
							if (includeIncomplete || !unit.isConstructing()) {
								count++;
							}
						}
					}
					return new IntegerJassValue(count);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTypedUnitCount", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CPlayer whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String legacySystemUnitTypeName = arguments.get(1)
							.visit(StringJassValueVisitor.getInstance());
					final boolean includeIncomplete = arguments.get(2).visit(BooleanJassValueVisitor.getInstance());
					final boolean includeUpgrades = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
					final List<CUnit> units = CommonEnvironment.this.simulation.getUnits();
					int count = 0;
					// TODO includeUpgrades is NYI!!
					for (final CUnit unit : units) {
						if (unit.getPlayerIndex() == whichPlayer.getId()) {
							if (legacySystemUnitTypeName.equals(unit.getUnitType().getLegacyName())) {
								if ((includeIncomplete || !unit.isConstructing())
										&& (includeUpgrades || !unit.isUpgrading())) {
									// TODO this might not actually be what includeUpgrades means, it probably means
									// to include higher tier of hall when asked for lower tier type ID
									count++;
								}
							}
						}
					}
					return new IntegerJassValue(count);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitEnemy", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					if ((whichUnit == null) || (whichPlayer == null)) {
						return BooleanJassValue.FALSE;
					}
					return BooleanJassValue.of(!whichUnit.isUnitAlly(whichPlayer));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsUnitAlly", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final CPlayer whichPlayer = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					if ((whichUnit == null) || (whichPlayer == null)) {
						return BooleanJassValue.FALSE;
					}
					return BooleanJassValue.of(whichUnit.isUnitAlly(whichPlayer));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsNoVictoryCheat", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return BooleanJassValue.FALSE;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("IsNoDefeatCheat", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return BooleanJassValue.FALSE;
				}
			});

			// Warsmash Ability API
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitMoveFollowBehavior", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final CWidget whichFollowTarget = nullable(arguments, 2, ObjectJassValueVisitor.getInstance());
					if (unit == null) {
						return null;
					}

					return new HandleJassValue(abilitybehaviorType,
							unit.getFollowBehavior().reset(highlightOrderId, whichFollowTarget));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitMovePointBehavior", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final double targetX = arguments.get(2).visit(RealJassValueVisitor.getInstance());
					final double targetY = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					if (unit == null) {
						return null;
					}

					return new HandleJassValue(abilitybehaviorType, unit.getMoveBehavior().reset(highlightOrderId,
							new AbilityPointTarget((float) targetX, (float) targetY)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitMovePointBehaviorLoc", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final Point2D.Double target = arguments.get(2).visit(ObjectJassValueVisitor.getInstance());
					if (unit == null) {
						return null;
					}

					return new HandleJassValue(abilitybehaviorType, unit.getMoveBehavior().reset(highlightOrderId,
							new AbilityPointTarget((float) target.x, (float) target.y)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackMovePointBehavior",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
							final double targetX = arguments.get(1).visit(RealJassValueVisitor.getInstance());
							final double targetY = arguments.get(2).visit(RealJassValueVisitor.getInstance());
							if (unit == null) {
								return null;
							}

							return new HandleJassValue(abilitybehaviorType, unit.getAttackMoveBehavior()
									.reset(new AbilityPointTarget((float) targetX, (float) targetY)));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackMovePointBehaviorLoc",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
							final Point2D.Double target = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
							if (unit == null) {
								return null;
							}

							return new HandleJassValue(abilitybehaviorType, unit.getAttackMoveBehavior()
									.reset(new AbilityPointTarget((float) target.x, (float) target.y)));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackWidgetBehavior", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int whichUnitAttackIndex = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final CWidget target = nullable(arguments, 3, ObjectJassValueVisitor.getInstance());
					if (unit == null) {
						return null;
					}

					return new HandleJassValue(abilitybehaviorType,
							unit.getAttackBehavior().reset(highlightOrderId,
									unit.getCurrentAttacks().get(whichUnitAttackIndex), target, false,
									CBehaviorAttackListener.DO_NOTHING));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackGroundBehavior", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
					final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int whichUnitAttackIndex = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final double targetX = arguments.get(3).visit(RealJassValueVisitor.getInstance());
					final double targetY = arguments.get(4).visit(RealJassValueVisitor.getInstance());
					if (unit == null) {
						return null;
					}

					return new HandleJassValue(abilitybehaviorType,
							unit.getAttackBehavior().reset(highlightOrderId,
									unit.getCurrentAttacks().get(whichUnitAttackIndex),
									new AbilityPointTarget((float) targetX, (float) targetY), false,
									CBehaviorAttackListener.DO_NOTHING));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitAttackGroundBehaviorLoc",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CUnit unit = nullable(arguments, 0, ObjectJassValueVisitor.getInstance());
							final int highlightOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
							final int whichUnitAttackIndex = arguments.get(2)
									.visit(IntegerJassValueVisitor.getInstance());
							final Point2D.Double target = arguments.get(3).visit(ObjectJassValueVisitor.getInstance());
							if (unit == null) {
								return null;
							}

							return new HandleJassValue(abilitybehaviorType,
									unit.getAttackBehavior().reset(highlightOrderId,
											unit.getCurrentAttacks().get(whichUnitAttackIndex),
											new AbilityPointTarget((float) target.x, (float) target.y), false,
											CBehaviorAttackListener.DO_NOTHING));
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("UnitPollNextOrderBehavior", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CUnit whichUnit = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					return new HandleJassValue(abilitybehaviorType,
							whichUnit.pollNextOrderBehavior(CommonEnvironment.this.simulation));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateAbilityBehavior", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int highlightOrderId = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final JassFunction func = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
					return new HandleJassValue(abilitybehaviorType,
							new CBehaviorJass(highlightOrderId, func, globalScope));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateBehaviorExpr", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassFunction func = arguments.get(0).visit(JassFunctionJassValueVisitor.getInstance());
					return new HandleJassValue(behaviorexprType, new JassFunctionBehaviorExpr(func));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityOrderId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new IntegerJassValue(((CommonTriggerExecutionScope) triggerScope).getSpellAbilityOrderId());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellAbilityOrderButton", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new IntegerJassValue(((CommonTriggerExecutionScope) triggerScope).getSpellAbilityOrderId());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetSpellTargetType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(orderbuttontypeType,
							((CommonTriggerExecutionScope) triggerScope).getSpellAbilityTargetType());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateAbilityType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					return new HandleJassValue(abilitytypeType, new CAbilityTypeJassDefinition(globalScope));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeOnAddAction", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final JassFunction func = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
					whichAbilityType.setOnAddJass(func);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeOnRemoveAction", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final JassFunction func = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
					whichAbilityType.setOnRemoveJass(func);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeOnTickAction", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final JassFunction func = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
					whichAbilityType.setOnTickJass(func);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeOnDeathAction", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final JassFunction func = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
					whichAbilityType.setOnDeathJass(func);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeOnCancelFromQueueAction",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final JassFunction func = arguments.get(1)
									.visit(JassFunctionJassValueVisitor.getInstance());
							whichAbilityType.setOnCancelFromQueueJass(func);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeOnCheckBeforeQueueQueueCondition",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final TriggerBooleanExpression boolExpr = arguments.get(1)
									.visit(ObjectJassValueVisitor.getInstance());
							whichAbilityType.setCheckBeforeQueueJass(boolExpr);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeCheckTargetCondition",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final TriggerBooleanExpression boolExpr = arguments.get(1)
									.visit(ObjectJassValueVisitor.getInstance());
							whichAbilityType.setCheckTargetCondition(boolExpr);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeCheckUseCondition",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final TriggerBooleanExpression boolExpr = arguments.get(1)
									.visit(ObjectJassValueVisitor.getInstance());
							whichAbilityType.setCheckUseCondition(boolExpr);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeEnabledWhileUnderConstruction",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final boolean enabled = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
							whichAbilityType.setEnabledWhileUnderConstruction(enabled);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeEnabledWhileUpgrading",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final boolean enabled = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
							whichAbilityType.setEnabledWhileUpgrading(enabled);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetAbilityTypeBehavior", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbilityTypeJassDefinition whichAbilityType = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final BehaviorExpr expr = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					whichAbilityType.setBeginJass(expr);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("RegisterAbilityType", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					final CAbilityTypeJassDefinition whichAbilityType = nullable(arguments, 1,
							ObjectJassValueVisitor.getInstance());
					CommonEnvironment.this.simulation.getAbilityData().registerJassType(new War3ID(rawcode),
							whichAbilityType);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetUnitIconUI", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(iconuiType,
							war3MapViewer.getAbilityDataUI().getUnitUI(new War3ID(rawcode)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityOnIconUI", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(iconuiType,
							war3MapViewer.getAbilityDataUI().getUI(new War3ID(rawcode)).getOnIconUI(0));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityOffIconUI", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(iconuiType,
							war3MapViewer.getAbilityDataUI().getUI(new War3ID(rawcode)).getOffIconUI(0));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetAbilityLearnIconUI", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(iconuiType,
							war3MapViewer.getAbilityDataUI().getUI(new War3ID(rawcode)).getLearnIconUI());
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("GetItemIconUI", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final int rawcode = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
					return new HandleJassValue(iconuiType,
							war3MapViewer.getAbilityDataUI().getItemUI(new War3ID(rawcode)));
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("CreateOrderButton", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrderButtonType type = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					final int buttonPositionX = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
					final int buttonPositionY = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
					final JassOrder javaValue = new JassOrder(orderId, buttonPositionX, buttonPositionY);
					javaValue.setType(type);
					return new HandleJassValue(orderbuttonType, javaValue);
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AbilityTypeAddOrderButton", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbilityTypeJassDefinition abilityType = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final JassOrder commandCard = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					abilityType.addJassOrder(commandCard);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("AbilityTypeRemoveOrderButton", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final CAbilityTypeJassDefinition abilityType = arguments.get(0)
							.visit(ObjectJassValueVisitor.getInstance());
					final JassOrder commandCard = arguments.get(1).visit(ObjectJassValueVisitor.getInstance());
					abilityType.removeJassOrder(commandCard);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonAutoCastOrderId", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int autoCastOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderCommandCard.setAutoCastOrderId(autoCastOrderId);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonUnAutoCastOrderId",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final JassOrder orderCommandCard = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final int autoCastUnOrderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
							orderCommandCard.setAutoCastUnOrderId(autoCastUnOrderId);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonContainerMenuOrderId",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final JassOrder orderCommandCard = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final int orderId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
							orderCommandCard.setContainerMenuOrderId(orderId);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonDisabled", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean disabled = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					orderCommandCard.setDisabled(disabled);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonManaCost", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int manaCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderCommandCard.setManaCost(manaCost);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonGoldCost", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int goldCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderCommandCard.setGoldCost(goldCost);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonLumberCost", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int lumberCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderCommandCard.setLumberCost(lumberCost);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonFoodCostDisplayOnly",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final JassOrder orderCommandCard = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final int foodCost = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
							orderCommandCard.setFoodCostDisplayOnly(foodCost);
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonCharges", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int charges = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderCommandCard.setCharges(charges);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonAutoCastActive", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean active = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					orderCommandCard.setAutoCastActive(active);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonHidden", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final boolean hidden = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
					orderCommandCard.setHidden(hidden);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonIconPath", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String iconPath = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					orderCommandCard.setIconPath(iconPath);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonButtonPositionX", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int buttonPosX = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderCommandCard.setButtonPositionX(buttonPosX);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonButtonPositionY", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final int buttonPosY = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
					orderCommandCard.setButtonPositionY(buttonPosY);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonToolTip", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String tip = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					orderCommandCard.setTip(tip);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonUberTip", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String uberTip = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					orderCommandCard.setUberTip(uberTip);
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonHotKey", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final String hotkeyString = arguments.get(1).visit(StringJassValueVisitor.getInstance());
					orderCommandCard.setHotkey(hotkeyString.charAt(0));
					return null;
				}
			});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonPreviewBuildUnitId",
					new JassFunction() {
						@Override
						public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
								final TriggerExecutionScope triggerScope) {
							final JassOrder orderCommandCard = arguments.get(0)
									.visit(ObjectJassValueVisitor.getInstance());
							final int unitId = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
							orderCommandCard.setPreviewBuildUnitId(new War3ID(unitId));
							return null;
						}
					});
			jassProgramVisitor.getJassNativeManager().createNative("SetOrderButtonAOE", new JassFunction() {
				@Override
				public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
						final TriggerExecutionScope triggerScope) {
					final JassOrder orderCommandCard = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
					final double radius = arguments.get(1).visit(RealJassValueVisitor.getInstance());
					orderCommandCard.setMouseTargetRadius((float) radius);
					return null;
				}
			});
		}

		public void config() {
			try {
				this.jassProgramVisitor.getGlobals().getFunctionByName("config").call(Collections.emptyList(),
						this.jassProgramVisitor.getGlobals(), JassProgramVisitor.EMPTY_TRIGGER_SCOPE);
			}
			catch (final Exception exc) {
				throw new JassException(this.jassProgramVisitor.getGlobals(),
						"Exception on Line " + this.jassProgramVisitor.getGlobals().getLineNumber(), exc);
			}
		}

		public void main() {
			final CTimer triggerQueueTimer = new CTimer() {
				@Override
				public void onFire() {
					CommonEnvironment.this.jassProgramVisitor.getGlobals().replayQueuedTriggers();
				}
			};
			triggerQueueTimer.setRepeats(true);
			triggerQueueTimer.setTimeoutTime(0f);
			triggerQueueTimer.start(this.simulation);
			this.simulation.setGlobalScope(this.jassProgramVisitor.getGlobals());
			try {
				this.jassProgramVisitor.getGlobals().getFunctionByName("main").call(Collections.emptyList(),
						this.jassProgramVisitor.getGlobals(), JassProgramVisitor.EMPTY_TRIGGER_SCOPE);
			}
			catch (final Exception exc) {
				throw new JassException(this.jassProgramVisitor.getGlobals(),
						"Exception on Line " + this.jassProgramVisitor.getGlobals().getLineNumber(), exc);
			}
		}
	}

	public static final class ConfigEnvironment {

		private final GameUI gameUI;
		private Element skin;
		private final JassProgramVisitor jassProgramVisitor;

		private ConfigEnvironment(final JassProgramVisitor jassProgramVisitor, final DataSource dataSource,
				final Viewport uiViewport, final Scene uiScene, final GameUI gameUI, final War3MapConfig mapConfig) {
			this.jassProgramVisitor = jassProgramVisitor;
			this.gameUI = gameUI;
			final Rectangle tempRect = new Rectangle();
			final GlobalScope globals = jassProgramVisitor.getGlobals();
			final HandleJassType agentType = globals.registerHandleType("agent");
			final HandleJassType eventType = globals.registerHandleType("event");
			final HandleJassType playerType = globals.registerHandleType("player");
			final HandleJassType widgetType = globals.registerHandleType("widget");
			final HandleJassType unitType = globals.registerHandleType("unit");
			final HandleJassType destructableType = globals.registerHandleType("destructable");
			final HandleJassType itemType = globals.registerHandleType("item");
			final HandleJassType abilityType = globals.registerHandleType("ability");
			final HandleJassType buffType = globals.registerHandleType("buff");
			final HandleJassType forceType = globals.registerHandleType("force");
			final HandleJassType groupType = globals.registerHandleType("group");
			final HandleJassType triggerType = globals.registerHandleType("trigger");
			final HandleJassType triggerconditionType = globals.registerHandleType("triggercondition");
			final HandleJassType triggeractionType = globals.registerHandleType("triggeraction");
			final HandleJassType timerType = globals.registerHandleType("timer");
			final HandleJassType locationType = globals.registerHandleType("location");
			final HandleJassType regionType = globals.registerHandleType("region");
			final HandleJassType rectType = globals.registerHandleType("rect");
			final HandleJassType boolexprType = globals.registerHandleType("boolexpr");
			final HandleJassType soundType = globals.registerHandleType("sound");
			final HandleJassType conditionfuncType = globals.registerHandleType("conditionfunc");
			final HandleJassType filterfuncType = globals.registerHandleType("filterfunc");
			final HandleJassType unitpoolType = globals.registerHandleType("unitpool");
			final HandleJassType itempoolType = globals.registerHandleType("itempool");
			final HandleJassType raceType = globals.registerHandleType("race");
			final HandleJassType alliancetypeType = globals.registerHandleType("alliancetype");
			final HandleJassType racepreferenceType = globals.registerHandleType("racepreference");
			final HandleJassType gamestateType = globals.registerHandleType("gamestate");
			final HandleJassType igamestateType = globals.registerHandleType("igamestate");
			final HandleJassType fgamestateType = globals.registerHandleType("fgamestate");
			final HandleJassType playerstateType = globals.registerHandleType("playerstate");
			final HandleJassType playerscoreType = globals.registerHandleType("playerscore");
			final HandleJassType playergameresultType = globals.registerHandleType("playergameresult");
			final HandleJassType unitstateType = globals.registerHandleType("unitstate");
			final HandleJassType aidifficultyType = globals.registerHandleType("aidifficulty");
			final HandleJassType eventidType = globals.registerHandleType("eventid");
			final HandleJassType gameeventType = globals.registerHandleType("gameevent");
			final HandleJassType playereventType = globals.registerHandleType("playerevent");
			final HandleJassType playeruniteventType = globals.registerHandleType("playerunitevent");
			final HandleJassType uniteventType = globals.registerHandleType("unitevent");
			final HandleJassType limitopType = globals.registerHandleType("limitop");
			final HandleJassType widgeteventType = globals.registerHandleType("widgetevent");
			final HandleJassType dialogeventType = globals.registerHandleType("dialogevent");
			final HandleJassType unittypeType = globals.registerHandleType("unittype");
			final HandleJassType gamespeedType = globals.registerHandleType("gamespeed");
			final HandleJassType gamedifficultyType = globals.registerHandleType("gamedifficulty");
			final HandleJassType gametypeType = globals.registerHandleType("gametype");
			final HandleJassType mapflagType = globals.registerHandleType("mapflag");
			final HandleJassType mapvisibilityType = globals.registerHandleType("mapvisibility");
			final HandleJassType mapsettingType = globals.registerHandleType("mapsetting");
			final HandleJassType mapdensityType = globals.registerHandleType("mapdensity");
			final HandleJassType mapcontrolType = globals.registerHandleType("mapcontrol");
			final HandleJassType playerslotstateType = globals.registerHandleType("playerslotstate");
			final HandleJassType volumegroupType = globals.registerHandleType("volumegroup");
			final HandleJassType camerafieldType = globals.registerHandleType("camerafield");
			final HandleJassType camerasetupType = globals.registerHandleType("camerasetup");
			final HandleJassType playercolorType = globals.registerHandleType("playercolor");
			final HandleJassType placementType = globals.registerHandleType("placement");
			final HandleJassType startlocprioType = globals.registerHandleType("startlocprio");
			final HandleJassType raritycontrolType = globals.registerHandleType("raritycontrol");
			final HandleJassType blendmodeType = globals.registerHandleType("blendmode");
			final HandleJassType texmapflagsType = globals.registerHandleType("texmapflags");
			final HandleJassType effectType = globals.registerHandleType("effect");
			final HandleJassType effecttypeType = globals.registerHandleType("effecttype");
			final HandleJassType weathereffectType = globals.registerHandleType("weathereffect");
			final HandleJassType terraindeformationType = globals.registerHandleType("terraindeformation");
			final HandleJassType fogstateType = globals.registerHandleType("fogstate");
			final HandleJassType fogmodifierType = globals.registerHandleType("fogmodifier");
			final HandleJassType dialogType = globals.registerHandleType("dialog");
			final HandleJassType buttonType = globals.registerHandleType("button");
			final HandleJassType questType = globals.registerHandleType("quest");
			final HandleJassType questitemType = globals.registerHandleType("questitem");
			final HandleJassType defeatconditionType = globals.registerHandleType("defeatcondition");
			final HandleJassType timerdialogType = globals.registerHandleType("timerdialog");
			final HandleJassType leaderboardType = globals.registerHandleType("leaderboard");
			final HandleJassType multiboardType = globals.registerHandleType("multiboard");
			final HandleJassType multiboarditemType = globals.registerHandleType("multiboarditem");
			final HandleJassType trackableType = globals.registerHandleType("trackable");
			final HandleJassType gamecacheType = globals.registerHandleType("gamecache");
			final HandleJassType versionType = globals.registerHandleType("version");
			final HandleJassType itemtypeType = globals.registerHandleType("itemtype");
			final HandleJassType texttagType = globals.registerHandleType("texttag");
			final HandleJassType attacktypeType = globals.registerHandleType("attacktype");
			final HandleJassType damagetypeType = globals.registerHandleType("damagetype");
			final HandleJassType weapontypeType = globals.registerHandleType("weapontype");
			final HandleJassType soundtypeType = globals.registerHandleType("soundtype");
			final HandleJassType lightningType = globals.registerHandleType("lightning");
			final HandleJassType pathingtypeType = globals.registerHandleType("pathingtype");
			final HandleJassType imageType = globals.registerHandleType("image");
			final HandleJassType ubersplatType = globals.registerHandleType("ubersplat");
			final HandleJassType hashtableType = globals.registerHandleType("hashtable");
			final HandleJassType frameHandleType = globals.registerHandleType("framehandle");

			// Warsmash Ability API
			final HandleJassType abilitytypeType = globals.registerHandleType("abilitytype");
			final HandleJassType ordercommandcardType = globals.registerHandleType("ordercommandcard");
			final HandleJassType ordercommandcardtypeType = globals.registerHandleType("ordercommandcardtype");
			final HandleJassType abilitybehaviorType = globals.registerHandleType("abilitybehavior");
			final HandleJassType behaviorexprType = globals.registerHandleType("behaviorexpr");
			final HandleJassType iconuiType = globals.registerHandleType("iconui");

			registerTypingNatives(jassProgramVisitor, raceType, alliancetypeType, racepreferenceType, igamestateType,
					fgamestateType, playerstateType, playerscoreType, playergameresultType, unitstateType,
					aidifficultyType, gameeventType, playereventType, playeruniteventType, uniteventType, limitopType,
					widgeteventType, dialogeventType, unittypeType, gamespeedType, gamedifficultyType, gametypeType,
					mapflagType, mapvisibilityType, mapsettingType, mapdensityType, mapcontrolType, playerslotstateType,
					volumegroupType, camerafieldType, playercolorType, placementType, startlocprioType,
					raritycontrolType, blendmodeType, texmapflagsType, effecttypeType, fogstateType, versionType,
					itemtypeType, attacktypeType, damagetypeType, weapontypeType, soundtypeType, pathingtypeType);
			registerConversionAndStringNatives(jassProgramVisitor, gameUI);
			registerConfigNatives(jassProgramVisitor, mapConfig, startlocprioType, gametypeType, placementType,
					gamespeedType, gamedifficultyType, mapdensityType, locationType, playerType, playercolorType,
					mapcontrolType, playerslotstateType, mapConfig);

		}

		public void config() {
			try {
				this.jassProgramVisitor.getGlobals().getFunctionByName("config").call(Collections.emptyList(),
						this.jassProgramVisitor.getGlobals(), JassProgramVisitor.EMPTY_TRIGGER_SCOPE);
			}
			catch (final Exception exc) {
				throw new JassException(this.jassProgramVisitor.getGlobals(),
						"Exception on Line " + this.jassProgramVisitor.getGlobals().getLineNumber(), exc);
			}
		}

	}

	private static void setupTriggerAPI(final JassProgramVisitor jassProgramVisitor, final HandleJassType triggerType,
			final HandleJassType triggeractionType, final HandleJassType triggerconditionType,
			final HandleJassType boolexprType, final HandleJassType conditionfuncType,
			final HandleJassType filterfuncType, final HandleJassType eventidType) {
		// ============================================================================
		// Native trigger interface
		//
		jassProgramVisitor.getJassNativeManager().createNative("CreateTrigger", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(triggerType, new Trigger());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyTrigger", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				trigger.destroy();
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ResetTrigger", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				trigger.reset();
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("EnableTrigger", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				trigger.setEnabled(true);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DisableTrigger", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				trigger.setEnabled(false);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("IsTriggerEnabled", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				return BooleanJassValue.of(trigger.isEnabled());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerWaitOnSleeps", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
				trigger.setWaitOnSleeps(value.booleanValue());
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("IsTriggerWaitOnSleeps", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				return BooleanJassValue.of(trigger.isWaitOnSleeps());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggeringTrigger", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(triggerType, triggerScope.getTriggeringTrigger());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggerEventId", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(eventidType,
						((CommonTriggerExecutionScope) triggerScope).getTriggerEventId());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggerEvalCount", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				return new IntegerJassValue(trigger.getEvalCount());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetTriggerExecCount", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger trigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				return new IntegerJassValue(trigger.getExecCount());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ExecuteFunc", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String funcName = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				final JassFunction functionByName = globalScope.getFunctionByName(funcName);
				System.out.println("ExecuteFunc (\"" + funcName + "\")");
				if (functionByName != null) {
					// TODO below TriggerExecutionScope.EMPTY is probably not correct
					functionByName.call(Collections.emptyList(), globalScope, TriggerExecutionScope.EMPTY);
				}
				return null;
			}
		});

		// ============================================================================
		// Boolean Expr API ( for compositing trigger conditions and unit filter
		// funcs...)
		// ============================================================================
		jassProgramVisitor.getJassNativeManager().createNative("And", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final TriggerBooleanExpression operandA = arguments.get(0)
						.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
				final TriggerBooleanExpression operandB = arguments.get(1)
						.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
				return new HandleJassValue(boolexprType, new BoolExprAnd(operandA, operandB));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Or", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final TriggerBooleanExpression operandA = arguments.get(0)
						.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
				final TriggerBooleanExpression operandB = arguments.get(1)
						.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
				return new HandleJassValue(boolexprType, new BoolExprOr(operandA, operandB));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Not", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final TriggerBooleanExpression operand = arguments.get(0)
						.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
				return new HandleJassValue(boolexprType, new BoolExprNot(operand));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Condition", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final JassFunction func = arguments.get(0).visit(JassFunctionJassValueVisitor.getInstance());
				return new HandleJassValue(conditionfuncType, new BoolExprCondition(func));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyCondition", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final BoolExprCondition condition = arguments.get(0)
						.visit(ObjectJassValueVisitor.<BoolExprCondition>getInstance());
				System.err.println(
						"DestroyCondition called but in Java we don't have a destructor, so we need to unregister later when that is implemented");
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Filter", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final JassFunction func = arguments.get(0).visit(JassFunctionJassValueVisitor.getInstance());
				return new HandleJassValue(filterfuncType, new BoolExprFilter(func));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyFilter", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final BoolExprFilter filter = arguments.get(0)
						.visit(ObjectJassValueVisitor.<BoolExprFilter>getInstance());
				System.err.println(
						"DestroyFilter called but in Java we don't have a destructor, so we need to unregister later when that is implemented");
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DestroyBoolExpr", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final TriggerBooleanExpression boolexpr = arguments.get(0)
						.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
				System.err.println(
						"DestroyBoolExpr called but in Java we don't have a destructor, so we need to unregister later when that is implemented");
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerAddCondition", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				final TriggerBooleanExpression condition = arguments.get(1)
						.visit(ObjectJassValueVisitor.<TriggerBooleanExpression>getInstance());
				final int index = whichTrigger.addCondition(condition);
				return new HandleJassValue(triggerconditionType, new TriggerCondition(condition, whichTrigger, index));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerRemoveCondition", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				final TriggerCondition condition = arguments.get(1)
						.visit(ObjectJassValueVisitor.<TriggerCondition>getInstance());
				if (condition.getTrigger() != whichTrigger) {
					throw new IllegalArgumentException("Unable to remove condition, wrong trigger");
				}
				whichTrigger.removeConditionAtIndex(condition.getConditionIndex());
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerClearConditions", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				whichTrigger.clearConditions();
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("TriggerAddAction", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Trigger whichTrigger = arguments.get(0).visit(ObjectJassValueVisitor.<Trigger>getInstance());
				final JassFunction actionFunc = arguments.get(1).visit(JassFunctionJassValueVisitor.getInstance());
				final int actionIndex = whichTrigger.addAction(actionFunc);
				return new HandleJassValue(triggeractionType, new TriggerAction(whichTrigger, actionFunc, actionIndex));
			}
		});
	}

	private static <T> T nullable(final List<JassValue> arguments, final int index,
			final ObjectJassValueVisitor<T> visitor) {
		final JassValue arg = arguments.get(index);
		if (arg == null) {
			return null;
		}
		return arg.visit(visitor);
	}

	private static JassFunction nullable(final List<JassValue> arguments, final int index,
			final JassFunctionJassValueVisitor visitor) {
		final JassValue arg = arguments.get(index);
		if (arg == null) {
			return null;
		}
		return arg.visit(visitor);
	}

	private static void doPreloadScript(final DataSource dataSource, final War3MapViewer war3MapViewer,
			final String filename) {
		final JassProgramVisitor jassProgramVisitor = new JassProgramVisitor();
		jassProgramVisitor.getJassNativeManager().createNative("Preload", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String filename = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				war3MapViewer.load(filename, war3MapViewer.mapPathSolver, war3MapViewer.solverParams);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("PreloadEnd", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final float timeout = arguments.get(1).visit(RealJassValueVisitor.getInstance()).floatValue();
				return null;
			}
		});
		try {
			JassLexer lexer;
			try {
				lexer = new JassLexer(CharStreams.fromStream(dataSource.getResourceAsStream(filename)));
			}
			catch (final IOException e) {
				throw new RuntimeException(e);
			}
			final JassParser parser = new JassParser(new CommonTokenStream(lexer));
//							parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
			parser.addErrorListener(new BaseErrorListener() {
				@Override
				public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
						final int charPositionInLine, final String msg, final RecognitionException e) {
					if (!REPORT_SYNTAX_ERRORS) {
						return;
					}

					final String sourceName = String.format("%s:%d:%d: ", filename, line, charPositionInLine);

					System.err.println(sourceName + "line " + line + ":" + charPositionInLine + " " + msg);
					throw new IllegalStateException(sourceName + "line " + line + ":" + charPositionInLine + " " + msg);
				}
			});
			jassProgramVisitor.visit(parser.program());
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static final class SaveHashtableValueFunc implements JassFunction {
		@Override
		public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
				final TriggerExecutionScope triggerScope) {
			final CHashtable table = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
			final Integer parentKey = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			final Integer childKey = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			table.save(parentKey, childKey, arguments.get(3));
			return null;
		}
	}

	private static final class LoadHashtableValueFunc implements JassFunction {
		private final JassValue nullValue;

		public LoadHashtableValueFunc(final JassValue nullValue) {
			this.nullValue = nullValue;
		}

		@Override
		public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
				final TriggerExecutionScope triggerScope) {
			final CHashtable table = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
			final Integer parentKey = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			final Integer childKey = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			final Object loadedValue = table.load(parentKey, childKey);
			if (loadedValue == null) {
				return this.nullValue;
			}
			return (JassValue) loadedValue;
		}
	}

	private static final class HaveSavedHashtableValueFunc implements JassFunction {
		private final JassType jassType;

		public HaveSavedHashtableValueFunc(final JassType jassType) {
			this.jassType = jassType;
		}

		@Override
		public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
				final TriggerExecutionScope triggerScope) {
			final CHashtable table = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
			final Integer parentKey = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
			final Integer childKey = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
			final Object loadedValue = table.load(parentKey, childKey);
			if (loadedValue != null) {
				if (loadedValue instanceof JassValue) {
					final JassValue loadedJassValue = (JassValue) loadedValue;
					final JassType type = loadedJassValue.visit(JassTypeGettingValueVisitor.getInstance());
					if (this.jassType.isAssignableFrom(type)) {
						return BooleanJassValue.TRUE;
					}
				}
			}
			return BooleanJassValue.FALSE;
		}
	}

	private static void registerConfigNatives(final JassProgramVisitor jassProgramVisitor,
			final War3MapConfig mapConfig, final HandleJassType startlocprioType, final HandleJassType gametypeType,
			final HandleJassType placementType, final HandleJassType gamespeedType,
			final HandleJassType gamedifficultyType, final HandleJassType mapdensityType,
			final HandleJassType locationType, final HandleJassType playerType, final HandleJassType playercolorType,
			final HandleJassType mapcontrolType, final HandleJassType playerslotstateType, final CPlayerAPI playerAPI) {
		jassProgramVisitor.getJassNativeManager().createNative("SetMapName", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String name = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				mapConfig.setMapName(name);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetMapDescription", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String name = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				mapConfig.setMapDescription(name);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetTeams", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer teamCount = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				mapConfig.setTeamCount(teamCount);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayers", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer playerCount = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				mapConfig.setPlayerCount(playerCount);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DefineStartLocation", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final Double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
				final Double y = arguments.get(2).visit(RealJassValueVisitor.getInstance());
				mapConfig.getStartLoc(whichStartLoc).setX(x.floatValue());
				mapConfig.getStartLoc(whichStartLoc).setY(y.floatValue());
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("DefineStartLocationLoc", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final Point2D.Double whichLocation = arguments.get(1)
						.visit(ObjectJassValueVisitor.<Point2D.Double>getInstance());
				mapConfig.getStartLoc(whichStartLoc).setX((float) whichLocation.x);
				mapConfig.getStartLoc(whichStartLoc).setY((float) whichLocation.y);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetStartLocPrioCount", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final Integer prioSlotCount = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				mapConfig.getStartLoc(whichStartLoc).setStartLocPrioCount(prioSlotCount);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetStartLocPrio", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final Integer prioSlotIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final Integer otherStartLocIndex = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
				final CStartLocPrio priority = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CStartLocPrio>getInstance());
				mapConfig.getStartLoc(whichStartLoc).setStartLocPrio(prioSlotIndex, otherStartLocIndex, priority);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocPrioSlot", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final Integer prioSlotIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				return new IntegerJassValue(mapConfig.getStartLoc(whichStartLoc).getOtherStartIndices()[prioSlotIndex]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocPrio", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				final Integer prioSlotIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(startlocprioType,
						mapConfig.getStartLoc(whichStartLoc).getOtherStartLocPriorities()[prioSlotIndex]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetGameTypeSupported", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CGameType gameType = arguments.get(0).visit(ObjectJassValueVisitor.<CGameType>getInstance());
				final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
				mapConfig.setGameTypeSupported(gameType, value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetMapFlag", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CMapFlag mapFlag = arguments.get(0).visit(ObjectJassValueVisitor.<CMapFlag>getInstance());
				final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
				mapConfig.setMapFlag(mapFlag, value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetGamePlacement", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CMapPlacement placement = arguments.get(0)
						.visit(ObjectJassValueVisitor.<CMapPlacement>getInstance());
				mapConfig.setPlacement(placement);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetGameSpeed", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CGameSpeed gameSpeed = arguments.get(0).visit(ObjectJassValueVisitor.<CGameSpeed>getInstance());
				mapConfig.setGameSpeed(gameSpeed);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetGameDifficulty", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CMapDifficulty gameDifficulty = arguments.get(0)
						.visit(ObjectJassValueVisitor.<CMapDifficulty>getInstance());
				mapConfig.setGameDifficulty(gameDifficulty);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetResourceDensity", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CMapDensity resourceDensity = arguments.get(0)
						.visit(ObjectJassValueVisitor.<CMapDensity>getInstance());
				mapConfig.setResourceDensity(resourceDensity);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetCreatureDensity", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CMapDensity creatureDensity = arguments.get(0)
						.visit(ObjectJassValueVisitor.<CMapDensity>getInstance());
				mapConfig.setCreatureDensity(creatureDensity);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetTeams", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(mapConfig.getTeamCount());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayers", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(mapConfig.getPlayerCount());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("IsGameTypeSupported", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CGameType gameType = arguments.get(0).visit(ObjectJassValueVisitor.<CGameType>getInstance());
				return BooleanJassValue.of(mapConfig.isGameTypeSupported(gameType));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameTypeSelected", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(gametypeType, mapConfig.getGameTypeSelected());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("IsMapFlagSet", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CMapFlag mapFlag = arguments.get(0).visit(ObjectJassValueVisitor.<CMapFlag>getInstance());
				return BooleanJassValue.of(mapConfig.isMapFlagSet(mapFlag));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetGamePlacement", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(placementType, mapConfig.getPlacement());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameSpeed", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(gamespeedType, mapConfig.getGameSpeed());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetGameDifficulty", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(gamedifficultyType, mapConfig.getGameDifficulty());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetResourceDensity", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(mapdensityType, mapConfig.getResourceDensity());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetCreatureDensity", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(mapdensityType, mapConfig.getCreatureDensity());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocationX", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new RealJassValue(mapConfig.getStartLoc(whichStartLoc).getX());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocationY", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new RealJassValue(mapConfig.getStartLoc(whichStartLoc).getY());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetStartLocationLoc", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer whichStartLoc = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(locationType, new Point2D.Double(mapConfig.getStartLoc(whichStartLoc).getX(),
						mapConfig.getStartLoc(whichStartLoc).getY()));
			}
		});
		// PlayerAPI

		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTeam", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final Integer whichTeam = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				player.setTeam(whichTeam);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerStartLocation", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final Integer startLocIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				player.setStartLocationIndex(startLocIndex);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ForcePlayerStartLocation", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final Integer startLocIndex = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				player.forceStartLocation(startLocIndex);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerColor", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CPlayerColor playerColor = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CPlayerColor>getInstance());
				player.setColor(playerColor.ordinal());
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerAlliance", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CPlayerJass otherPlayer = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CAllianceType whichAllianceSetting = arguments.get(2)
						.visit(ObjectJassValueVisitor.<CAllianceType>getInstance());
				final Boolean value = arguments.get(3).visit(BooleanJassValueVisitor.getInstance());
				player.setAlliance(otherPlayer.getId(), whichAllianceSetting, value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerAlliance", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CPlayerJass otherPlayer = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CAllianceType whichAllianceSetting = arguments.get(2)
						.visit(ObjectJassValueVisitor.<CAllianceType>getInstance());
				return BooleanJassValue.of(player.hasAlliance(otherPlayer.getId(), whichAllianceSetting));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerTaxRate", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CPlayerJass otherPlayer = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CPlayerState whichResource = arguments.get(2)
						.visit(ObjectJassValueVisitor.<CPlayerState>getInstance());
				final int taxRate = arguments.get(3).visit(IntegerJassValueVisitor.getInstance());
				player.setTaxRate(otherPlayer.getId(), whichResource, taxRate);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerRacePreference", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CRacePreference whichRacePreference = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CRacePreference>getInstance());
				player.setRacePref(whichRacePreference);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerRaceSelectable", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
				player.setRaceSelectable(value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerController", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CMapControl controlType = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CMapControl>getInstance());
				player.setController(controlType);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerName", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final String name = arguments.get(1).visit(StringJassValueVisitor.getInstance());
				player.setName(name);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SetPlayerOnScoreScreen", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final Boolean value = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
				player.setOnScoreScreen(value);
				return null;
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTeam", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				return new IntegerJassValue(player.getTeam());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerStartLocation", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				return new IntegerJassValue(player.getStartLocationIndex());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerColor", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				return new HandleJassValue(playercolorType, CPlayerColor.getColorByIndex(player.getColor()));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerSelectable", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				return BooleanJassValue.of(player.isRaceSelectable());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerController", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				return new HandleJassValue(mapcontrolType, player.getController());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerSlotState", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				return new HandleJassValue(playerslotstateType, player.getSlotState());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerTaxRate", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CPlayerJass otherPlayer = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CPlayerState whichResource = arguments.get(2)
						.visit(ObjectJassValueVisitor.<CPlayerState>getInstance());
				return new IntegerJassValue(player.getTaxRate(otherPlayer.getId(), whichResource));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("IsPlayerRacePrefSet", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				final CRacePreference racePref = arguments.get(1)
						.visit(ObjectJassValueVisitor.<CRacePreference>getInstance());
				return BooleanJassValue.of(player.isRacePrefSet(racePref));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerName", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass player = arguments.get(0).visit(ObjectJassValueVisitor.<CPlayerJass>getInstance());
				return new StringJassValue(player.getName());
			}
		});

		jassProgramVisitor.getJassNativeManager().createNative("Player", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int playerIndex = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(playerType, playerAPI.getPlayer(playerIndex));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerId", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final CPlayerJass whichPlayer = arguments.get(0).visit(ObjectJassValueVisitor.getInstance());
				if (whichPlayer == null) {
					return new IntegerJassValue(-1);
				}
				return new IntegerJassValue(whichPlayer.getId());
			}
		});

		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerNeutralAggressive", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(WarsmashConstants.MAX_PLAYERS - 4);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJPlayerNeutralVictim", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(WarsmashConstants.MAX_PLAYERS - 3);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJPlayerNeutralExtra", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(WarsmashConstants.MAX_PLAYERS - 2);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetPlayerNeutralPassive", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(WarsmashConstants.MAX_PLAYERS - 1);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJMaxPlayers", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(WarsmashConstants.MAX_PLAYERS - 4);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetBJMaxPlayerSlots", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new IntegerJassValue(WarsmashConstants.MAX_PLAYERS);
			}
		});
	}

	public static void registerTypingNatives(final JassProgramVisitor jassProgramVisitor, final HandleJassType raceType,
			final HandleJassType alliancetypeType, final HandleJassType racepreferenceType,
			final HandleJassType igamestateType, final HandleJassType fgamestateType,
			final HandleJassType playerstateType, final HandleJassType playerscoreType,
			final HandleJassType playergameresultType, final HandleJassType unitstateType,
			final HandleJassType aidifficultyType, final HandleJassType gameeventType,
			final HandleJassType playereventType, final HandleJassType playeruniteventType,
			final HandleJassType uniteventType, final HandleJassType limitopType, final HandleJassType widgeteventType,
			final HandleJassType dialogeventType, final HandleJassType unittypeType, final HandleJassType gamespeedType,
			final HandleJassType gamedifficultyType, final HandleJassType gametypeType,
			final HandleJassType mapflagType, final HandleJassType mapvisibilityType,
			final HandleJassType mapsettingType, final HandleJassType mapdensityType,
			final HandleJassType mapcontrolType, final HandleJassType playerslotstateType,
			final HandleJassType volumegroupType, final HandleJassType camerafieldType,
			final HandleJassType playercolorType, final HandleJassType placementType,
			final HandleJassType startlocprioType, final HandleJassType raritycontrolType,
			final HandleJassType blendmodeType, final HandleJassType texmapflagsType,
			final HandleJassType effecttypeType, final HandleJassType fogstateType, final HandleJassType versionType,
			final HandleJassType itemtypeType, final HandleJassType attacktypeType, final HandleJassType damagetypeType,
			final HandleJassType weapontypeType, final HandleJassType soundtypeType,
			final HandleJassType pathingtypeType) {
		jassProgramVisitor.getJassNativeManager().createNative("ConvertRace", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				CRace race = WarsmashConstants.RACE_MANAGER.getRace(i);
				if (race == null) {
					race = new CRace(i); // Give them a placeholder!
				}
				return new HandleJassValue(raceType, race);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertAllianceType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(alliancetypeType, CAllianceType.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertRacePref", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				CRacePreference racePreference = WarsmashConstants.RACE_MANAGER.getRacePreference(i);
				if (racePreference == null) {
					racePreference = new CRacePreference(i); // Give them a placeholder!
				}
				return new HandleJassValue(racepreferenceType, racePreference);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertIGameState", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(igamestateType, CGameState.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertFGameState", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(fgamestateType, CGameState.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerState", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(playerstateType, CPlayerState.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerScore", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(playerscoreType, CPlayerScore.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerGameResult", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(playergameresultType, CPlayerGameResult.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertUnitState", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(unitstateType, CUnitState.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertAIDifficulty", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(aidifficultyType, AIDifficulty.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameEvent", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(gameeventType, JassGameEventsWar3.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerEvent", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(playereventType, JassGameEventsWar3.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerUnitEvent", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(playeruniteventType, JassGameEventsWar3.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertWidgetEvent", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(widgeteventType, JassGameEventsWar3.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertDialogEvent", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(dialogeventType, JassGameEventsWar3.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertUnitEvent", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(uniteventType, JassGameEventsWar3.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertLimitOp", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(limitopType, CLimitOp.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertUnitType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(unittypeType, CUnitTypeJass.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameSpeed", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(gamespeedType, CGameSpeed.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlacement", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(placementType, CMapPlacement.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertStartLocPrio", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(startlocprioType, CStartLocPrio.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameDifficulty", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(gamedifficultyType, CMapDifficulty.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertGameType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(gametypeType, CGameType.getById(i));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapFlag", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(mapflagType, CMapFlag.getById(i));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapVisibility", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(mapvisibilityType, null);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapSetting", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				return new HandleJassValue(mapsettingType, null);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapDensity", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(mapdensityType, CMapDensity.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertMapControl", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(mapcontrolType, CMapControl.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerColor", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				if (i >= CPlayerColor.VALUES.length) {
					return playercolorType.getNullValue();
				}
				return new HandleJassValue(playercolorType, CPlayerColor.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPlayerSlotState", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(playerslotstateType, CPlayerSlotState.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertVolumeGroup", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(volumegroupType, CSoundVolumeGroup.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertCameraField", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(camerafieldType, CCameraField.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertBlendMode", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(blendmodeType, CBlendMode.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertRarityControl", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(raritycontrolType, CRarityControl.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertTexMapFlags", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(texmapflagsType, CTexMapFlags.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertFogState", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(fogstateType, CFogState.getById(i));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertEffectType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(effecttypeType, CEffectType.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertVersion", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(versionType, CVersion.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertItemType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(itemtypeType, CItemTypeJass.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertAttackType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(attacktypeType, CAttackTypeJass.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertDamageType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(damagetypeType, CDamageType.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertWeaponType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(weapontypeType, CWeaponSoundTypeJass.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertSoundType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(soundtypeType, CSoundType.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("ConvertPathingType", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final int i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new HandleJassValue(pathingtypeType, CPathingTypeJass.VALUES[i]);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("OrderId", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String idString = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				final int orderId = OrderIdUtils.getOrderId(idString);
				return new IntegerJassValue(orderId);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("OrderId2String", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer id = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new StringJassValue(OrderIdUtils.getStringFromOrderId(id));
			}
		});
	}

	public static void registerConversionAndStringNatives(final JassProgramVisitor jassProgramVisitor,
			final GameUI gameUI) {
		jassProgramVisitor.getJassNativeManager().createNative("Deg2Rad", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				return new RealJassValue(StrictMath.toRadians(value));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Rad2Deg", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				return new RealJassValue(StrictMath.toDegrees(value));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Sin", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				return new RealJassValue(StrictMath.sin(value));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Cos", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				return new RealJassValue(StrictMath.cos(value));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Tan", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				return new RealJassValue(StrictMath.tan(value));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Asin", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final double result = StrictMath.asin(value);
				if (Double.isNaN(result)) {
					return new RealJassValue(0);
				}
				return new RealJassValue(result);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Acos", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final double result = StrictMath.acos(value);
				if (Double.isNaN(result)) {
					return new RealJassValue(0);
				}
				return new RealJassValue(result);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Atan", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final double result = StrictMath.atan(value);
				if (Double.isNaN(result)) {
					return new RealJassValue(0);
				}
				return new RealJassValue(result);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Atan2", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double y = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final Double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
				final double result = StrictMath.atan2(y, x);
				if (Double.isNaN(result)) {
					return new RealJassValue(0);
				}
				return new RealJassValue(result);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SquareRoot", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double value = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final double result = StrictMath.sqrt(value);
				return new RealJassValue(result);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("Pow", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double y = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final Double x = arguments.get(1).visit(RealJassValueVisitor.getInstance());
				final double result = StrictMath.pow(y, x);
				if (Double.isNaN(result)) {
					return new RealJassValue(0);
				}
				return new RealJassValue(result);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("I2R", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new RealJassValue(i.doubleValue());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("R2I", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double r = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				return new IntegerJassValue(r.intValue());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("I2S", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Integer i = arguments.get(0).visit(IntegerJassValueVisitor.getInstance());
				return new StringJassValue(i.toString());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("R2S", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double r = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				return new StringJassValue(r.toString());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("R2SW", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final Double r = arguments.get(0).visit(RealJassValueVisitor.getInstance());
				final int width = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				final int precision = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
				return new StringJassValue(String.format("%" + precision + "." + width + "f", r));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("S2I", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				try {
					final int intValue = Integer.parseInt(s);
					return new IntegerJassValue(intValue);
				}
				catch (final Exception exc) {
					return new IntegerJassValue(0);
				}
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("S2R", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				try {
					final double parsedValue = Double.parseDouble(s);
					return new RealJassValue(parsedValue);
				}
				catch (final Exception exc) {
					return new RealJassValue(0);
				}
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("SubString", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				int start = arguments.get(1).visit(IntegerJassValueVisitor.getInstance());
				int end = arguments.get(2).visit(IntegerJassValueVisitor.getInstance());
				if (start > s.length()) {
					start = s.length();
				}
				if (start < 0) {
					start = 0;
				}
				if (end > s.length()) {
					end = s.length();
				}
				if (end < start) {
					end = start;
				}
				return new StringJassValue(s.substring(start, end));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("StringLength", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				return new IntegerJassValue(s.length());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("StringCase", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				final boolean upper = arguments.get(1).visit(BooleanJassValueVisitor.getInstance());
				return new StringJassValue(upper ? s.toUpperCase(Locale.US) : s.toLowerCase(Locale.US));
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("StringHash", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String s = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				return new IntegerJassValue(s.hashCode());
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalizedString", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String key = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				// TODO this might be wrong, or a subset of the needed return values
				final String decoratedString = gameUI.getTemplates().getDecoratedString(key);
				if (key.equals(decoratedString)) {
					System.err.println("GetLocalizedString: NOT FOUND: " + key);
				}
				return new StringJassValue(decoratedString);
			}
		});
		jassProgramVisitor.getJassNativeManager().createNative("GetLocalizedHotkey", new JassFunction() {
			@Override
			public JassValue call(final List<JassValue> arguments, final GlobalScope globalScope,
					final TriggerExecutionScope triggerScope) {
				final String key = arguments.get(0).visit(StringJassValueVisitor.getInstance());
				// TODO this might be wrong, or a subset of the needed return values
				final String decoratedString = gameUI.getTemplates().getDecoratedString(key);
				if (key.equals(decoratedString)) {
					System.err.println("GetLocalizedHotkey: NOT FOUND: " + key);
				}
				return new IntegerJassValue(decoratedString.charAt(0));
			}
		});
	}
}
