37.2.0-beta:  Preliminary builds for testing purposes.

37.2 focuses on avoiding the need to install an MQTT server. MQTT is used  to support full-fledged hardware devices that are TCRR-compliant (such as those in the [Blue Owl project](https://github.com/owlcms/blue-owl))

- 37. 2 Enhancements
  
  - A MQTT server is now embedded in owlcms. There is no longer a need to run a separate one. 
    
  - If you don't use MQTT refereeing devices (such as referee devices with a warning light/buzzer) then you have nothing to do.
    
  - If you used MQTT before,
    - See [embedded MQTT Server configuration documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/MQTT)  Simple cases don't require any configuration.
  
    - You likely have used a `-DmqttServer` flag (or have defined an `OWLCMS_MQTTSERVER` environment variable). You should *remove them*, otherwise owlcms will use these values to locate an *external* MQTT server.  On  Windows, check the `owlcms.l4j.ini` file in the installation directory.  You should also disable the local Mosquitto or aedes server if you wish to use the embedded one.
  
  - 37.2.0-beta00: added a `OWLCMS_ENABLEEMBEDDEDMQTT` variable (`-DenableEmbeddedMqtt`).  If explicitly set to `false` (or any value other than `true`), then the embedded MQTT server will not be started.  If absent, the value of the checkbox in the database config is used.
  
  - 37.2.0-alpha01: For cloud-based setups, free MQTT brokers like [hivemq](https://console.hivemq.cloud) that require mqtts are now supported (plain TLS without client-side certificates).  If the port given to owlcms starts with "8", TLS will be used when connecting to an external server.
  
- 37.2 Fixes
  
  - 37.2.0-rc01: Setting a password using OWLCMS_PIN or -Dpin was no longer working.
  - 37.2.0-rc01: Connecting to the MQTT Server without a username is now possible (previously when the owlcms field was empty a username still had to be provided, but could be anything)
  - 37.2.0-beta00: the MQTT connection loop is now a separate thread to avoid blocking the user interface.
  - 37.2.0-alpha01: the MQTT server was not enabled on a new database
  - 37.2.0-alpha01: the presence/absence of the MQTT server parameter war.


##### Highlights from recent stable releases

- The announcer and marshal screens show the 6 attempts and total for each athlete. ([#525](https://github.com/jflamy/owlcms4/issues/525))
- Migration to the most recent version of the Vaadin user interface framework. The navigation (menu, top menu bar) has been redone to use officially supported components.
- There is now a separate page for pre-competition documents. There are now separate documents for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/2400PreCompetitionDocuments).
- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- Improvements to Records eligibility. See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_${revision}.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_${revision}.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Heroku) or (recommended) **[Fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean)
