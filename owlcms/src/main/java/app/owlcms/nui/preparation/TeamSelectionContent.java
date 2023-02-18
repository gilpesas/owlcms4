/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.utils.Resource;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ResultsContent.
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@Route(value = "preparation/teams", layout = OwlcmsLayout.class)
public class TeamSelectionContent extends VerticalLayout
        implements CrudListener<Participation>, OwlcmsContent, DisplayParameters {

	public static final String TITLE = "TeamMembership.Title";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamSelectionContent.class);
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}

	private AgeDivision ageDivision;
	private String ageGroupPrefix;
	private ComboBox<Category> categoryFilter;
	private GridCrud<Participation> crudGrid;
	private Button download;

	private Anchor finalPackage;

	private ComboBox<Gender> genderFilter;
	private Location location;
	private UI locationUI;
	private OwlcmsLayout routerLayout;
	private ComboBox<String> teamFilter;
	private boolean teamFilterRecusion;
	private ComboBox<Resource> templateSelect;
	private FlexLayout topBar;
	private ComboBox<AgeDivision> topBarAgeDivisionSelect;
	private ComboBox<String> topBarAgeGroupPrefixSelect;
	private JXLSCompetitionBook xlsWriter;
	private Timer dialogTimer;

	private String routeParameter;

	HashMap<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in
	 * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public TeamSelectionContent() {
		OwlcmsCrudFormFactory<Participation> crudFormFactory = createFormFactory();
		crudGrid = createGrid(crudFormFactory);
		// setTopBarTitle(getTranslation(TITLE));
		defineFilters(crudGrid);
		crudGrid.getGrid().setMultiSort(true);
		fillHW(crudGrid, this);

	}

	@Override
	public Participation add(Participation domainObjectToAdd) {
		// no-op
		return null;
	}

	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		return;
	}

	/**
	 * Create the top bar.
	 *
	 * Note: the top bar is created before the content.
	 *
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and
	 *      vice-versa
	 *
	 * @param topBar
	 */
	@Override
	public FlexLayout createMenuArea() {
		// logger.trace("createTopBar {}", LoggerUtils. stackTrace());
		// show arrow but close menu
		getAppLayout().setMenuVisible(true);
		getAppLayout().closeDrawer();

		topBar = new FlexLayout();
		xlsWriter = new JXLSCompetitionBook(true, UI.getCurrent());
		StreamResource href = new StreamResource(TITLE + "Report" + ".xls", xlsWriter);
		finalPackage = new Anchor(href, "");
		finalPackage.getStyle().set("margin-left", "1em");
		download = new Button(getTranslation(TITLE + ".Report"), new Icon(VaadinIcon.DOWNLOAD_ALT));

		topBarAgeGroupPrefixSelect = new ComboBox<>();
		topBarAgeGroupPrefixSelect.setPlaceholder(getTranslation("AgeGroup"));
		topBarAgeGroupPrefixSelect.setEnabled(false);
		topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		topBarAgeGroupPrefixSelect.setValue(null);
		topBarAgeGroupPrefixSelect.setWidth("8em");
		topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		topBarAgeGroupPrefixSelect.getStyle().set("margin-left", "1em");
		setAgeGroupPrefixSelectionListener();

		topBarAgeDivisionSelect = new ComboBox<>();
		topBarAgeDivisionSelect.setPlaceholder(getTranslation("AgeDivision"));
		List<AgeDivision> adItems = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
		topBarAgeDivisionSelect.setItems(adItems);
		topBarAgeDivisionSelect.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
		topBarAgeDivisionSelect.setClearButtonVisible(true);
		topBarAgeDivisionSelect.setWidth("8em");
		topBarAgeDivisionSelect.getStyle().set("margin-left", "1em");
		setAgeDivisionSelectionListener();

		setAgeGroupPrefixItems(topBarAgeGroupPrefixSelect, getAgeDivision());
		topBarAgeGroupPrefixSelect.setValue(getAgeGroupPrefix());
		topBarAgeDivisionSelect.setValue(getAgeDivision());

		finalPackage.add(download);
		HorizontalLayout buttons = new HorizontalLayout(finalPackage);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		buttons.setPadding(false);
		buttons.setMargin(false);
		buttons.setSpacing(true);

		topBar.getStyle().set("flex", "100 1");
		topBar.removeAll();
		topBar.add(topBarAgeDivisionSelect, topBarAgeGroupPrefixSelect
		/* , templateSelect */
		/* , buttons */
		);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		return topBar;
	}

	@Override
	public void delete(Participation domainObjectToDelete) {
		// no-op
	}

	/**
	 * Get the content of the crudGrid. Invoked by refreshGrid.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Participation> findAll() {
		if (getAgeGroupPrefix() == null && getAgeDivision() == null) {
			return new ArrayList<>();
		}

		List<Participation> participations = AgeGroupRepository.allParticipationsForAgeGroupAgeDivision(ageGroupPrefix,
		        ageDivision);

		Set<String> teamNames = new TreeSet<>();
		Stream<Participation> stream = participations.stream()
		        .filter(p -> {
			        Category catFilterValue = categoryFilter.getValue();
			        String catCode = catFilterValue != null ? catFilterValue.getComputedCode() : null;
			        String athleteCatCode = p.getCategory().getComputedCode();

			        String teamFilterValue = teamFilter.getValue();
			        String athleteTeamName = p.getAthlete().getTeam();
			        if (athleteTeamName == null || athleteTeamName.isBlank()) {
				        return false;
			        }
			        teamNames.add(athleteTeamName);

			        Gender genderFilterValue = genderFilter != null ? genderFilter.getValue() : null;
			        Gender athleteGender = p.getAthlete().getGender();

			        boolean catOk = (catFilterValue == null || athleteCatCode.contentEquals(catCode))
			                && (genderFilterValue == null || genderFilterValue == athleteGender)
			                && (teamFilterValue == null || teamFilterValue.contentEquals(athleteTeamName));
			        return catOk;
		        })
		        .sorted((a, b) -> {
			        int compare = 0;
			        String teamA = a.getAthlete().getTeam();
			        String teamB = b.getAthlete().getTeam();
			        compare = teamA.compareTo(teamB);
			        if (compare != 0) {
				        return compare;
			        }

			        Category catA = a.getCategory();
			        Category catB = b.getCategory();
			        compare = catA.compareTo(catB);
			        if (compare != 0) {
				        return compare;
			        }

			        String nameA = a.getAthlete().getFullName();
			        String nameB = b.getAthlete().getFullName();
			        compare = nameA.compareTo(nameB);
			        if (compare != 0) {
				        return compare;
			        }

			        return 0;
		        })
		// .peek(p -> logger.trace("findAll {}", p.long_dump()))
		;
		String teamValue = teamFilter.getValue();
		teamFilterRecusion = false;
		teamFilter.setItems(teamNames);
		teamFilter.setValue(teamValue);
		teamFilterRecusion = true;
		return stream.collect(Collectors.toList());
	}

	@Override
	public Dialog getDialog() {
		return null;
	}

	@Override
	public Timer getDialogTimer() {
		return dialogTimer;
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

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation(TITLE);
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public HashMap<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public boolean isDarkMode() {
		return false;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	public boolean isShowInitialDialog() {
		return false;
	}

	@Override
	public boolean isSilenced() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#readParams(com.vaadin.flow.router.Location,
	 *      java.util.Map)
	 */
	@Override
	public HashMap<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		HashMap<String, List<String>> params1 = new HashMap<>(parametersMap);

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

