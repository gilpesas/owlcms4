/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("resultsmedals-template")
@JsModule("./components/ResultsMedals.js")
@Route("displays/resultsMedals")

public class ResultsMedals extends PolymerTemplate<TemplateModel>
        implements DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle,
        RequireDisplayLogin {

	JsonArray cattempts;

	JsonArray sattempts;

	private Category category;

	private boolean darkMode = true;

	private Dialog dialog;
	private FieldOfPlay fop;
	private Group group;
	private boolean initializationNeeded;
	private Location location;
	private UI locationUI;
	final private Logger logger = (Logger) LoggerFactory.getLogger(ResultsMedals.class);

	private TreeMap<Category, TreeSet<Athlete>> medals;

	private boolean silenced = true;

	private EventBus uiEventBus;

	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	private Timer dialogTimer;

	private String routeParameter;

	/**
	 * Instantiates a new results board.
	 */
	public ResultsMedals() {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
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
		DisplayOptions.addLightingEntries(vl, target, this);
	}

	@Override
	public void doBreak(UIEvent event) {
		if (!(event instanceof UIEvent.BreakStarted)) {
			return;
		}
		// logger.trace("break event = {} {} {}", e.getBreakType(), e.getTrace(),
		// ceremonyGroup);

		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			this.getElement().setProperty("fullName",
			        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("attempt", "");
			setHidden(false);

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
			this.getElement().callJsFunction("doBreak");
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			Group ceremonyGroup = e.getCeremonyGroup();
			setGroup(ceremonyGroup);
			Category ceremonyCategory = e.getCeremonyCategory();
			setCategory(ceremonyCategory);

			this.getElement().setProperty("fullName",
			        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("attempt", "");
			setHidden(false);

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
			this.getElement().callJsFunction("doBreak");
		}));
	}

	public Category getCategory() {
		return category;
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

	public FieldOfPlay getFop() {
		return fop;
	}

	public Group getGroup() {
		return group;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getPageTitle() {
		return getTranslation("CeremonyType.MEDALS") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public boolean isDarkMode() {
		return darkMode;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
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
		return silenced;
	}

	@Override
	public HashMap<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		FOPParameters r = (this);
		HashMap<String, List<String>> newParameterMap = new HashMap<>(parametersMap);

		// get the fop from the query parameters, set to the default FOP if not provided
		FieldOfPlay fop = null;

		@Nonnull
		List<String> fopNames = parametersMap.get("fop");
		boolean fopFound = fopNames != null && fopNames.get(0) != null;
		if (!fopFound) {
			r.setShowInitialDialog(true);
		}

		if (!r.isIgnoreFopFromURL()) {
			if (fopFound) {
				FOPParameters.logger.trace("fopNames {}", fopNames);
				fop = OwlcmsFactory.getFOPByName(fopNames.get(0));
			} else if (OwlcmsSession.getFop() != null) {
				FOPParameters.logger.trace("OwlcmsSession.getFop() {}", OwlcmsSession.getFop());
				fop = OwlcmsSession.getFop();
			}
			if (fop == null) {
				FOPParameters.logger.trace("OwlcmsFactory.getDefaultFOP() {}", OwlcmsFactory.getDefaultFOP());
				fop = OwlcmsFactory.getDefaultFOP();
			}
			newParameterMap.put("fop", Arrays.asList(URLUtils.urlEncode(fop.getName())));
			this.setFop(fop);
		} else {
			newParameterMap.remove("fop");
		}

		// get the group from query parameters
		Group group = null;
		if (!r.isIgnoreGroupFromURL()) {
			List<String> groupNames = parametersMap.get("group");
			if (groupNames != null && groupNames.get(0) != null) {
				group = GroupRepository.findByName(groupNames.get(0));
			} else {
				group = (fop != null ? fop.getGroup() : null);
			}
			if (group != null) {
				newParameterMap.put("group", Arrays.asList(URLUtils.urlEncode(group.getName())));
			}
			this.setGroup(group);
		} else {
			newParameterMap.remove("group");
		}

		// get the category from query parameters
		Category cat = null;
		if (!r.isIgnoreGroupFromURL()) {
			List<String> catCodes = parametersMap.get("cat");
			if (catCodes != null && catCodes.get(0) != null) {
				cat = CategoryRepository.findByCode(catCodes.get(0));
			}
			// logger.trace("cat = {}", cat);
			if (cat != null) {
				newParameterMap.put("cat", Arrays.asList(URLUtils.urlEncode(cat.getName())));
			}
			this.setCategory(cat);
		} else {
			newParameterMap.remove("cat");
		}

		FOPParameters.logger.debug("URL parsing: {} OwlcmsSession: fop={} group={}", LoggerUtils.whereFrom(),
		        (fop != null ? fop.getName() : null), (cat != null ? cat.getName() : null));
		HashMap<String, List<String>> params = newParameterMap;

		List<String> darkParams = params.get(DARK);
		// dark is the default. dark=false or dark=no or ... will turn off dark mode.
		boolean darkMode = darkParams == null || darkParams.isEmpty() || darkParams.get(0).toLowerCase().equals("true");
		setDarkMode(darkMode);
		switchLightingMode(this, darkMode, false);
		updateParam(params, DARK, !isDarkMode() ? "false" : null);

		List<String> silentParams = params.get(SILENT);
		// silent is the default. silent=false will cause sound
		boolean silentMode = silentParams == null || silentParams.isEmpty()
		        || silentParams.get(0).toLowerCase().equals("true");
		if (!isSilencedByDefault()) {
			// for referee board, default is noise
			silentMode = silentParams != null && !silentParams.isEmpty()
			        && silentParams.get(0).toLowerCase().equals("true");
		}
		switchSoundMode(this, silentMode, false);
		updateParam(params, SILENT, !isSilenced() ? "false" : "true");

		return params;
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

	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	public void setGroup(Group group) {
		this.group = group;
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

	@Override
	public void setSilenced(boolean silent) {
	}

	@Subscribe
	public void slaveAllEvents(UIEvent e) {
		// logger.trace("*** {}", e);
	}

//    @Subscribe
//    public void slaveStopBreak(UIEvent.BreakDone e) {
//        // logger.debug("------ slaveStopBreak {}", e.getBreakType());
//        uiLog(e);
//        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
//            setHidden(false);
//            this.getElement().callJsFunction("reset");
//            doUpdate(e);
//        });
//    }

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			// logger.trace("------- slaveBreakDone {}", e.getBreakType());
			setHidden(false);
			doUpdate(e);
		}));
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			// logger.trace("------- slaveCeremonyDone {}", e.getCeremonyType());
			if (e.getCeremonyType() == CeremonyType.MEDALS) {
				// end of medals break.
				// If this page was opened in replacement of a display, go back to the display.
				unregister(this, uiEventBus);
				retrieveFromSessionStorage("pageURL", result -> {
					if (result != null && !result.isBlank()) {
						UI.getCurrent().getPage().setLocation(result);
					}
				});
			}
		}));
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.trace("------- slaveCeremonyStarted {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setHidden(false);
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		// logger.trace("------- slaveStartBreak {}", e.getBreakType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setHidden(false);
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		// logger.trace("****** slaveStartLifting ");
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setHidden(false);
			// If this page was opened in replacement of a display, go back to the display.
			unregister(this, uiEventBus);
			retrieveFromSessionStorage("pageURL", result -> {
				if (result != null && !result.isBlank()) {
					UI.getCurrent().getPage().setLocation(result);
				} else {
					this.getElement().callJsFunction("reset");
				}
			});
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(e);
		});
	}

	private void computeCategoryMedalsJson() {
		// logger.trace("computeCategoryMedalsJson = {} {}", getCategory(),
		// LoggerUtils.whereFrom(1));
		OwlcmsSession.withFop(fop -> {
			medals = Competition.getCurrent().getMedals(getGroup() != null ? getGroup() : fop.getGroup());
			TreeSet<Athlete> medalists = medals.get(getCategory());

			JsonArray jsonMCArray = Json.createArray();
			JsonObject jMC = Json.createObject();
			int mcX = 0;
			if (medalists != null && !medalists.isEmpty()) {
				jMC.put("categoryName", getCategory().getName());
				jMC.put("leaders", getAthletesJson(new ArrayList<>(medalists), fop));
				// logger.debug("medalCategory: {}", jMC.toJson());
				jsonMCArray.set(mcX, jMC);
				mcX++;
			}

			// logger.debug("medalCategories {}", jsonMCArray.toJson());
			this.getElement().setPropertyJson("medalCategories", jsonMCArray);
			if (mcX == 0) {
				this.getElement().setProperty("noCategories", true);
			}
		});
	}

	private void computeGroupMedalsJson() {
		OwlcmsSession.withFop(fop -> {
			// logger.trace("computeGroupMedalsJson = {} {}", getGroup(),
			// LoggerUtils.stackTrace());
			medals = Competition.getCurrent().getMedals(getGroup() != null ? getGroup() : fop.getGroup());
			JsonArray jsonMCArray = Json.createArray();
			int mcX = 0;
			for (Entry<Category, TreeSet<Athlete>> medalCat : medals.entrySet()) {
				JsonObject jMC = Json.createObject();
				TreeSet<Athlete> medalists = medalCat.getValue();
				if (medalists != null && !medalists.isEmpty()) {
					jMC.put("categoryName", medalCat.getKey().getName());
					jMC.put("leaders", getAthletesJson(new ArrayList<>(medalists), fop));
					// logger.debug("medalCategory: {}", jMC.toJson());
					jsonMCArray.set(mcX, jMC);
					mcX++;
				}
			}
			// logger.debug("medalCategories {}", jsonMCArray.toJson());
			this.getElement().setPropertyJson("medalCategories", jsonMCArray);
			if (mcX == 0) {
				this.getElement().setProperty("noCategories", true);
			}
		});
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private void computeMedalsJson() {
		if (getCategory() != null) {
			computeCategoryMedalsJson();
		} else {
			computeGroupMedalsJson();
		}
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

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	private String formatRank(Integer total) {
		if (total == null || total == 0) {
			return "";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else {
			return total.toString();
		}
	}

	private void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
		String category;
		category = curCat != null ? curCat.getName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank);
		ja.put("sattempts", sattempts);
		ja.put("cattempts", cattempts);
		ja.put("total", formatInt(a.getTotal()));
		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			ja.put("snatchRank", formatRank(mainRankings.getSnatchRank()));
			ja.put("cleanJerkRank", formatRank(mainRankings.getCleanJerkRank()));
			ja.put("totalRank", formatRank(mainRankings.getTotalRank()));
		} else {
			logger.error("main rankings null for {}", a);
		}
		ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
		Double double1 = a.getAttemptsDone() <= 3 ? a.getSinclairForDelta()
		        : a.getSinclair();
		ja.put("sinclair", double1 > 0.001 ? String.format("%.3f", double1) : "-");
		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");
		ja.put("sinclairRank", a.getSinclairRank() != null ? "" + a.getSinclairRank() : "-");

		String highlight = "";
		ja.put("classname", highlight);
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	private JsonValue getAthletesJson(List<Athlete> displayOrder, FieldOfPlay fop) {
		JsonArray jath = Json.createArray();
		int athx = 0;
//        Category prevCat = null;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();
		for (Athlete a : athletes) {
			JsonObject ja = Json.createObject();
			Category curCat = a.getCategory();
//            if (curCat != null && !curCat.sameAs(prevCat)) {
//                // changing categories, put marker before athlete
//                ja.put("isSpacer", true);
//                jath.set(athx, ja);
//                ja = Json.createObject();
//                prevCat = curCat;
//                athx++;
//            }
			// no blinking = 0
			getAthleteJson(a, ja, curCat, 0);
			String team = a.getTeam();
			if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
				logger.trace("long team {}", team);
				setWideTeamNames(true);
			}
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}

	/**
	 * Compute Json string ready to be used by web component template
	 *
	 * CSS classes are pre-computed and passed along with the values; weights are
	 * formatted.
	 *
	 * @param a
	 * @param fop
	 * @return json string with nested attempts values
	 */
	private void getAttemptsJson(Athlete a, int liftOrderRank) {
		sattempts = Json.createArray();
		cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;

			jri.put("goodBadClassName", "narrow empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
				case ACTUAL:
					if (!trim.isEmpty()) {
						if (trim.contentEquals("-") || trim.contentEquals("0")) {
							jri.put("goodBadClassName", "narrow fail");
							jri.put("stringValue", "-");
						} else {
							boolean failed = stringValue != null && stringValue.startsWith("-");
							jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
							jri.put("stringValue", formatKg(stringValue));
						}
					}
					break;
				default:
					if (stringValue != null && !trim.isEmpty()) {
						// logger.debug("{} {} {}", fop.getState(), x.getShortName(), curLift);

						String highlight = "";
						jri.put("goodBadClassName", "narrow request");
						if (notDone) {
							jri.put("className", highlight);
						}
						jri.put("stringValue", stringValue);
					}
					break;
				}
			}

			if (ix < 3) {
				sattempts.set(ix, jri);
			} else {
				cattempts.set(ix % 3, jri);
			}
			ix++;
		}
	}

	private Object getOrigin() {
		return this;
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting result board on FOP {}", fop.getLoggingName());
			setId("scoreboard-" + fop.getName());
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		});
		setTranslationMap();
	}

	private void retrieveFromSessionStorage(String key, SerializableConsumer<String> resultHandler) {
		getElement().executeJs("return window.sessionStorage.getItem($0);", key)
		        .then(String.class, resultHandler);
	}

	private void setCategory(Category cat) {
		this.category = cat;
	}

	private void setHidden(boolean hidden) {
		this.getElement().setProperty("hiddenBlockStyle", (hidden ? "display:none" : "display:block"));
		this.getElement().setProperty("inactiveBlockStyle", (hidden ? "display:block" : "display:none"));
		this.getElement().setProperty("hiddenGridStyle", (hidden ? "display:none" : "display:grid"));
		this.getElement().setProperty("inactiveGridStyle", (hidden ? "display:grid" : "display:none"));
		this.getElement().setProperty("inactiveClass", (hidden ? "bigTitle" : ""));
	}

	private void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
		// logger.debug("sync {}", e.getState());
		switch (e.getState()) {
		case INACTIVE:
			doEmpty();
			break;
		case BREAK:
			if (e.getGroup() == null) {
				doEmpty();
			} else {
				doUpdate(e);
				doBreak(e);
			}
			break;
		default:
			setHidden(false);
			doUpdate(e);
		}
	}

	private void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin(), LoggerUtils.whereFrom());
	}

	private void updateBottom(String liftType, FieldOfPlay fop) {
		// logger.debug("updateBottom");
		this.getElement().setProperty("groupName", "");
		this.getElement().setProperty("liftDone", "-");
		computeMedalsJson();
	}

	protected void doEmpty() {
		// no need to hide, text is self evident.
		// this.setHidden(true);
	}

	protected void doUpdate(UIEvent e) {
		// logger.trace("---------- doUpdate {} {} {}", e != null ?
		// e.getClass().getSimpleName() : "no event");
		boolean leaveTopAlone = false;
		if (e instanceof UIEvent.LiftingOrderUpdated) {
			LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
			if (e2.isInBreak()) {
				leaveTopAlone = !e2.isDisplayToggle();
			} else {
				leaveTopAlone = !e2.isCurrentDisplayAffected();
			}
		}

		FieldOfPlay fop = OwlcmsSession.getFop();
		if (!leaveTopAlone) {
			this.getElement().callJsFunction("reset");
		}
		logger.debug("updating bottom");
		updateBottom(null, fop);
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();
			// logger.debug("group {}", this.getGroup());
			medals = Competition.getCurrent().getMedals(this.getGroup());
			setHidden(false);
			computeMedalsJson();
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
		switchLightingMode(this, isDarkMode(), true);
		if (!Competition.getCurrent().isSnatchCJTotalMedals()) {
			getElement().setProperty("noLiftRanks", "noranks");
		}
		SoundUtils.enableAudioContextNotification(this.getElement());
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