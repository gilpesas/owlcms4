# owlcms: Olympic Weightlifting Competition Management System 

> **The full-range solution for running competitions**
>
> This free application is a *fully-compliant* weightlifting competition management system that has been used worldwide to manage many large-scale championships. 
>
> **Simple and automatic**
>
> The program computes lifting order automatically, and automatically sets the clock correctly.  It also enforces the rules for weight changes (time remaining, moving down).  The announcer always sees the lifting order, and is informed of all the changes, and is shown what weight is required on the bar.
>
> It is actually simple enough that you can run a small club meet with just a single laptop, or a regional meet with only everyday electronics (a couple of laptops and TVs, some phones).

## Gallery



<table>
<tr><td><a href="https://www.google.com/maps/d/embed?mid=1cFqfyfoF_RSoM56GewSPDWbuoHihsw4&ehbc=2E312F&ll=29.63705469324934%2C18.91643749999993&z=3" target="_bkank"><img src='img/Gallery/Map.png'></a></td><td>Current Usage<br/><a href="https://www.google.com/maps/d/embed?mid=1cFqfyfoF_RSoM56GewSPDWbuoHihsw4&ehbc=2E312F&ll=29.63705469324934%2C18.91643749999993&z=3" target="_blank">Click on image for an interactive map</a>.<br/>The owlcms competition management system is currently in use in over 45 federations.  See here for <a href="./#/Countries">a list</a></td></tr>
<tr><td><img src='img/Gallery/lima.jpg'></img></td><td>PanAm Junior and SouthAmerican  U20/U17/U15 Championships, Lima, Peru.<br>The program  supports simultaneous platforms and displays the multiple rankings for multiple concurrent age group competitions</td></tr>
<tr><td><img src='img/Gallery/ElSalvador.jpg'></img></td><td>National Competition, El Salvador.<br>A simple setup with everyday electronics.</td></tr>
<tr><td><img src='img/Gallery/chelles.jpg' size=250></img></td><td>Club meet, Chelles, France.<br>Supports mixed-gender kid groups.</td></tr>
<tr><td><img src='img/Gallery/IMG_1480.jpg' size=250></img></td><td>Canadian Senior Championship, Kelowna, Canada. A full-scale national championship.</td></tr>
<tr><td><img src='img/Gallery/gaspe2018.png' size=250></img></td><td>PanAm Masters Championship, Gaspé, Canada. The program has full support of Masters rules and coefficients.</td></tr>
<tr><td><img src='img/Gallery/nordic2021.png' size=250></img></td><td>Nordic Championships, Copenhagen, Denmark.<br> Streaming software can easily integrate the displays produced by the application.</td></tr>
<tr><td><img src='img/Gallery/mexicoNational.jpg'></img></td><td>Mexico National Games.<br>(4 platforms, each with jury.)</td></tr>




</table>

## Downloads and Installation