//        switchLightingMode(this, darkMode, false);
		updateURLLocations();

		// buildDialog(this);
		return params1;
	}

	public void refresh() {
		crudGrid.refreshGrid();
	}

	@Override
	public void setDarkMode(boolean dark) {
		return;
	}

	@Override
	public void setDialog(Dialog dialog) {
	}

	@Override
	public void setDialogTimer(Timer dialogTimer) {
		this.dialogTimer = dialogTimer;
	}

	@Override
	public void setHeaderContent() {
		routerLayout.setMenuTitle(getPageTitle());
		routerLayout.setMenuArea(createMenuArea());
		routerLayout.showLocaleDropdown(false);
		routerLayout.setDrawerOpened(false);
		routerLayout.updateHeader(true);
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

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public void setShowInitialDialog(boolean b) {
		return;
	}

	@Override
	public void setSilenced(boolean silent) {
		return;
	}

	@Override
	public void setUrlParameterMap(HashMap<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Override
	public Participation update(Participation domainObjectToUpdate) {
		// no-op
		return null;
	}

	/**
	 * No editing for Team membership, direct unbuffered modification.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	private OwlcmsCrudFormFactory<Participation> createFormFactory() {
		return null; // not used.
	}

	private AgeDivision getAgeDivision() {
		return ageDivision;
	}

	private String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
		Resource found = null;
		Resource totalTemplate = null;
		for (Resource curResource : resourceList) {
			String fileName = curResource.getFileName();
			if (fileName.equals(curTemplateName)) {
				found = curResource;
				break;
			}
			if (fileName.startsWith("Total")) {
				totalTemplate = curResource;
			}
		}
		if (found != null) {
			return found;
		} else {
			// should be non-null, if not there, null is ok.
			return totalTemplate;
		}
	}

	private void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	private void setAgeGroupPrefix(String value) {
		this.ageGroupPrefix = value;
	}

	private List<String> setAgeGroupPrefixItems(ComboBox<String> ageGroupPrefixComboBox,
	        AgeDivision ageDivision2) {
		List<String> activeAgeGroups = AgeGroupRepository.findActiveAndUsed(ageDivision2);
		ageGroupPrefixComboBox.setItems(activeAgeGroups);
		return activeAgeGroups;
	}

	@SuppressWarnings("unused")
	private void setTemplateSelectionListener(List<Resource> resourceList) {
		String curTemplateName = Competition.getCurrent().getFinalPackageTemplateFileName();
		Resource found = searchMatch(resourceList, curTemplateName);
		templateSelect.addValueChangeListener((e) -> {
			Competition.getCurrent().setFinalPackageTemplateFileName(e.getValue().getFileName());
			CompetitionRepository.save(Competition.getCurrent());
		});
		templateSelect.setValue(found);
	}

	private void updateFilters(AgeDivision ageDivision2, String ageGroupPrefix2) {
		List<Category> categories = CategoryRepository.findByGenderDivisionAgeBW(genderFilter.getValue(),
		        getAgeDivision(), null, null);
		if (getAgeGroupPrefix() != null && !getAgeGroupPrefix().isBlank()) {
			categories = categories.stream().filter((c) -> c.getAgeGroup().getCode().equals(getAgeGroupPrefix()))
			        .collect(Collectors.toList());
		}
		// logger.trace("updateFilters {}, {}, {}", ageDivision2, ageGroupPrefix2,
		// categories);
		categoryFilter.setItems(categories);
	}

	private void updateURLLocations() {
		updateURLLocation(UI.getCurrent(), getLocation(), DARK, null);
//                !isDarkMode() ? Boolean.TRUE.toString() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ag",
		        getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null);
		updateURLLocation(UI.getCurrent(), getLocation(), "ad",
		        getAgeDivision() != null ? getAgeDivision().name() : null);
	}

	/**
	 * Gets the crudGrid.
	 *
	 * @return the crudGrid crudGrid
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createCrudGrid(app.owlcms.nui.crudui.OwlcmsCrudFormFactory)
	 */
	protected OwlcmsCrudGrid<Participation> createGrid(OwlcmsCrudFormFactory<Participation> crudFormFactory) {
		Grid<Participation> grid = new Grid<>(Participation.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn(new ComponentRenderer<>(p -> {
			// checkbox to avoid entering in the form
			Checkbox activeBox = new Checkbox("Name");
			activeBox.setLabel(null);
			activeBox.getElement().getThemeList().set("secondary", true);
			activeBox.setValue(p.getTeamMember());
			activeBox.addValueChangeListener(click -> {
				Boolean value = click.getValue();
				activeBox.setValue(value);
				JPAService.runInTransaction(em -> {
					logger.info("{} {} as team member for category {}", value ? "setting" : "removing",
					        p.getAthlete().getShortName(), p.getCategory().getName());
					p.setTeamMember(Boolean.TRUE.equals(value));
					em.merge(p);
					return null;
				});
				// crudGrid.getGrid().getDataProvider().refreshItem(p);
				crudGrid.refreshGrid();
			});
			return activeBox;
		})).setHeader(Translator.translate("TeamMembership.TeamMember")).setWidth("0").setSortable(true);
		grid.addColumn(p -> p.getAthlete().getTeam())
		        .setHeader(Translator.translate("Team"))
		        .setSortable(true);
		grid.addColumn(p -> p.getAthlete().getFullName())
		        .setHeader(Translator.translate("Name"))
		        .setSortable(true);
		grid.addColumn(p -> p.getCategory())
		        .setHeader(Translator.translate("Category"))
		        .setSortable(true);
		grid.addColumn(p -> p.getAthlete().getGender())
		        .setHeader(Translator.translate("Gender"))
		        .setSortable(true);

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
		OwlcmsCrudGrid<Participation> crudGrid = new OwlcmsCrudGrid<>(Participation.class, gridLayout, crudFormFactory,
		        grid) {
			@Override
			protected void initToolbar() {
				Checkbox selectAll = new Checkbox();
				selectAll.addValueChangeListener(e -> {
					JPAService.runInTransaction(em -> {
						if (Boolean.TRUE.equals(e.getValue())) {
							findAll().forEach(a -> {
								a.setTeamMember(true);
								em.merge(a);
							});
						} else {
							findAll().forEach(a -> {
								a.setTeamMember(false);
								em.merge(a);
							});
						}
						em.flush();
						return null;
					});
					refresh();
				});
				HorizontalLayout vl = new HorizontalLayout(selectAll);
				vl.setAlignItems(Alignment.CENTER);
				crudLayout.addToolbarComponent(vl);
			}

			@Override
			protected void updateButtonClicked() {
			}

			@Override
			protected void updateButtons() {
			}
		};

		crudGrid.setCrudListener(this);
		crudGrid.setClickRowToUpdate(true);

		return crudGrid;
	}

	protected void defineFilters(GridCrud<Participation> crud) {
		if (teamFilter == null) {
			teamFilter = new ComboBox<>();
			teamFilter.setPlaceholder(getTranslation("Team"));
			teamFilter.setClearButtonVisible(true);
			teamFilter.addValueChangeListener(e -> {
				if (!teamFilterRecusion) {
					return;
				}
				crud.refreshGrid();
			});
			teamFilter.setWidth("10em");
		}
		crud.getCrudLayout().addFilterComponent(teamFilter);

		if (genderFilter == null) {
			genderFilter = new ComboBox<>();
			genderFilter.setPlaceholder(getTranslation("Gender"));
			genderFilter.setItems(Gender.M, Gender.F);
			genderFilter.setItemLabelGenerator((i) -> {
				return i == Gender.M ? getTranslation("Gender.M") : getTranslation("Gender.F");
			});
			genderFilter.setClearButtonVisible(true);
			genderFilter.addValueChangeListener(e -> {
				crud.refreshGrid();
			});
			genderFilter.setWidth("10em");
		}
		crud.getCrudLayout().addFilterComponent(genderFilter);

		if (categoryFilter == null) {
			categoryFilter = new ComboBox<>();
			categoryFilter.setClearButtonVisible(true);
			categoryFilter.setPlaceholder(getTranslation("Category"));
			categoryFilter.setClearButtonVisible(true);
			categoryFilter.addValueChangeListener(e -> {
				crud.refreshGrid();
			});
			categoryFilter.setWidth("10em");
		}
		crud.getCrudLayout().addFilterComponent(categoryFilter);
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play
	 * (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	protected void setAgeDivisionSelectionListener() {
		topBarAgeDivisionSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the download button.
			AgeDivision ageDivisionValue = e.getValue();
			setAgeDivision(ageDivisionValue);
			if (ageDivisionValue == null) {
				topBarAgeGroupPrefixSelect.setValue(null);
				topBarAgeGroupPrefixSelect.setItems(new ArrayList<String>());
				topBarAgeGroupPrefixSelect.setEnabled(false);
				topBarAgeGroupPrefixSelect.setValue(null);
				crudGrid.refreshGrid();
				return;
			}

			String existingAgeGroupPrefix = getAgeGroupPrefix();
			List<String> activeAgeGroups = setAgeGroupPrefixItems(topBarAgeGroupPrefixSelect, ageDivision);
			if (existingAgeGroupPrefix != null) {
				topBarAgeGroupPrefixSelect.setValue(existingAgeGroupPrefix);
				topBarAgeGroupPrefixSelect.setEnabled(true);
			} else if (activeAgeGroups != null && !activeAgeGroups.isEmpty() && ageDivision != AgeDivision.MASTERS) {
				// no default value
				// topBarAgeGroupPrefixSelect.setValue(activeAgeGroups.get(0));
				topBarAgeGroupPrefixSelect.setEnabled(true);
			} else {
				topBarAgeGroupPrefixSelect.setEnabled(false);
			}

			xlsWriter.setAgeDivision(ageDivisionValue);
			finalPackage.getElement().setAttribute("download",
			        "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
			                : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");

			crudGrid.refreshGrid();
		});
	}

	protected void setAgeGroupPrefixSelectionListener() {
		topBarAgeGroupPrefixSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds
			// the download button.
			setAgeGroupPrefix(e.getValue());

			xlsWriter.setAgeGroupPrefix(ageGroupPrefix);
			finalPackage.getElement().setAttribute("download",
			        "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
			                : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");

			updateFilters(getAgeDivision(), getAgeGroupPrefix());
			updateURLLocations();
			crudGrid.refreshGrid();
		});
	}
}
