/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.topteams;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.team.Team;
import app.owlcms.data.team.TeamResultsTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.video.VideoCSSOverride;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.HasBoardMode;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class TopTeamsSinclair
 *
 * Show best sinclair team scores
 *
 * @author Jean-François Lamu
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("topteamsinclair-template")
@JsModule("./components/TopTeamsSinclair.js")
@Route("displays/topteamsinclair")

public class TopTeamsSinclair extends LitTemplate
        implements DisplayParameters,
        SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireDisplayLogin, VideoCSSOverride, HasBoardMode {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TopTeamsSinclair.class);
	private static final int SHOWN_ON_BOARD = 5;
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private AgeDivision ageDivision = null;
	private String ageGroupPrefix = null;
	private boolean darkMode;
	private Dialog dialog;
	private DecimalFormat floatFormat;
	private boolean initializationNeeded;
	private Location location;
	private UI locationUI;
	private List<TeamTreeItem> mensTeams;
	private EventBus uiEventBus;
	private List<TeamTreeItem> womensTeams;
	private Timer dialogTimer;
	private boolean video;

	public boolean isVideo() {
		return video;
	}

	public void setVideo(boolean video) {
		this.video = video;
	}

	private String routeParameter;

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	/**
	 * Instantiates a new results board.
	 */
	public TopTeamsSinclair() {
		OwlcmsFactory.waitDBInitialized();
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		// logger.debug("addDialogContent ad={} ag={} darkMode={}", getAgeDivision(),
		// getAgeGroupPrefix(),
		// isDarkMode());

		DisplayOptions.addLightingEntries(vl, target, this);
		ComboBox<AgeDivision> ageDivisionComboBox = new ComboBox<>();
		ComboBox<String> ageGroupPrefixComboBox = new ComboBox<>();
		List<AgeDivision> ageDivisions = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
		ageDivisionComboBox.setItems(ageDivisions);
		ageDivisionComboBox.setPlaceholder(getTranslation("AgeDivision"));
		ageDivisionComboBox.setClearButtonVisible(true);
		ageDivisionComboBox.addValueChangeListener(e -> {
			AgeDivision ageDivision = e.getValue();
			setAgeDivision(ageDivision);
			String existingAgeGroupPrefix = getAgeGroupPrefix();
			List<String> activeAgeGroups = setAgeGroupPrefixItems(ageGroupPrefixComboBox, ageDivision);
			if (existingAgeGroupPrefix != null) {
				ageGroupPrefixComboBox.setValue(existingAgeGroupPrefix);
			} else if (activeAgeGroups != null && !activeAgeGroups.isEmpty() && ageDivision != AgeDivision.MASTERS) {
				ageGroupPrefixComboBox.setValue(activeAgeGroups.get(0));
			}
		});
		ageGroupPrefixComboBox.setPlaceholder(getTranslation("AgeGroup"));
		ageGroupPrefixComboBox.setClearButtonVisible(true);
		ageGroupPrefixComboBox.addValueChangeListener(e -> {
			setAgeGroupPrefix(e.getValue());
			updateURLLocations();
			doUpdate(Competition.getCurrent());
		});
		setAgeGroupPrefixItems(ageGroupPrefixComboBox, getAgeDivision());
		ageGroupPrefixComboBox.setValue(getAgeGroupPrefix());
		ageDivisionComboBox.setValue(getAgeDivision());

		vl.add(new NativeLabel(getTranslation("SelectAgeGroup")),
		        new HorizontalLayout(ageDivisionComboBox, ageGroupPrefixComboBox));
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			// just update the display
			setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), getElement());
			doUpdate(fop.getCurAthlete(), null);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		doBreak(e);
	}

	public void doUpdate(Competition competition) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), getElement());

		TeamResultsTreeData teamResultsTreeData = new TeamResultsTreeData(getAgeGroupPrefix(), getAgeDivision(), (Gender) null,
		        Ranking.BW_SINCLAIR, true);
		Map<Gender, List<TeamTreeItem>> teamsByGender = teamResultsTreeData.getTeamItemsByGender();

		mensTeams = teamsByGender.get(Gender.M);
		if (mensTeams != null) {
			mensTeams.sort(TeamTreeItem.sinclairScoreComparator);
		}
		mensTeams = topN(mensTeams);

		womensTeams = teamsByGender.get(Gender.F);
		if (womensTeams != null) {
			womensTeams.sort(TeamTreeItem.sinclairScoreComparator);
		}
		womensTeams = topN(womensTeams);

		updateBottom();
	}

	/**
	 * return dialog, but only on first call.
	 *
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#getDialog()
	 */
	@Override
	public Dialog getDialog() {
		return dialog;
	}

	@Override
	public Timer getDialogTimer() {
		return this.dialogTimer;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#getLocation()
	 */
	@Override
	public Location getLocation() {
		return this.location;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#getLocationUI()
	 */
	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.TopTeamsSinclair");
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isDarkMode()
	 */
	@Override
	public boolean isDarkMode() {
		return this.darkMode;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#isIgnoreFopFromURL()
	 */
	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#isIgnoreGroupFromURL()
	 */
	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
	 */
	@Override
	public boolean isShowInitialDialog() {
		return this.initializationNeeded;
	}

	@Override
	public boolean isSilenced() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#readParams(com.vaadin.flow.router.Location, java.util.Map)
	 */
	@Override
	public HashMap<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		HashMap<String, List<String>> params1 = new HashMap<>(parametersMap);

		List<String> darkParams = params1.get(DARK);
		// dark is the default. dark=false or dark=no or ... will turn off dark mode.
		boolean darkMode = darkParams == null || darkParams.isEmpty() || darkParams.get(0).toLowerCase().equals("true");
		setDarkMode(darkMode);
		updateParam(params1, DARK, !isDarkMode() ? "false" : null);

		List<String> silentParams = params1.get(SILENT);
		// dark is the default. dark=false or dark=no or ... will turn off dark mode.
		boolean silentMode = silentParams == null || silentParams.isEmpty()
		        || silentParams.get(0).toLowerCase().equals("true");
		setSilenced(silentMode);
		updateParam(params1, SILENT, !isSilenced() ? "false" : null);

		List<String> ageDivisionParams = params1.get("ad");
		// no age division
		String ageDivisionName = (ageDivisionParams != null && !ageDivisionParams.isEmpty() ? ageDivisionParams.get(0)
		        : null);
		try {
			setAgeDivision(AgeDivision.valueOf(ageDivisionName));
		} catch (Exception e) {
			List<AgeDivision> ageDivisions = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
			setAgeDivision((ageDivisions != null && !ageDivisions.isEmpty()) ? ageDivisions.get(0) : null);
		}
		// remove if now null
		String value = getAgeDivision() != null ? getAgeDivision().name() : null;
		updateParam(params1, "ad", value);

		List<String> ageGroupParams = params1.get("ag");
		// no age group is the default
		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
		setAgeGroupPrefix(ageGroupPrefix);
		String value2 = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateParam(params1, "ag", value2);

		switchLightingMode(this, darkMode, false);
		updateURLLocations();
		setShowInitialDialog(
		        darkParams == null && ageDivisionParams == null && ageGroupParams == null && silentParams == null);

		if (getDialog() == null) {
			buildDialog(this);
		}
		setUrlParameterMap(params1);
		return params1;
	}

	public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	@Override
	public void setDarkMode(boolean dark) {
		this.darkMode = dark;
	}

	@Override
	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void setDialogTimer(Timer timer) {
		this.dialogTimer = timer;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setShowInitialDialog(boolean)
	 */
	@Override
	public void setShowInitialDialog(boolean b) {
		this.initializationNeeded = true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setSilenced(boolean)
	 */
	@Override
	public void setSilenced(boolean silent) {
		// no-op, silenced by definition
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			doUpdate(competition);
		});
	}

	@Override
	public void switchLightingMode(Component target, boolean dark, boolean updateURL) {
		target.getElement().getClassList().set(DARK, dark);
		target.getElement().getClassList().set(LIGHT, !dark);
		setDarkMode(dark);
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), DARK, dark ? null : "false");
		}
	}

	@Override
	public void switchSoundMode(Component target, boolean silent, boolean updateURL) {
		setSilenced(silent);
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SILENT, silent ? null : "false");
		}
	}

	public void uiLog(UIEvent e) {
		if (e == null) {
			uiEventLogger.debug("### {} {}", this.getClass().getSimpleName(), LoggerUtils.whereFrom());
		} else {
			uiEventLogger.debug("### {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			        LoggerUtils.whereFrom());
		}
	}

	private String computeAgeGroupSuffix() {
		String suffix = null;
		if (getAgeGroupPrefix() != null) {
			suffix = getAgeGroupPrefix();
		}
		return (suffix != null ? " &ndash; " + suffix : "");
	}

	private String formatDouble(double d) {
		if (floatFormat == null) {
			floatFormat = new DecimalFormat();
			floatFormat.setMinimumIntegerDigits(1);
			floatFormat.setMaximumFractionDigits(3);
			floatFormat.setMinimumFractionDigits(3);
			floatFormat.setGroupingUsed(false);
		}
		return floatFormat.format(d);
	}

	private String formatInt(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else if (total < 0) {
			return "(" + Math.abs(total) + ")";
		} else {
			return total.toString();
		}
	}

	private AgeDivision getAgeDivision() {
		return ageDivision;
	}

	private String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	@SuppressWarnings("unused")
	private Object getOrigin() {
		return this;
	}

	private void getTeamJson(Team t, JsonObject ja, Gender g) {
		ja.put("team", t.getName());
		ja.put("counted", formatInt(t.getCounted()));
		ja.put("size", formatInt((int) t.getSize()));
		ja.put("score", formatDouble(t.getSinclairScore()));
		ja.put("points", formatInt(t.getPoints()));
	}

	private JsonValue getTeamsJson(List<TeamTreeItem> teamItems, boolean overrideTeamWidth) {
		JsonArray jath = Json.createArray();
		int athx = 0;
		List<Team> list3 = teamItems != null
		        ? teamItems.stream().map(TeamTreeItem::getTeam).collect(Collectors.toList())
		        : Collections.emptyList();
		if (overrideTeamWidth) {
			// when we are called for the second time, and there was a wide team in the top
			// section.
			// we use the wide team setting for the remaining sections.
			setWide(false);
		}

		for (Team t : list3) {
			JsonObject ja = Json.createObject();
			Gender curGender = t.getGender();
			getTeamJson(t, ja, curGender);
			String teamName = t.getName();
			if (teamName != null && teamName.length() > Competition.SHORT_TEAM_LENGTH) {
				setWide(true);
			}
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}

	private List<String> setAgeGroupPrefixItems(ComboBox<String> ageGroupPrefixComboBox,
	        AgeDivision ageDivision2) {
		List<String> activeAgeGroups = AgeGroupRepository.findActiveAndUsed(ageDivision2);
		ageGroupPrefixComboBox.setItems(activeAgeGroups);
		return activeAgeGroups;
	}

	private void setWide(boolean b) {
		this.getElement().setProperty("wideTeamNames", b);
	}

	private List<TeamTreeItem> topN(List<TeamTreeItem> list) {
		if (list == null) {
			return new ArrayList<>();
		}
		int size = list.size();
		if (size > 0) {
			int min = Math.min(size, SHOWN_ON_BOARD);
			list = list.subList(0, min);
		}
		return list;
	}

	private void updateBottom() {
		this.getElement().setProperty("topTeamsMen",
		        mensTeams != null && mensTeams.size() > 0
		                ? getTranslation("Scoreboard.TopTeamsSinclairMen") + computeAgeGroupSuffix()
		                : "");
		this.getElement().setPropertyJson("mensTeams", getTeamsJson(mensTeams, true));

		this.getElement().setProperty("topTeamsWomen",
		        womensTeams != null && womensTeams.size() > 0
		                ? getTranslation("Scoreboard.TopTeamsSinclairWomen") + computeAgeGroupSuffix()
		                : "");
		this.getElement().setPropertyJson("womensTeams", getTeamsJson(womensTeams, false));
	}

	private void updateURLLocations() {
		updateURLLocation(UI.getCurrent(), getLocation(), DARK,
		        !isDarkMode() ? Boolean.TRUE.toString() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ag",
		        getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ad",
		        getAgeDivision() != null ? getAgeDivision().name() : null);
	}

	protected void doEmpty() {
		logger.trace("doEmpty");
		FieldOfPlay fop = OwlcmsSession.getFop();
		setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), getElement());
	}

	protected void doUpdate(Athlete a, UIEvent e) {
		logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			if (a != null) {
				updateBottom();
			}
		});
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		checkVideo(Config.getCurrent().getParamStylesDir()+"/video/top.css", routeParameter, this);
		switchLightingMode(this, isDarkMode(), true);
		setWide(false);
		setTranslationMap();
		for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
			// we listen on all the uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		}
		Competition competition = Competition.getCurrent();
		doUpdate(competition);
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		this.getElement().setPropertyJson("t", translations);
	}
}