Most people run the program on a laptop at the competition site ([overview](InstallationOverview#stand-alone-laptop-installation))

- [Windows Stand-alone Installation](LocalWindowsSetup)
- [Linux or Mac Stand-alone Installation](LocalLinuxMacSetup)

You can also run the program in the cloud, for example to host virtual meets. See [here](InstallationOverview#cloud-based-installation) for instructions

## Demo

The following videos and demos are available

- [Simple setup](Demo1): running a minimal setup for a small club meet with a single person controlling the meet.
- [Full setup](Demo2): running a regular or virtual competition with technical officials
- Live demo: after watching the videos, try the [Live demo](https://owlcms.fly.dev) site, or better yet, [install your own copy](Downloads) and experiment.

## Features

The following list is a sampling of the many features available.  <u>*Click on the images if you wish to view them full-sized*</u>.

- Run a **regular** or **masters** competition, with or without a **jury**.
  
- Ability to run locally or in the [**cloud**](EquipmentSetup#cloud-access-over-the-internet).  Decisions, timers and sounds are handled locally in the browser to provide better feedback.
  
- **[Scoreboard](Displays#scoreboard)** for public or warm-up room display.  Current and next lifters are highlighted.   If record information has been loaded, the records being attempted are highlighted

     ![020_Scoreboard](img/Records/records.png ':size=800')

     Leaders from current and previous groups can be shown for multi-group competitions. The leaders and records sections can be shown or hidden on demand.

     ![024_Scoreboard_Leaders](img/Displays/024_Scoreboard_Leaders.png)

     If athletes can win medals in several categories, a multi-rank scoreboard is available

     ![hvmDyjbdr2](img/Displays/hvmDyjbdr2.png)

     The top part of the scoreboard contains the same information as the attempt board, and shows the down signal and decisions.

     ![022_Scoreboard_Decision](img/Displays/022_Scoreboard_Decision.png)

- [**Announcer and Marshall**](Announcing) screens (updating athlete cards and recalculation of lifting order).    The information is laid out as on the official cards.  Messages are given for late changes, and the changes are checked for correctness relative to the lifting order. 
  
    ![090](nimg/3200Lifting/090.png ':size=350')
  
- [**Timekeeping**](Announcing#Starting_the_clock) Time can either be [managed by the announcer](Announcing#Starting-the-clock)  (useful for smaller meets) or a dedicated [timekeeper screen](Announcing#Timekeeper) can be used. The timekeeper screen can be conveniently operated from a phone or tablet.
  
    <img src="img/Lifting/050_Timekeeper.png" alt="050_Timekeeper.png" width=350 style="border-style:solid; border-width: thin" />

- **[Attempt Board](Displays#attempt-board)** showing current athlete information, remaining time, weight requested, down signal and decision.
  
    ![032_Attempt_Running](img/Displays/032_Attempt_Running.png ':size=350' )  ![038_Attempt_Decision](img/Displays/038_Attempt_Decision.png ':size=350') 
  
- **Support for refereeing devices**
  
  - Any USB or Bluetooth [**keypad**](Refereeing#usb-or-bluetooth-keypads) that can be programmed to generate the digits 0 to 9 can be used to enter decisions
    
      ![refereeingSetup](img/equipment/refereeingSetup.jpg ':size=350')  ![030_iPad_Flic](img/Refereeing/030_iPad_Flic.jpg ':size=350')
    
  - [**Mobile phones or tablets**](Refereeing#mobile-device-refereeing) can also be used.  These devices can provide notifications to the referees.
    
      ![mobile_ref](img/Refereeing/mobile_ref.png ':size=350')
      
  - [**Physical devices with visual and audio feedback capability**](Refereeing#full-feedback-keypad)  Schematics and the full necessary software and firmware are available to build affordable devices that support referee reminders and jury summoning to comply with IWF TCRR.
  
      ![refereeBox](nimg/3200Lifting/refereeBox.png ':size=450')

- **[Athlete-facing display](Displays#attempt-board)** (the decision display matches the referee positions as seen from platform). Refereeing keypads are typically connected to this laptop.
  
    ![044_AF_Down](img/Displays/044_AF_Down.png ':size=350') ![048_AF_Decision](img/Displays/048_AF_Decision.png ':size=350')
  
-  **[Records](Records)**  Record information can be provided using Excel files. Records for multiple federations and events can be loaded.  Record is then shown on the scoreboards, and notifications are given to the officials when records are attempted or set.  If a record is improved, the record information is updated.
  
-  **[Lifting order display](Displays#lifting-order)**. Useful for the marshal or for regional championships to help newer coaches.

    ![Lifting](img/Displays/025_LiftingOrder.png  ':size=350')
    
- [**Team Competitions and Sinclair Competitions**](Displays#Top-Teams-Scoreboard).  Team Results are computed in either the IWF points system or as a sum of Sinclair scores. The competition secretary has access to the full details.
  
  ![050_TeamScoreboard](img/Displays/050_TeamScoreboard.png ':size=350')
  ![060_TopSinclair](img/Displays/060_TopSinclair.png ':size=350')
    ![061_TopTeamSinclair](img/Displays/061_TopTeamSinclair.png ':size=350')
  
- [**3 and 5-person jury**](Refereeing#jury).  Jury members see referee decisions as they happen. Jury members see their vote outcome once all jurors have voted. 

    ![070_Jury](img/Refereeing/070_Jury.png  ':size=350')
  
- **[Athlete Registration](Registration) and [Weigh-in](WeighIn) screens**, including production of **[weigh-in sheet](WeighIn#starting-weight-sheet)** with starting weights and **[athlete cards](WeighIn#athlete-cards)**.

    ![042_AthleteCards](img/WeighIn/043_AthleteCards.png ':size=350')

- [**Upload of registration sheet**](Registration#uploading-a-list-of-athletes) Upload a list of athletes with their team, group, entry totals etc. (same format as owlcms2, in either xls or xlsx format)
  
    ![073_excel](img/Preparation/073_excel.png ':size=350')
  
- Multiple **[simultaneous age divisions](Preparation#defining-age-divisions-and-categories)**: ability to award separate medals according to age division (e.g. youth vs junior vs senior) .  Simultaneous inclusion of Masters and non-masters groups athletes is possible.
  
     ![020_ageGroupList](img/Categories/020_ageGroupList.png ':size=350')
  
- [**Competition Parameters**](Preparation#competition-information) :  screens for defining a competition (general info, location, organizer, etc.) and special rules that apply (for example, enforcing or not the 20kg rule, etc.)
  
    ![030_Competition](img/Preparation/030_Competition.png ':size=350')

- **[Multiple fields of play](Preparation#defining-fields-of-play-platforms)** (platforms): simultaneous competition platforms within the same competition.
  
  ![IMG_1610](img/ZoomVideo/IMG_1610.jpg)
  
- **[Countdown timer for breaks](Announcing#breaks)** (before introduction, before first snatch, break before clean and jerk, technical break)

    ![070_IntroTimer](img/Displays/070_IntroTimer.png ':size=350')
    
- Production of **[group results (protocol sheets)](Documents#group-results)** and of the **[final result package](Documents#competition-package)**
  
    ![SessionResults](img/Documents/SessionResults.png  ':size=350')
    
- Option to treat the competition as a **[Masters competition](Preparation#masters)** with proper processing of age groups (older age groups presented first)
  
- **[Video Streaming Scene Switching](OBSSceneSwitching)** When using OBS (or similar software) to stream a competition, a special status window can be monitored to switch scenes, trigger replays, or provide information as to the course of the competition (for example, jury deliberation, etc.)
  
- [**Multiple languages**](Preparation#display-language). Currently English, French, Spanish (Latin America, Spain), Danish, Swedish, German, Portuguese, Romanian, Hungarian, Russian, and Armenian.
  
- **[Color and Visual Styling Customization](Styles)**  The colors of the displays are controlled by Web-standard CSS stylesheets, the format used by web designers world wide. A tutorial is given for the common case of adjusting the color scheme to local preferences.
  
    ![colors](img/Displays/colors.png ':size=350')
  
- Etc.  Refer to the side menu for the full list of topics.

## Support

- [Discussion list](https://groups.google.com/forum/#!forum/owlcms)  If you wish to discuss the program or ask questions, please add yourself to this discussion [group](https://groups.google.com/forum/#!forum/owlcms).  You can withdraw at any time.
- [Issues and Feature Requests](https://github.com/jflamy/owlcms4/issues)  See the Project Board below to see what we are working on.
- [Project board](https://github.com/users/jflamy/projects/4/views/1) This shows what we are working on, and our work priorities.  Check here first, we may actually already be working on it
