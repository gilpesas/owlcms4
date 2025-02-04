**Version 49 alpha**

> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - Alpha releases are **not** normally used in actual competitions.

- (alpha02) Fix for session card print order.  When printing all sessions, the cards were not in session order, but rather in category order.
  
- Announcer
  - The previous athlete is now highlighted in blue in the grid.  The current and next athletes are also highlighted (yellow and orange, same color convention as on the default scoreboards)

- Jury
  - The weight attempted on the previous attempt is now shown, to facilitate filling in a manual protocol sheet.

- Timekeeper
  - The timekeeper can restart the clock even if a down signal has been given or a decision is shown.  This is required if referees mistakenly give reds when the bar has not reached the knees.

- Scoreboards:
  - White is now used for good lifts on all scoreboards (previously some used green)
  - The layout now includes vertical spacing between the lifts for better readability.
- Team flag preview: 
  - The team membership page now shows the flag for each team, allowing a quick check that all are correctly assigned.
- Documents:
  - The Weigh-in Form now includes the participation categories so the coach can sign them off and they can be cross-checked during data entry.  This is useful when there are multiple championships with the same categories.
  - Additional options to get Session Date/Time for Excel templates: the following values are now available on the session object (for example `${session.localWeighInDay}` would give the short date for weigh-in using the current country settings).
    - Using the local formatting conventions for dates: `localWeighInDay`, `localWeighInHour`, `localStartDay`, `localStartHour`
    - Using the international ISO format: `intlWeighInDay`, `intlWeighInHour`, `intlStartDay`, `intlStartHour`
- Technical
  - Event Forwarding and MQTT event propagation refactoring. In previous releases, obsolete forwarders could accidentally be kept when reloading sessions.  This would cause the publicresults scoreboard to alternate between out-of-date and current results.

