# icalendar-to-jrnl

Convert Google Calendar iCalendar (ICS) events into `jrnl` import format. Works with a local `.ics` file or directly from a Google Calendar Secret iCal URL.

## Overview
This CLI reads an iCalendar and prints entries in the `jrnl --import` format:

- Extracts DTSTART, SUMMARY
- Formats date/time as `[YYYY-MM-DD HH:mm] Title` followed by optional body
- Splits `SUMMARY` into title and body at the first period (`.`). If a period exists, it is kept at the end of the title and the remainder becomes the body.

Implementation: Java + [ical4j](https://github.com/ical4j/ical4j). Main class: `ep.App`.

## Build
Requires Java 24+ and Maven.

```bash
mvn clean package
```

This produces `target/icalendar-to-jrnl.jar` (fat jar with dependencies).

## Usage
Two modes are supported.

### 1) Local ICS file
```bash
java -jar ./target/icalendar-to-jrnl.jar -f /path/to/calendar.ics | jrnl --import
```

### 2) Secret iCal URL (Google Calendar)
Get the Secret iCal URL in Google Calendar:
- Gear icon → Settings
- Select the desired calendar
- Integrate calendar → Secret address in iCal format

Then run:
```bash
java -jar ./target/icalendar-to-jrnl.jar -u "https://calendar.google.com/calendar/ical/your_secret_ical_url" | jrnl --import
```

## Output format
Each event becomes one `jrnl` entry like:
```
[YYYY-MM-DD HH:mm] Title.
Body text if provided
```

- Date comes from `DTSTART` (assumed format `yyyyMMdd'T'HHmmss'Z'`).
- Title/body come from `SUMMARY` split at the first period.

## Notes & limitations
- All-day events and timezones beyond the `Z` UTC format are not handled explicitly.
- Only events with both `DTSTART` and `SUMMARY` are exported.
- Description, location, attendees, etc. are not used.

